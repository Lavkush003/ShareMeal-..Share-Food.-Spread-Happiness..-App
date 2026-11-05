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
PickupStatus: SCHEDULED | IN_PROGRESS | COLLECTED | CANCELLED | FAILED
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
  "estimatedPickupAt": "2026-11-05T22:00:00Z",
  "note": "Volunteer with a refrigerated van."
}
```

Server rules:

- Donation must be PUBLISHED or PARTIALLY_ALLOCATED.
- Current time must be before `collectionDeadline`.
- `requestedQuantity` must be positive and not exceed remaining quantity.
- One active request per NGO per donation.
- Allocation is performed with an atomic conditional update inside a MongoDB transaction.
- Duplicate `Idempotency-Key` returns the original request.

Response `201`:

```json
{
  "id": "req_123",
  "donationId": "don_123",
  "status": "PENDING",
  "requestedQuantity": 60,
  "estimatedPickupAt": "2026-11-05T22:00:00Z",
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

### POST /requests/{id}/pickup

NGO creates or updates pickup scheduling.

```json
{
  "scheduledAt": "2026-11-05T22:00:00Z",
  "volunteerName": "Ravi Kumar",
  "volunteerPhone": "+919812345678",
  "vehicleDetails": "KA01AB1234"
}
```

### PATCH /pickups/{id}

Update schedule or volunteer details before collection.

### POST /pickups/{id}/start

Marks pickup as IN_PROGRESS.

### POST /pickups/{id}/collected

NGO confirms collection with optional proof image and notes.

```json
{ "collectedAt": "2026-11-05T22:15:00Z", "proofImageRef": "doc_200", "note": "Sealed containers verified." }
```

### POST /pickups/{id}/distributed

NGO confirms distribution.

```json
{
  "distributedAt": "2026-11-05T23:30:00Z",
  "mealsDistributed": 60,
  "distributionLocation": "Community kitchen, Shivajinagar",
  "notes": "Served to 60 people."
}
```

Rules:

- Distributed quantity cannot exceed approved quantity.
- Collection must be confirmed first.
- Completion updates both request and donation status.

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
COLLECTION_CONFIRMED
DISTRIBUTION_CONFIRMED
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
pickups           { _id, requestId(unique), donationId, ngoId, scheduledAt, volunteer, status, collectedAt, distributedAt, mealsDistributed, proofImageRef }
notifications     { _id, userId, type, title, body, referenceId, isRead, createdAt }
donationHistory   { _id, donationId, requestId, eventType, actorId, actorRole, note, occurredAt }
safetyReports     { _id, donationId, requestId, reporterId, reason, description, evidenceRefs, status, resolution, createdAt, updatedAt }
idempotencyKeys   { _id, key(unique), userId, endpoint, requestHash, responseStatus, responseBody, createdAt, expiresAt }
```

Indexes:

- `donations`: `{ status: 1, collectionDeadline: 1 }`, `{ location: "2dsphere" }`, `{ donorId: 1, createdAt: -1 }`
- `donationRequests`: unique `{ donationId: 1, ngoId: 1, active: 1 }`, `{ ngoId: 1, status: 1 }`
- `notifications`: `{ userId: 1, isRead: 1, createdAt: -1 }`
- `idempotencyKeys`: TTL on `expiresAt`

## 14. Transaction-Safe Allocation

The backend must not rely on client-side checks.

```js
const donation = await Donation.findOneAndUpdate(
  {
    _id: donationId,
    status: { $in: ["PUBLISHED", "PARTIALLY_ALLOCATED"] },
    collectionDeadline: { $gt: new Date() },
    remainingQuantity: { $gte: requestedQuantity }
  },
  { $inc: { remainingQuantity: -requestedQuantity }, $inc: { version: 1 } },
  { new: true, session }
);
```

If `donation` is null, return `409 QUANTITY_EXCEEDED` or `409 DONATION_NOT_AVAILABLE`. Create the request and audit record inside the same transaction. On rejection or cancellation before collection, return quantity atomically.

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
