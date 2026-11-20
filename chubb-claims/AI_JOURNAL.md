# AI working journal

## Milestone 1 — Claim submission and lookup

- Asked: I shared my ideas for the project flow, backend, and tools, then compared them with the AI's suggestions before settling on a plan. I switched to the Sol model and asked it to start with the backend. I reviewed the generated code and made the commits locally.
- AI proposal: Use one Spring Boot application, REST for operations that need an immediate response, and file-backed H2 to simplify local setup and preserve claims across restarts.
- AI implementation: Generated the backend and the claim submission and lookup endpoints. Submitting a claim stores the incident details and returns a unique claim ID and an initial status.
- AI verification: An API smoke test found a timestamp precision difference after reading a saved claim. The AI updated the code to store the timestamp at microsecond precision. The rerun passed submission, lookup, validation, unknown-ID handling, and persistence after restart.
- Candidate verification: I reviewed the code and workflow against the plan and ran the Spring Boot backend locally on Windows. I submitted a sample motor claim with `POST`, retrieved it with `GET` using the returned ID, and confirmed that the details matched and the status was `SUBMITTED`.

## Milestone 2 — Staff claim workflow

- Asked AI to add the staff claim queue, officer assignment, and settlement or rejection decisions.
- Candidate verification: I ran the backend locally, assigned a new claim to Officer A, and confirmed that its status changed to `UNDER_REVIEW`. I settled it and verified that a separate `GET` request returned `SETTLED`.

## Milestone 3 — Additional information requests

- Asked AI to let officers request more information and claimants reply.
- AI implementation: The claim stores the latest question and answer. Each request has its own ID so that a reply to an older question cannot be applied to the current request.
- Candidate verification: I tested both sides of the exchange. The claim moved to `AWAITING_INFORMATION` when the officer asked a question and returned to `UNDER_REVIEW` after I replied. I also checked that the API returned the answer I submitted.

## Milestone 4 — Staff workload summary

- Asked AI to add a workload summary with claim counts by status and officer.
- Candidate verification: I called the endpoint locally and checked the totals against the status breakdown. There were three claims in total, two open claims, and one open claim assigned to Officer A. The other open claim was still `SUBMITTED` and unassigned.

## Milestone 5 — Outstanding exposure

- Asked AI to add an outstanding exposure summary grouped by currency.
- Candidate verification: I recorded an estimate of MYR 1,250.50 on a claim under review. The exposure summary showed one MYR claim worth 1,250.50 and counted two other open claims whose estimates were still missing.

## Milestone 6 — Angular claims portal

- Asked AI to connect the claimant and staff views to the Spring Boot API.
- Candidate verification: I submitted a claim in the browser and confirmed that the portal filled in the returned claim ID and that the same claim appeared in the staff queue. I picked it up as an officer, requested additional information, replied as the claimant, and recorded a settlement. I returned to the claimant view, clicked **Check status**, and confirmed that it showed `SETTLED` and the decision reason.

## Milestone 7 — Kafka decision events

- Asked AI to add Kafka for asynchronous claim decision events. The REST request saves the decision first. After the database transaction commits, the backend publishes the event to Kafka, and the demo consumer logs it.
- Candidate verification: I ran Kafka and Spring Boot locally on Windows and settled a new claim through the Angular staff portal. The backend logged `Consumed ClaimDecisionRecorded` with the same claim ID and `status=SETTLED`.
- Candidate decision: Once I saw the matching Kafka event, I stopped adding features with about 30 minutes left and used the remaining time to review the documentation and prepare the submission.

## Final review

- I spent approximately three hours on the assessment. About two hours went into the core application, with most of that time spent on the backend. I spent about 30 minutes integrating and testing Kafka, then used the final 30 minutes to review the documentation and prepare the submission.
- I kept claim decisions on REST so staff can see the saved status immediately. Kafka handles the follow-up event asynchronously after the decision is committed.
- I kept the Kafka consumer focused on logging so I could verify event delivery within the timebox. Email delivery and durable retries are follow-up work.
- During the Kafka browser demo, I entered a future incident date and saw the generic message `Invalid request content`. I corrected the date and submitted the claim successfully. I would improve the form by showing a field-specific message that explains which dates are allowed.