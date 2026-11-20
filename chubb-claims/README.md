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
