# AI working journal

## Milestone 1 — Claim submission and lookup

- Asked: plan the Chubb assessment first, then start with the backend after switching to Sol. The candidate will review the code and make the commits locally.
- AI proposal: one Spring Boot application, REST for operations with an immediate response, and file-backed H2 to keep the local startup small and persist claims across restarts.
- AI implementation: generated the backend and the submit/read API. Claim submission stores the incident and returns a UUID and the initial status.
- AI verification: an API smoke check exposed a timestamp precision change on read-back. The implementation now stores the submission timestamp at microsecond precision. The rerun passed submission, lookup, validation, unknown ID, and persistence after restart.
- Candidate verification: I ran the Spring Boot backend locally on Windows.
  I submitted a sample motor claim via POST, then retrieved it via GET using
  the returned claim ID. The claim details matched and the status was SUBMITTED.

  ## Milestone 2 — Staff claim workflow

- Asked AI to continue with the staff claim workflow. It added a claim queue, officer assignment, and settlement or rejection decisions.
- Candidate verification: I ran the backend locally, assigned a new claim to Officer A, and saw its status change to `UNDER_REVIEW`. I settled it and verified that a separate GET request returned `SETTLED`.

## Milestone 3 — Additional information requests

- Asked AI to add a flow for officers to request more information and for claimants to reply.
- AI implementation: the claim stores the latest question and answer. A request ID prevents replies to an older question from being applied to the current one.
- Candidate verification: I tested the flow locally. The claim moved to `AWAITING_INFORMATION` when the officer asked a question and returned to `UNDER_REVIEW` after I replied. The API returned the answer I submitted.
