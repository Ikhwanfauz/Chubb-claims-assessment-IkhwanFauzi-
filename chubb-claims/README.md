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
