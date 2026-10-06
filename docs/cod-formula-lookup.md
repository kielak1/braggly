# COD formula lookup contract

`POST /openai/cod` returns `formulaCOD`. The dashboard passes that value unchanged
to `GET /api/cod/id?formula=...`. COD CSV `formula` is stored verbatim in `cod_entry`;
`compoundsource` is not a composition key. No existing application formula-sum
parser exists; ciftools parses CIF documents, not this lookup contract.

For flat formula sums, lookup compares maps from real element symbols to positive
atom counts. Order, whitespace, optional enclosing COD dashes, omitted count 1,
compact notation and equivalent decimal counts do not affect equality. Repeated
element tokens are summed. Counts are exact BigDecimal values, not floating point;
stoichiometric ratios are not reduced (H2O and H4O2 differ).

Parentheses, hydrate separators, charges, isotope labels, scientific notation and
unknown element symbols are deliberately unsupported. Those requests keep the
previous exact-text lookup; they are never partially parsed into false matches.

Existing database values are not changed. A parameterized PostgreSQL regex narrows
candidates to flat sums of the requested symbols, but never establishes equality.
The same parser checks every candidate. Keyset pages contain at most 500 lightweight
id/formula projections; no managed entity graph or unbounded candidate collection
is loaded. Only matching IDs accumulate, as required by the existing response.
The regex has no indexed composition column and can scan the table; at larger scale
an indexed canonical column would be a separate migration, outside this fix.

API URLs, response shapes, authorization, OpenAI prompt/model, import behavior,
CIF limits, JVM and pool settings are unchanged. No P0/P1 changes are included.
