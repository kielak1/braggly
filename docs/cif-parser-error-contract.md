# CIF atom types and error dispatch

This release is based on production backend `74aa84e11e9b6380852a6301c6526a98f198bc8d`.
It changes CIF atom-type resolution and servlet error authorization only. P0/P1 limits,
pools, JVM settings, storage lifecycle and dependencies remain unchanged.

## Atom-type sources

IUCr allows `_atom_site_type_symbol` to be absent when atom-site label component 0
identifies the type. An explicit per-site type takes precedence over the label:

1. Read `_atom_site_type_symbol` when the row has a value.
2. Otherwise use the separate `_atom_site_label_component_0` column, when available.
3. Otherwise extract component 0 from a strictly recognized `_atom_site_label`.
   Supported labels include `C1`, `Cl2`, `Bi1`, `Fe3`, `O1A`, `O(3A)` and underscore
   components such as `C_a_phe_83_a_0`.

For sources 2/3, a populated `_atom_type_symbol` dictionary must contain the
identified code. The dictionary is not paired to sites by row order, and neither
the formula nor a single dictionary entry is used to guess an unknown site type.
Human-readable `_atom_type_description` and scattering data are not interpreted
as an authoritative mapping to elements.

Every chosen code must contain one correctly cased element from the existing
118-element table in `ChemicalComposition`. The resolver accepts plain symbols
and ionic suffixes such as `Fe3+` and `Bi+3`; it returns the element (`Fe`, `Bi`)
for the viewer, without the oxidation-state suffix. Explicit types override even
conflicting labels. Invalid explicit types are rejected rather than overridden.
CIF missing-value markers `.` and `?` permit fallback to the next source.

This is deliberately not a complete CIF label grammar. Mixed species, dummy
types, isotope notation, arbitrary words, incorrectly cased symbols and ambiguous
charge/site suffixes are unsupported. For example `CO1`, `Cobalt1`, `FeNi1`,
`13C1` and `Fe3+17` are not inferred from labels. They produce a safe 422 rather
than a guessed element. A separate component-0 column can resolve a charged type
without that label ambiguity.

## Real cases and offline fixtures

| COD | Available sources | Atoms | Elements after resolution |
|---|---|---:|---|
| 7209481 | explicit ionic type symbols, labels, fractional coordinates | 5 | B:1, Bi:1, O:3 |
| 1550148 | labels such as `O(3A)` and `C(1A)`, coordinates, atom-type dictionary C/H/O; no per-site type symbol | 66 | C:32, H:28, O:6 |
| 7105573 | labels such as `O(1)`, `N(1)` and `C(1)`, coordinates; no per-site type symbol or atom-type dictionary | 20 | C:8, H:9, N:1, O:2 |

`backend/src/test/resources/cif/` contains reduced scientific atom-site fixtures
from these COD records, including the relevant dictionary for 1550148. The
actual labels, ionic codes and coordinate values are preserved. Automated tests
use local fixtures and mocked storage, with no crystallography.net dependency.
The full previously downloaded scientific files were also checked locally and
yielded the same atom counts/compositions. No production imports/uploads are
needed to validate this change.

## HTTP and security contract

- Supported CIF: 200.
- Unsupported/ambiguous atom data, absent required coordinates, malformed input,
  or ciftools `EmptyColumnException`/`ParsingException`: controlled 422, generic
  message, no exception details.
- Existing size/atom limits: 413; concurrency limit: 429.
- Unexpected backend/storage/IO failures: 5xx, without client exception details.
- Anonymous protected request: existing 403; USER access to admin API: 403.

Previously an unhandled exception entered servlet ERROR dispatch. The stateless
JWT filter does not authenticate that dispatch; the catch-all authentication
rule therefore masked the original failure as 403. SecurityConfig now permits
only `DispatcherType.ERROR` before applying the existing request matchers.
Normal requests, including direct GET `/error`, retain their existing protection.
No public wildcard path or general `/error` URL permission is added.

Tests use embedded Tomcat, the real SecurityConfig/JWT filter and actual admin
controller role check. They verify generic RuntimeException -> 500 (including
requests with `trace=true&message=true`), anonymous denial, USER/admin separation,
ADMIN success, normal CIF success, and unsupported CIF -> 422. Stream closure,
permit release and a subsequent valid request are checked after failures.

## Frontend and separate WebGL issue

The deployed frontend already treats 422 as a per-CIF failure: it retains valid
results, continues subsequent CIF downloads and does not show a global auth
error. Existing 31 tests and two additional local diagnostic contract tests pass.
No frontend changes are part of this release.

`CodAccordion` independently probes WebGL for every mounted result, even when
collapsed. Local tests produced 17 probe contexts for 17 results and 72 for 72
results, with zero actual viewers. This is separate from CIF parsing and is not
evidence of a reopen lifecycle leak. A future, separate change should share or
memoize one capability probe per page/application, or probe only the expanded
item. No WebGL/3Dmol changes are included here.

## Sources

- [IUCr atom-site type](https://www.iucr.org/__data/iucr/cifdic_html/3/CORE_DIC/Iatom_site.type_symbol.html)
- [IUCr atom-site label](https://www.iucr.org/__data/iucr/cifdic_html/3/CORE_DIC/Iatom_site.label.html)
- [IUCr separate component 0](https://www.iucr.org/__data/iucr/cifdic_html/3/CORE_DIC/Iatom_site.label_component_0.html)
- [IUCr atom-type dictionary](https://www.iucr.org/__data/iucr/cifdic_html/3/CORE_DIC/Iatom_type.symbol.html)
- [Spring Security dispatcher authorization](https://docs.spring.io/spring-security/reference/servlet/authorization/authorize-http-requests.html)
