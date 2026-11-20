# APAC Claims Platform

This milestone implements claim submission and lookup for motor and property claims.

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
