# APAC Claims Platform

The platform supports motor and property claim submission, tracking, and a staff review queue with settlement and rejection decisions.

## Run locally

Install JDK 21 and Maven 3.9 or later. From the repository root:

```sh
cd backend
mvn spring-boot:run
```

The API listens at http://localhost:8080. H2 stores local demo records in `backend/data/`.

## Try the API

Send `POST /api/claims` with `Content-Type: application/json` and this body:

```json
{
  "type": "MOTOR",
  "market": "MY",
  "incidentDate": "2026-01-15",
  "description": "Rear bumper damaged in a parking lot."
}
```

Expect `201 Created` with status `SUBMITTED` and a `Location` header. Send `GET` to that location to retrieve the stored claim. An invalid payload returns `400`. A valid but unknown claim ID returns `404`.

Use synthetic records for this milestone. Lookup by claim ID is exposed to local demo users. Identity and authorization are pending. The API accepts two uppercase letters for the market while the six supported markets are unspecified in the brief.

## Staff review workflow

| Request | Behaviour |
| --- | --- |
| `GET /api/claims?status=SUBMITTED` | List unassigned claims, oldest first. Omit the filter for all claims. |
| `POST /api/claims/{id}/assignment` | Send `{"officer":"Officer A"}` to pick up a submitted claim and move it to `UNDER_REVIEW`. |
| `POST /api/claims/{id}/decision` | Send `{"outcome":"SETTLED","reason":"Damage verified"}` or use `REJECTED` as the outcome. A decision requires a claim under review. |
| `GET /api/claims/{id}` | Read the current status, assigned officer, and the decision reason when available. |

Workflow updates return `409 Conflict` when the claim is in the wrong state. A database row lock serializes updates to the same claim so two staff requests cannot both pick it up.

The staff routes and free-text officer field are for a local demo. They share the claim API and currently have no role checks. Assignment and decisions use synchronous REST so the caller sees the committed status immediately. The workflow is contained in one Spring Boot service and one database transaction per update.

### Smoke test in PowerShell

With the backend running, use a separate terminal:

```powershell
$api = 'http://localhost:8080/api/claims'
$claim = Invoke-RestMethod -Method Post -Uri $api -ContentType 'application/json' -Body '{"type":"MOTOR","market":"MY","incidentDate":"2026-01-15","description":"Sample damage for staff review."}'
$claimUrl = "$api/$($claim.id)"
Invoke-RestMethod -Method Post -Uri "$claimUrl/assignment" -ContentType 'application/json' -Body '{"officer":"Officer A"}'
Invoke-RestMethod -Method Post -Uri "$claimUrl/decision" -ContentType 'application/json' -Body '{"outcome":"SETTLED","reason":"Damage verified"}'
Invoke-RestMethod -Uri $claimUrl
```

Expect `UNDER_REVIEW` after assignment and `SETTLED` after the decision and lookup.

## Request additional information

An officer can ask a question while the claim is `UNDER_REVIEW`. The claim moves to `AWAITING_INFORMATION` and appears in that status filter. The claimant can read the question through the claim lookup and reply using the request ID. The reply sends the claim back to `UNDER_REVIEW`, and the assigned officer can continue to a decision.

| Request | Body |
| --- | --- |
| `POST /api/claims/{id}/information-requests` | `{"question":"Where is the damaged panel?"}` |
| `POST /api/claims/{id}/information-requests/{requestId}/response` | `{"answer":"The rear left panel."}` |

The response and claim lookup include an `informationRequest` object with the latest question, request ID and answer. The demo keeps one request at a time and stores the latest exchange. A new request replaces the previous exchange after it has been answered. A stale or duplicate reply returns `409 Conflict`. The claimant checks for updates by looking up the claim.

### Test the information exchange in PowerShell

```powershell
$api = 'http://localhost:8080/api/claims'
$claim = Invoke-RestMethod -Method Post -Uri $api -ContentType 'application/json' -Body '{"type":"MOTOR","market":"MY","incidentDate":"2026-01-15","description":"Sample damage for information review."}'
$claimUrl = "$api/$($claim.id)"
Invoke-RestMethod -Method Post -Uri "$claimUrl/assignment" -ContentType 'application/json' -Body '{"officer":"Officer A"}'
$pending = Invoke-RestMethod -Method Post -Uri "$claimUrl/information-requests" -ContentType 'application/json' -Body '{"question":"Where is the damaged panel?"}'
$pending.status
$replyUrl = "$claimUrl/information-requests/$($pending.informationRequest.id)/response"
$review = Invoke-RestMethod -Method Post -Uri $replyUrl -ContentType 'application/json' -Body '{"answer":"The rear left panel."}'
$review.status
$review.informationRequest.answer
```

Expect `AWAITING_INFORMATION` after the question, then `UNDER_REVIEW` and the submitted answer after the reply.

## Staff workload summary

`GET /api/staff/workload` returns a current snapshot across all markets. It includes
`totalClaims`, `openClaims`, counts for every status in `byStatus`, and the same
status counts per assigned officer in `byOfficer`.

Open claims include `SUBMITTED`, `UNDER_REVIEW`, and `AWAITING_INFORMATION`.
Unassigned claims contribute to the overall counts. Each officer row represents
claims assigned to that officer, including closed claims. The officer keys use
the demo names entered at assignment. The database groups counts in one query.

Officer figures are counts by current state. Use the exposure endpoint below for monetary estimates.

With the backend running, query the summary in a second PowerShell terminal:

```powershell
Invoke-RestMethod 'http://localhost:8080/api/staff/workload' | ConvertTo-Json -Depth 5
```

A new claim increases the overall `SUBMITTED` count. Assign it to an officer and
check again. The claim moves to `UNDER_REVIEW` and contributes to that officer's
open workload.

## Outstanding exposure

While a claim is `UNDER_REVIEW`, an officer can set or replace a loss estimate using
`PUT /api/claims/{id}/estimate` with `{"amount":1250.50,"currency":"MYR"}`.
The endpoint accepts nonnegative decimal amounts, validates the currency code and its
minor-unit precision, and returns the updated claim. The estimate remains on the claim
after closure.

`GET /api/staff/exposure` returns the sum and claim count for each currency across
open claims. `openClaimsWithoutEstimate` counts open claims whose exposure is still
unknown. A recorded zero is a known estimate. When a claim is settled or rejected, its
estimate leaves the outstanding total. The summary uses the latest staff-entered
estimates and performs no currency conversion.

### Test exposure in PowerShell

```powershell
$api = 'http://localhost:8080/api/claims'
$claim = Invoke-RestMethod -Method Post -Uri $api -ContentType 'application/json' -Body '{"type":"PROPERTY","market":"MY","incidentDate":"2026-01-15","description":"Sample property damage for exposure review."}'
$url = "$api/$($claim.id)"
$null = Invoke-RestMethod -Method Post -Uri "$url/assignment" -ContentType 'application/json' -Body '{"officer":"Officer A"}'
$null = Invoke-RestMethod -Method Put -Uri "$url/estimate" -ContentType 'application/json' -Body '{"amount":1250.50,"currency":"MYR"}'
Invoke-RestMethod 'http://localhost:8080/api/staff/exposure' | ConvertTo-Json -Depth 5
```

The MYR amount increases by 1250.50 and the MYR claim count increases by one.

## Run the Angular portal

Start the Spring Boot backend on port 8080. In a second terminal, from the
`chubb-claims` project folder, use Node.js 22.12+ (22.x) or Node.js 24:

```powershell
cd frontend
npm ci
npm start
```

Open http://localhost:4200. The Angular dev server proxies `/api` requests
to Spring Boot. Run `npm run build` in `frontend` to compile the frontend.

### Demo walkthrough

1. In **Claimant**, report an incident. The portal fills in the returned claim ID.
2. In **Staff**, select the claim, pick it up, and save an estimate. Check the
   workload and exposure summaries. Then send a question to the claimant.
3. Return to **Claimant**, click **Check status**, and answer the question.
4. Return to **Staff**, record a decision, and check that outstanding exposure
   reflects the closure. In **Claimant**, click **Check status** to read the decision.

The Claimant and Staff tabs are demo views over the same API. The demo uses
manual refresh for external updates and has no authentication or access controls.

## Architecture and trade-offs

Angular uses REST for claim submission, lookup, staff actions and summary queries.
The user gets the saved state or a validation error immediately. One Spring Boot
service owns the claim workflow and H2 data. Claim updates lock one database row
inside the transaction, which avoids competing changes to the same claim during
staff review. The two Angular views share this API. H2 keeps the demo portable.

Kafka is an optional asynchronous path for decisions. Once a settlement or
rejection has committed to H2, the `kafka` profile publishes a
`ClaimDecisionRecorded` event to `claims.decision-recorded.v1`, keyed by claim ID.
The payload contains the claim ID, outcome and timestamp. The demo consumer logs
the event. The claimant's tracking page continues to read the persisted decision.
Email delivery, durable outbox retries, and authentication are future work. Broker
or process failures after the database commit can leave a decision without an
event. A production notification flow would use a transactional outbox and an
idempotent consumer.

## Run with Kafka

Normal backend and Angular startup work independently of Kafka. To demonstrate
events, start a local Kafka 4.1.2 broker in its own terminal:

```powershell
docker run --rm -p 127.0.0.1:9092:9092 apache/kafka:4.1.2
```

For a Windows setup with JDK 21, the [Apache Kafka 4.1.2 archive](https://archive.apache.org/dist/kafka/4.1.2/kafka_2.13-4.1.2.tgz)
also works without Docker. Extract the archive, enter the extracted Kafka folder,
and run:

```powershell
(Get-Content .\config\server.properties) -replace '^log.dirs=.*', 'log.dirs=./claims-demo-data' |
  Set-Content -Encoding Ascii .\config\claims-demo.properties
$clusterId = java -cp ".\libs\*" kafka.tools.StorageTool random-uuid
java -cp ".\libs\*" kafka.tools.StorageTool format --standalone --cluster-id $clusterId --config .\config\claims-demo.properties
java -cp ".\libs\*" kafka.Kafka .\config\claims-demo.properties
```

Run the format command once per broker data directory. On later runs, use the
last command with the previously created configuration.

With the broker running, restart Spring Boot from `backend` with the Kafka profile:

```powershell
mvn spring-boot:run "-Dspring-boot.run.profiles=kafka"
```

If Maven was installed using a PowerShell path variable, replace `mvn` with
`& $mvn`. Set `KAFKA_BOOTSTRAP_SERVERS` if the broker uses a different address.

From the Angular portal, pick up a new claim and settle or reject it. In the
backend terminal, look for a line containing `Consumed ClaimDecisionRecorded`,
the claim ID and the outcome.

