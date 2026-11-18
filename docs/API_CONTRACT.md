# ShareMeal REST API Contract

Version: 1.0 draft for future Node.js, Express, and MongoDB backend integration.

Base URL: `https://api.sharemeal.app/api/v1`

All Android remote implementations must follow this contract. Mock repositories mirror it.

## 1. Conventions

- JSON request and response bodies; UTF-8.
- HTTPS only in staging and production.
- Timestamps are ISO-8601 UTC strings on the wire; Room stores epoch milliseconds.
- IDs are server-generated strings.
- List endpoints are paginated with `page` and `pageSize`; responses include `items`, `page`, `pageSize`, `total`.
- Every mutating request accepts `Idempotency-Key`; duplicate keys return the original result.
- Authorization uses `Authorization: Bearer <accessToken>`.
- Roles: `DONOR`, `NGO`, `ADMIN`. ADMIN is web-only.
- Ownership is enforced on the server for every resource.

## 2. Error Envelope

```json
{
  "error": {
    "code": "QUANTITY_EXCEEDED",
    "message": "Requested quantity exceeds remaining quantity.",
    "details": { "remainingQty": 40 }
  }
}
```

Common status codes:

| Status | Meaning |
|---|---|
| 400 | Validation failure |
| 401 | Missing or expired access token |
| 403 | Role, ownership, or verification failure |
| 404 | Resource not found |
| 409 | Conflict: duplicate request, over-allocation, invalid transition |
| 422 | Business rule rejection |
| 429 | Rate limited |

## 3. Enumerations

```
UserRole: DONOR | NGO | ADMIN
AccountStatus: PENDING | ACTIVE | SUSPENDED | REJECTED
VerificationStatus: NOT_SUBMITTED | PENDING | VERIFIED | REJECTED | SUSPENDED
DonorType: INDIVIDUAL | RESTAURANT | HOTEL | CATERER | EVENT_ORGANIZER |
           WEDDING_ORGANIZER | COLLEGE_MESS | HOSTEL | INSTITUTION | FOOD_PROCESSOR
FoodCategory: COOKED_MEAL | BAKERY | PACKAGED_FOOD | FRUITS_VEGETABLES |
              DAIRY | BEVERAGES | GRAINS | OTHER
QuantityUnit: MEALS | KG | PACKETS | LITRES | PIECES
DietType: VEGETARIAN | NON_VEGETARIAN | MIXED | UNKNOWN
DonationStatus: DRAFT | PUBLISHED | PARTIALLY_ALLOCATED | FULLY_ALLOCATED |
                PICKUP_SCHEDULED | COLLECTED | DISTRIBUTED | COMPLETED |
                CANCELLED | EXPIRED | SAFETY_HOLD
RequestStatus: PENDING | APPROVED | REJECTED | CANCELLED | PICKUP_SCHEDULED |
               COLLECTED | DISTRIBUTED | COMPLETED | EXPIRED
PickupStatus: SCHEDULED | NGO_READY | DONOR_READY | READY | IN_PROGRESS |
              COLLECTED | DISTRIBUTED | COMPLETED | CANCELLED | MISSED | FAILED
PickupExceptionReason: MISSED_PICKUP | DONOR_UNAVAILABLE | NGO_UNAVAILABLE |
                       VEHICLE_FAILURE | QUANTITY_MISMATCH | QUALITY_CONCERN |
                       ADDRESS_ISSUE | SAFETY_CONCERN | OTHER
WorkflowStage: DONATION_PUBLISHED | NGO_REQUESTED | DONOR_APPROVED |
               PICKUP_SCHEDULED | NGO_READY | DONOR_READY | READY |
               PICKUP_IN_PROGRESS | FOOD_COLLECTED | DISTRIBUTION_CONFIRMED |
               COMPLETED | CANCELLED | MISSED | FAILED
SafetyReportStatus: OPEN | UNDER_REVIEW | RESOLVED | DISMISSED
```

## 4. Authentication

### POST /auth/register

```json
{
  "name": "Asha Rao",
  "email": "asha@example.org",
  "phone": "+919876543210",
  "password": "strong-password",
  "role": "DONOR"
}
```

Response `201`:

```json
{
  "user": {
    "id": "usr_123",
    "name": "Asha Rao",
    "email": "asha@example.org",
    "phone": "+919876543210",
    "role": "DONOR",
    "accountStatus": "ACTIVE",
    "createdAt": "2026-11-05T10:00:00Z"
  },
  "tokens": {
    "accessToken": "jwt",
    "refreshToken": "jwt",
    "expiresIn": 900
  }
}
```

### POST /auth/login

```json
{ "email": "asha@example.org", "password": "strong-password" }
```

Response `200`: same shape as register.

### POST /auth/refresh

```json
{ "refreshToken": "jwt" }
```

Response `200`: new token pair. Refresh token rotation is required.

### POST /auth/logout

Requires access token. Invalidates the refresh token.

### POST /auth/forgot-password

```json
{ "email": "asha@example.org" }
```

Always returns `202` to avoid account enumeration.

### POST /auth/verify-otp

```json
{ "email": "asha@example.org", "otp": "123456", "purpose": "EMAIL_VERIFICATION" }
```

### GET /auth/me

Returns the current user and profile summary.

## 5. Profiles

### POST /donors/profile

```json
{
  "donorType": "WEDDING_ORGANIZER",
  "organizationName": "Green Leaf Events",
  "contactPerson": "Asha Rao",
  "phone": "+919876543210",
  "addressLine": "12 MG Road",
  "city": "Bengaluru",
  "state": "Karnataka",
  "postalCode": "560001",
  "latitude": 12.9716,
  "longitude": 77.5946
}
```

### GET /donors/{userId}

Public fields only. Private contact details are restricted by ownership and active request status.

### PATCH /donors/profile

Partial update for the authenticated donor.

### POST /ngos/profile

```json
{
  "ngoName": "Helping Hands Foundation",
  "registrationNumber": "KA/2019/0123456",
  "contactPerson": "Ravi Kumar",
  "phone": "+919812345678",
  "officeAddressLine": "45 Church Street",
  "city": "Bengaluru",
  "state": "Karnataka",
  "postalCode": "560001",
  "latitude": 12.9750,
  "longitude": 77.6000,
  "collectionCapacityMeals": 200,
  "peopleServedDaily": 500,
  "serviceRadiusKm": 15
}
```

### POST /ngos/verification

Multipart or presigned-document references.

```json
{
  "registrationDocumentRef": "doc_123",
  "taxExemptionDocumentRef": "doc_124",
  "authorizedSignatoryIdRef": "doc_125"
}
```

Response `202` with `verificationStatus: PENDING`.

### GET /ngos/{userId}

Public profile includes `verificationStatus` and service area; never private documents.

### GET /ngos/nearby

Query: `latitude`, `longitude`, `radiusKm`, `page`, `pageSize`.

## 6. File Uploads

### POST /uploads/presign

```json
{
  "purpose": "DONATION_IMAGE",
  "mimeType": "image/jpeg",
  "sizeBytes": 524288
}
```

Response:

```json
{
  "uploadUrl": "https://storage.example.com/presigned",
  "objectKey": "donations/img_123.jpg",
  "expiresIn": 300,
  "publicUrl": "https://cdn.example.com/donations/img_123.jpg"
}
```

Validation: allowed image MIME types, maximum size, server-side malware scanning. No Firebase Storage.

## 7. Donations

### POST /donations

Requires DONOR role and an active donor profile.

```json
{
  "title": "100 vegetarian meals from wedding reception",
  "description": "Freshly cooked meals, packed at 21:00.",
  "category": "COOKED_MEAL",
  "images": ["https://cdn.example.com/donations/img_123.jpg"],
  "totalQuantity": 100,
  "quantityUnit": "MEALS",
  "dietType": "VEGETARIAN",
  "preparedAt": "2026-11-05T20:30:00Z",
  "pickupWindowStart": "2026-11-05T21:30:00Z",
  "pickupWindowEnd": "2026-11-06T00:30:00Z",
  "collectionDeadline": "2026-11-06T01:00:00Z",
  "storageConditions": "Held in insulated containers at room temperature.",
  "packaging": "Sealed food-grade containers.",
  "allergens": ["DAIRY", "GLUTEN"],
  "pickupAddressLine": "Green Leaf Banquet Hall, 12 MG Road",
  "city": "Bengaluru",
  "state": "Karnataka",
  "postalCode": "560001",
  "latitude": 12.9716,
  "longitude": 77.5946,
  "safetyDeclaration": true
}
```

Validation:

- `collectionDeadline` must be after `preparedAt`.
- `pickupWindowEnd` cannot exceed `collectionDeadline`.
- `totalQuantity` must be greater than zero.
- `safetyDeclaration` must be true.
- Critical safety fields are required for PUBLISHED status.
- Coordinates are optional when manual address entry is used.

Response `201`:

```json
{
  "id": "don_123",
  "status": "PUBLISHED",
  "totalQuantity": 100,
  "remainingQuantity": 100,
  "version": 1,
  "createdAt": "2026-11-05T21:00:00Z"
}
```

### GET /donations

Requires authentication.

Query:

| Parameter | Meaning |
|---|---|
| `latitude`, `longitude` | Search origin |
| `radiusKm` | Maximum distance |
| `category` | Food category filter |
| `dietType` | Vegetarian filter |
| `minQuantity`, `maxQuantity` | Quantity filter |
| `deadlineBefore` | Deadline filter |
| `city` | Manual location fallback |
| `status` | Defaults to requestable statuses |
| `page`, `pageSize` | Pagination |

Only requestable, non-expired listings are returned for NGO discovery. Exact pickup address is omitted until approval.

### GET /donations/{id}

Returns full details for the owner, an approved NGO, or a discovery-safe projection for other authenticated NGOs.

### PATCH /donations/{id}

Owner only. Editing quantity is allowed only before any approved request. Editing is blocked after pickup scheduling.

### POST /donations/{id}/cancel

Owner only. Body: `{ "reason": "..." }`. Blocked after collection.

### GET /donations/mine

Query: `status`, `page`, `pageSize`.

### GET /donations/{id}/requests

Owner only. Returns requests with NGO public profile and verification status.

### POST /donations/{id}/handover

Owner confirms food handover.

```json
{ "pickupId": "pck_123", "confirmedAt": "2026-11-05T22:00:00Z" }
```

## 8. Donation Requests and Allocation

### POST /donations/{id}/requests

Requires a VERIFIED NGO.

```json
{
  "requestedQuantity": 60,
  "peopleToServe": 60,
  "expectedPickupAt": "2026-11-05T22:00:00Z",
  "note": "Volunteer with a refrigerated van."
}
```

Server rules:

- Donation must be PUBLISHED or PARTIALLY_ALLOCATED.
- Current time must be before `collectionDeadline`.
- `requestedQuantity` and `peopleToServe` must be positive.
- `requestedQuantity` must not exceed remaining quantity at submission time.
- One active request per NGO per donation.
- A pending request does not reserve or decrement quantity. Reservation occurs only on approval.
- Duplicate `Idempotency-Key` returns the original request.

Response `201`:

```json
{
  "id": "req_123",
  "donationId": "don_123",
  "status": "PENDING",
  "requestedQuantity": 60,
  "approvedQuantity": null,
  "peopleToServe": 60,
  "expectedPickupAt": "2026-11-05T22:00:00Z",
  "createdAt": "2026-11-05T21:10:00Z"
}
```

### GET /requests/mine

Query: `roleContext=ngo|donor`, `status`, pagination.

### GET /requests/{id}

Accessible to the owning NGO and the donation owner.

### POST /requests/{id}/approve

Donor only.

```json
{ "approvedQuantity": 60, "pickupWindowStart": "2026-11-05T22:00:00Z", "pickupWindowEnd": "2026-11-05T23:00:00Z" }
```

Rules:

- Approved quantity cannot exceed requested quantity or remaining quantity.
- Server allocates atomically and updates donation status.
- Rejected if donation is expired, cancelled, or on safety hold.

### POST /requests/{id}/reject

Donor only: `{ "reason": "..." }`.

### POST /requests/{id}/cancel

NGO owner or donor. Releasing an approved allocation returns quantity to the donation only if pickup has not been collected.

### POST /requests/{id}/counteroffer

Optional future capability. Contract reserved.

## 9. Pickup and Distribution

Pickup is a separate auditable workflow. Approval does not mean food was collected. The authoritative sequence is:

`PENDING -> APPROVED -> PICKUP_SCHEDULED -> NGO_READY -> DONOR_READY -> READY -> IN_PROGRESS -> COLLECTED -> DISTRIBUTED -> COMPLETED`

An exception may move an active pickup to `CANCELLED`, `MISSED`, or `FAILED`. Completed donations are terminal; reopening requires an explicit correction endpoint outside this Android API surface.

### POST /requests/{id}/pickup

Either party may create or update scheduling before collection. Requires an `APPROVED` request.

```json
{
  "scheduledAt": "2026-11-05T22:00:00Z",
  "contactPerson": "Sunil Mehta",
  "collectionInstructions": "Use the loading dock.",
  "expectedQuantity": 60,
  "volunteerName": "Ravi Kumar",
  "volunteerPhone": "+919812345678"
}
```

### PATCH /pickups/{id}

Update schedule, contact, or volunteer details before collection only.

### POST /pickups/{id}/ngo-ready

NGO confirms volunteer readiness. Body may include `volunteerName`, `volunteerPhone`, and `evidenceRefs`.

### POST /pickups/{id}/donor-ready

Donor confirms handover readiness. Body may include `contactPerson`, `collectionInstructions`, and `evidenceRefs`.

### POST /pickups/{id}/start

Moves a ready pickup to `IN_PROGRESS`.

### POST /pickups/{id}/collected

NGO confirms physical collection and records the **actual** quantity.

```json
{
  "collectedAt": "2026-11-05T22:15:00Z",
  "actualCollectedQuantity": 55,
  "note": "Five packs were damaged.",
  "proofImageRefs": ["doc_200"]
}
```

Server rules:

- Both readiness confirmations must exist before collection.
- `actualCollectedQuantity` must be positive and cannot exceed `approvedQuantity`.
- If `actualCollectedQuantity < approvedQuantity`, the uncollected allocation is released atomically and a quantity-discrepancy event is written.
- Collection updates pickup, request, and donation status to `COLLECTED` and prevents duplicate collection confirmations.

### POST /pickups/{id}/distributed

NGO confirms aggregate distribution after collection.

```json
{
  "actualReceivedQuantity": 55,
  "actualDistributedQuantity": 52,
  "beneficiariesServed": 60,
  "distributedAt": "2026-11-05T23:30:00Z",
  "location": "Community kitchen, Shivajinagar",
  "remainingQuantity": 3,
  "notes": "Served to 60 people.",
  "evidenceRefs": ["doc_201"]
}
```

Rules:

- Collection must be confirmed first.
- `actualDistributedQuantity` cannot exceed `actualCollectedQuantity`.
- Records must contain aggregate counts only; beneficiary names, phone numbers, addresses, and identity documents are prohibited.
- `evidenceRefs` are server object keys, not personal information.

### POST /pickups/{id}/complete

Donor closes the pickup after distribution. Completion updates pickup, request, and donation to `COMPLETED` and writes an audit event.

### POST /pickups/{id}/exception

Either party records an exception for an active pickup.

```json
{
  "reason": "VEHICLE_FAILURE",
  "note": "NGO vehicle broke down.",
  "evidenceRefs": ["doc_202"]
}
```

Server rules:

- Cancels the pickup and request and releases any uncollected allocation atomically.
- Writes an auditable exception event and notifies the other party.
- Does not delete any earlier timeline event.

### GET /pickups

Query: `roleContext=donor|ngo`, `status`, `from`, `to`, `page`, `pageSize`.

### GET /pickups/{id}

Accessible only to the owning NGO and the donation owner. Returns pickup details, distribution data if present, and current status.

### GET /pickups/{id}/timeline

Returns immutable workflow events ordered by `occurredAt`: actor, role, stage, note, evidence refs, and created timestamp.

## 10. Notifications

### GET /notifications

Query: `unreadOnly`, pagination.

### PATCH /notifications/{id}/read

Marks one notification as read.

### POST /notifications/read-all

Marks all as read.

### POST /devices/register

```json
{ "token": "fcm-or-provider-token", "platform": "ANDROID", "appVersion": "1.0.0" }
```

Push provider is abstracted; no provider-specific client code leaks into ViewModels.

Notification types:

```
NEW_DONATION_REQUEST
REQUEST_APPROVED
REQUEST_REJECTED
REQUEST_CANCELLED
PICKUP_SCHEDULED
PICKUP_REMINDER
DEADLINE_APPROACHING
PICKUP_NGO_READY
PICKUP_DONOR_READY
PICKUP_COLLECTED
COLLECTION_CONFIRMED
DISTRIBUTION_CONFIRMED
PICKUP_EXCEPTION
DONATION_COMPLETED
NGO_VERIFICATION_APPROVED
NGO_VERIFICATION_REJECTED
ACCOUNT_SUSPENDED
SAFETY_REPORT_UPDATE
```

## 11. Safety Reports

### POST /donations/{id}/reports

Requires NGO with an approved or collected request.

```json
{
  "reason": "TEMPERATURE_ABUSE",
  "description": "Food arrived warm although storage declaration stated refrigerated.",
  "evidenceRefs": ["doc_300"]
}
```

### GET /safety-reports/mine

Returns the reporter's reports.

Admin review endpoints are web-only and not part of the Android API surface.

## 12. History and Statistics

### GET /history

Query: `role`, `status`, `from`, `to`, pagination.

### GET /stats/me

```json
{
  "donationsPublished": 12,
  "mealsDonated": 840,
  "requestsApproved": 10,
  "mealsCollected": 790,
  "mealsDistributed": 790,
  "activeDonations": 2
}
```

### GET /stats/platform

Public aggregate figures only; no personal data.

## 13. MongoDB Collections and Indexes

```
users             { _id, name, email(unique), phone, passwordHash, role, accountStatus, createdAt, updatedAt }
donorProfiles     { _id, userId(unique), donorType, organizationName, address, location(2dsphere), createdAt, updatedAt }
ngoProfiles       { _id, userId(unique), ngoName, registrationNumber, verificationStatus, location(2dsphere), capacity, createdAt, updatedAt }
documents         { _id, ownerUserId, purpose, objectKey, mimeType, status, createdAt }
donations         { _id, donorId, title, description, category, images, totalQuantity, remainingQuantity, quantityUnit, dietType, preparedAt, pickupWindowStart, pickupWindowEnd, collectionDeadline, location(2dsphere), address, safety, status, version, createdAt, updatedAt }
donationRequests  { _id, donationId, ngoId, requestedQuantity, approvedQuantity, status, estimatedPickupAt, idempotencyKey(unique), createdAt, updatedAt }
pickups           { _id, requestId(unique), donationId, ngoId, donorId, status, schedule, ngoReady, donorReady, actualCollectedQuantity, quantityDiscrepancy, collectedAt, proofImageRefs, exceptionReason, distribution, version, createdAt, updatedAt }
distributions     { _id, pickupId(unique), requestId, donationId, actualReceivedQuantity, actualDistributedQuantity, beneficiariesServed, distributedAt, location, remainingQuantity, notes, evidenceRefs }
pickupEvents      { _id, pickupId, requestId, donationId, stage, actorId, actorRole, note, evidenceRefs, occurredAt }
notifications     { _id, userId, type, title, body, referenceId, isRead, createdAt }
donationHistory   { _id, donationId, requestId, eventType, actorId, actorRole, note, occurredAt }
safetyReports     { _id, donationId, requestId, reporterId, reason, description, evidenceRefs, status, resolution, createdAt, updatedAt }
idempotencyKeys   { _id, key(unique), userId, endpoint, requestHash, responseStatus, responseBody, createdAt, expiresAt }
```

Indexes:

- `donations`: `{ status: 1, collectionDeadline: 1 }`, `{ location: "2dsphere" }`, `{ donorId: 1, createdAt: -1 }`
- `donationRequests`: unique `{ donationId: 1, ngoId: 1, active: 1 }`, `{ ngoId: 1, status: 1 }`
- `pickups`: unique `{ requestId: 1 }`, `{ donorId: 1, status: 1, createdAt: -1 }`, `{ ngoId: 1, status: 1, createdAt: -1 }`
- `pickupEvents`: `{ pickupId: 1, occurredAt: 1 }`, `{ requestId: 1, occurredAt: 1 }`
- `notifications`: `{ userId: 1, isRead: 1, createdAt: -1 }`
- `idempotencyKeys`: TTL on `expiresAt`

## 14. Transaction-Safe Allocation

The backend must not rely on client-side checks. A pending request is only an expression of interest; capacity is reserved when the donor approves it. The request transition, quantity decrement, donation status update, notification and audit record must all succeed or fail together in one MongoDB transaction.

```js
const session = await mongoose.startSession();
await session.withTransaction(async () => {
  const request = await DonationRequest.findOneAndUpdate(
    { _id: requestId, status: "PENDING" },
    { $set: { status: "APPROVED", approvedQuantity } },
    { new: true, session }
  );
  if (!request) throw conflict("REQUEST_NOT_PENDING");

  const donation = await Donation.findOneAndUpdate(
    {
      _id: request.donationId,
      status: { $in: ["PUBLISHED", "PARTIALLY_ALLOCATED"] },
      collectionDeadline: { $gt: new Date() },
      remainingQuantity: { $gte: approvedQuantity }
    },
    { $inc: { remainingQuantity: -approvedQuantity, version: 1 } },
    { new: true, session }
  );
  if (!donation) throw conflict("QUANTITY_EXCEEDED");

  await updateDonationStatusFromRemaining(donation, session);
  await Notification.create([/* request approved payload */], { session });
  await AuditLog.create([/* allocation event */], { session });
});
```

On rejection before approval no quantity changes. On cancellation of an approved or scheduled request before collection, return the approved quantity atomically.

Pickup collection uses the same transaction discipline: the pickup status transition, actual quantity, donation allocation release for a short collection, request status, donation status, notifications, and pickup timeline event must all succeed or fail together. Distribution, completion, and exception transitions must also write their audit events in the same transaction as status changes. The backend must reject stale transitions from any client with `409 INVALID_TRANSITION`; client mocks are convenience state only.

Local debug mocks can only provide atomicity inside one Android process; they are not a cross-device reservation or an authoritative record.

## 15. Future AI Extensions

Reserved, advisory only:

- Demand forecasting from historical distribution data.
- Surplus trend analysis.
- NGO recommendation scoring by distance, capacity, and pickup time.
- Donation description generation.
- Anomaly detection for unusual donation patterns.

AI must never override safety rules, verification, or authorization. Rule-based behavior must not be labeled as machine learning.

## 16. Android Mock Mapping

Debug mocks must implement the same repository methods and honor the same state transitions. Mock mode is indicated in debug UI. Mock repositories are excluded from release builds by source-set separation and `BuildConfig.USE_MOCK_DATA`.
