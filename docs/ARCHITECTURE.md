# ShareMeal Architecture

Share Food. Spread Happiness.

## 1. Project Identity

| Item | Decision |
|---|---|
| Application name | ShareMeal |
| Application ID | com.sharemeal.app |
| Namespace | com.sharemeal.app |
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Repository + use cases |
| Local persistence | Room |
| Remote integration | Retrofit + OkHttp + kotlinx.serialization |
| DI | Hilt |
| Navigation | Navigation Compose |
| Async | Coroutines + Flow, StateFlow for UI |
| Background | WorkManager |
| Preferences | DataStore |
| Images | Android Photo Picker + Coil; upload abstraction |
| Target market | India |
| Food-safety basis | Configurable rules informed by FSSAI guidance |
| Maps | Deferred to Phase 8 |
| Admin | Not part of the mobile application |

## 2. Backend Strategy

There is no existing backend. ShareMeal is mock-first until a backend is delivered.

- Android development runs against repository interfaces and deterministic mock implementations.
- Mock implementations live only in the `debug` source set and cannot be compiled into a release build.
- Production repository implementations live in `main` or `release` and use Retrofit services.
- Mock data is never placed in production repositories or production DI modules.
- A complete REST contract is maintained in `docs/API_CONTRACT.md`.
- The future backend is Node.js + Express + MongoDB with server-side authorization, transactional allocation, and audit logging.
- No successful API response is simulated inside production code. Mock mode is surfaced in debug UI.

### Mock-mode isolation

- `BuildConfig.USE_MOCK_DATA` is `true` for debug and `false` for release.
- Debug-only DI modules bind fake repositories.
- Release DI must bind only network-backed repositories.
- Mock mode must be visible in debug builds.
- A release build with mock mode enabled is a release-blocking defect.

## 3. Layer Model

```
Compose screen
  -> ViewModel (StateFlow<UiState>)
    -> Use case (validation, state transitions, allocation rules)
      -> Repository interface
        -> Local data source (Room DAO)
        -> Remote data source (Retrofit)
        -> Sync coordinator (WorkManager outbox)
```

Rules:

- Screens do not call Retrofit or Room directly.
- Repositories expose Room-backed Flows as the observable source of truth.
- The backend is authoritative for shared donation state.
- A donation or request is not shown as successful until the server confirms it, except when explicitly marked pending sync.
- All mutations carry an idempotency key.
- Status changes flow through one explicit state machine.

## 4. Package Structure

```
com.sharemeal.app
├── ShareMealApplication.kt
├── MainActivity.kt
├── core/
│   ├── common/
│   ├── config/
│   ├── constants/
│   ├── database/
│   ├── location/
│   ├── media/
│   ├── navigation/
│   ├── network/
│   ├── notifications/
│   ├── security/
│   ├── ui/
│   └── utils/
├── data/
│   ├── local/
│   ├── remote/
│   ├── repository/
│   ├── mapper/
│   └── sync/
├── domain/
│   ├── model/
│   ├── repository/
│   └── usecase/
├── feature/
│   ├── splash/
│   ├── onboarding/
│   ├── authentication/
│   ├── dashboard/
│   ├── donor/
│   ├── ngo/
│   ├── donation/
│   ├── discovery/
│   ├── requests/
│   ├── pickup/
│   ├── tracking/
│   ├── notifications/
│   ├── profile/
│   ├── history/
│   └── settings/
├── di/
└── ui/theme/
```

The project stays a single `:app` module until the domain and API are stable. Feature and core boundaries are enforced by package discipline first, then extracted into Gradle modules.

## 5. Data Design

Room entities planned for implementation in Phase 7:

- UserEntity
- DonorProfileEntity
- NgoProfileEntity
- FoodDonationEntity
- DonationRequestEntity
- PickupEntity
- NotificationEntity
- DonationHistoryEntity
- PendingSyncEntity

Every server-backed entity stores its server identifier, `serverUpdatedAt`, `syncState`, and `isDirty`. Enums are persisted as strings. Timestamps use epoch milliseconds. Exported Room schemas and explicit migrations are mandatory.

Offline behavior:

- Cache listings, profiles, requests, notifications, and history.
- Queue mutations in a PendingSync outbox with unique idempotency keys.
- Replay with WorkManager on connectivity.
- Show pending/conflict states explicitly.
- Reject expired or withdrawn listings at query time and on the server.

## 6. Donation Lifecycle

```
PUBLISHED
  -> REQUESTED
    -> APPROVED
      -> PICKUP_SCHEDULED
        -> COLLECTED
          -> DISTRIBUTED
            -> COMPLETED
```

Alternative terminal states: REJECTED, CANCELLED, EXPIRED, SAFETY_HOLD.

Partial allocation is supported. The server performs atomic conditional quantity updates inside a MongoDB transaction. The client mirrors allocation for display only.

## 7. Authentication and Security

- JWT access token and refresh token.
- Keystore-backed encrypted token storage, implemented in Phase 2.
- OkHttp authenticator for single-flight refresh.
- Server-side role and ownership enforcement for every sensitive action.
- HTTPS only in production.
- No secrets in source control or BuildConfig.
- Input validation on client and server.
- Safe image upload with MIME and size validation.
- No personal data in logs.
- Account deletion and session expiry required before release.

## 8. Location, Maps, and Images

Location:

- Manual address entry is always available.
- Location abstractions are prepared now; implementation arrives in Phase 8.
- Fused Location Provider is planned, foreground-only.
- Exact pickup coordinates are revealed only after request approval.
- Distance is computed by the backend or a shared geospatial query.
- Google Maps is deferred; no API key is required in Phases 1-7.

Images:

- Android Photo Picker for selection.
- Local preview with Coil.
- Placeholder image when none is selected.
- Image repository abstraction for future presigned uploads.
- No Firebase Storage.

## 9. Food Safety (India)

- Capture preparation time, collection deadline, storage conditions, packaging, allergens, and donor declaration.
- Rules are configurable and delivered by backend configuration; no invented universal safe durations.
- FSSAI guidance is the reference for future rule configuration.
- Missing critical safety data produces visible warnings.
- NGOs can report unsafe food; administrators investigate through the future web console.
- ShareMeal never certifies food as safe.

## 10. Verification and Administration

- NGO verification status is enforced by the backend.
- Unverified NGOs cannot accept unrestricted donations.
- Mobile contains no admin routes or privileges.
- Future admin web capabilities use dedicated admin APIs and roles.

## 11. Navigation Map

```
Splash -> Onboarding -> Auth
Auth -> Login / Register / Forgot Password / OTP
Login -> Donor Graph or NGO Graph (role from server)
Donor: Dashboard, Add Donation, My Donations, Requests, Tracking, History, Notifications, Profile, Settings
NGO: Dashboard, Discovery, Food Details, Request, Pickups, Distribution, History, Notifications, Profile, Settings
Shared: Profile edit, location, privacy, terms, help, report unsafe food
```

Phase 1 provides the navigation skeleton with clearly marked placeholder destinations. Phase 2 replaces authentication and onboarding placeholders with real implementations.

## 12. API Strategy

- Base path: `/api/v1`.
- JSON over HTTPS.
- Pagination on list endpoints.
- Idempotency-Key on all mutations.
- Typed error body with machine-readable codes.
- Partial allocation protected by server-side atomic updates.
- WebSocket or SSE can be added later behind a notification/refresh abstraction.

Full contract: `docs/API_CONTRACT.md`.

## 13. Testing Strategy

- Unit tests: validation, state transitions, allocation rules, ViewModel logic.
- Database tests: entities, relationships, DAOs, migrations.
- UI tests: auth, role navigation, donation creation, request, approval, pickup.
- Integration tests: MockWebServer for Retrofit, fake repositories for flows, offline sync.
- Release checks: mock-mode guard, manifest permissions, ProGuard/R8, signed AAB.

## 14. Roadmap

| Phase | Scope | Gate |
|---|---|---|
| 0 | Inspection and architecture | Complete |
| 1 | Foundation, theme, navigation, DI, components | Current |
| 2 | Onboarding, auth, role profiles, secure session | Approval |
| 3 | Donor dashboard and donation creation | Approval |
| 4 | NGO dashboard and discovery | Approval |
| 5 | Requests, approval, allocation | Approval |
| 6 | Pickup, collection, distribution | Approval |
| 7 | Room cache, offline, outbox sync | Approval |
| 8 | Location, notifications, real-time refresh | Approval |
| 9 | Safety validation, verification, admin contract | Approval |
| 10 | Analytics, tests, security and performance | Approval |
| 11 | Release configuration and Play Store materials | Approval |

## 15. Key Risks

| Risk | Mitigation |
|---|---|
| Backend absent | Contract-first, debug-only mocks, no false success states |
| Over-allocation | Server transaction and conditional update |
| Duplicate submissions | Idempotency keys and unique indexes |
| Stale listings | Deadline and status checks, server refresh |
| Offline conflicts | Version checks and explicit conflict UI |
| Verification abuse | Server-side role verification and audit |
| Location privacy | Foreground-only access, reveal after approval |
| Food-safety liability | Declarations, configurable warnings, no certification |
| Play Store compliance | Privacy policy, data safety, account deletion, signed AAB |
| Bleeding-edge toolchain | Pin versions, rebuild after each change, test on API 24+ |
