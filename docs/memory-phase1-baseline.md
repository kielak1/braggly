# Memory optimization phase 1 baseline (2026-10-06)

Production remains unchanged until local validation and separate deployment authorization.

| Measurement | Baseline |
| --- | ---: |
| Production warm memory | approximately 0.95–0.98 GB |
| Production 30-day mean memory | approximately 0.89 GB |
| Production peak memory | 1.426 GB |
| Local unoptimized warm process RSS | approximately 565 MiB |
| Earlier experimental Xmx256/CPU2/Xss512k/codecache96m warm RSS | approximately 400 MiB |

GB denotes decimal Railway memory; MiB denotes process RSS in binary units.
The earlier 256 MiB profile is a comparison only, not phase 1 deployment settings.

Previous local method: actual executable JAR on Temurin Java 21, isolated Podman
PostgreSQL and synthetic COD/CIF/B2/OpenAI HTTP services; process `/proc/1/status`
RSS and thread counts, 75-second idle/warm sampling at five-second intervals,
`jcmd` heap/Native Memory Tracking snapshots. Explicit local GC measurements are
reported separately from normal idle measurements. No production credentials or
production datasets are used by local tests.

Prior raw evidence: `/tmp/braggly-memory-analysis-20261006` (external to Git).
Phase 1 evidence: `/tmp/braggly-memory-phase1-20261006` (external to Git), including
the preserved pre-change JAR for comparable before/after workload measurements.
There are no historical comparable large-import or large-CIF peak measurements;
report N/A unless a new matching pre-change run was performed.

Read-only production B2 sizing before selecting limits: 187 cached CIF objects;
median 13,590 bytes, p95 590,424 bytes, maximum 2,243,835 bytes. The three largest
files contain 56, 55 and 37 atom rows respectively (actual ciftools parser,
in-memory metadata extraction, no production data saved). Initial configurable
limits: 32 MiB/file (>14x current maximum), 100,000 atom rows, two concurrent CIF
HTTP operations including response serialization. File size also bounds parser
input; ciftools still builds an in-memory document, so this is not a streaming
CIF parser.

Subsequent read-only validation parsed all 187 cached CIFs in memory solely to
extract counts: median 32 atoms, p95 77, maximum 231. No CIF bodies were saved or
used by application tests. The 100,000-atom limit is >432x the observed maximum.
