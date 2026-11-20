# AI working journal

## Milestone 1 — Claim submission and lookup

- Asked: plan the Chubb assessment first, then start with the backend after switching to Sol. The candidate will review the code and make the commits locally.
- AI proposal: one Spring Boot application, REST for operations with an immediate response, and file-backed H2 to keep the local startup small and persist claims across restarts.
- AI implementation: generated the backend and the submit/read API. Claim submission stores the incident and returns a UUID and the initial status.
- AI verification: an API smoke check exposed a timestamp precision change on read-back. The implementation now stores the submission timestamp at microsecond precision. The rerun passed submission, lookup, validation, unknown ID, and persistence after restart.
- Candidate review: record which proposal or code you accepted, challenged, or changed and why before committing this milestone.
