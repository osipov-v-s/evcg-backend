# Career-guidance project guide

## Repositories

- Backend: this Gradle/Spring Boot project (`src/main/java/com/profession/suggest`).
- Frontend: `E:/PROJECTS/evcg/evcg-frontend/evcg-web`, React + TypeScript + Vite.

## Active roles and authorization

Active roles are `ADMIN`, `SPECIALIST`, `PUPIL`, and `CURATOR`. Historical enum values remain readable solely for migration compatibility and cannot register or log in as active users. JWT creation and role checks filter to active roles. MVC paths requiring a token are listed in `configuration/WebConfig`; method-level access is enforced by `@HasRole` and `RoleAspect`.

## Main domain

- `Account` and `Role`: authentication identities and role assignments.
- `Pupil` → `School`: a structured nullable `school_id` link plus the legacy text `school` field during migration.
- `Curator` → `School`: a curator belongs to exactly one educational organization; `PupilService` enforces school-scoped reads and reports.
- `Specialist` → `Profession` → `Company`: reference professional profile and informational workplace.
- `PsychTest`, `PsychParam`, `PsychTestType`: shared pupil/specialist psychodiagnostic results. Test types have an admin-controlled active flag.
- `PupilGrade`, `Subject`, `PupilSubjectProfile`: educational profile.
- `VRTest`, `VRTestAnswer`, `VRTestType`: profession-linked VR tests, separate from simulations.
- `Simulation`, `Scenario`, `SimulationType`, `SimulationDataSource`: uploaded VR/external simulation files and metadata.
- `ComparisonCollection`/`ComparisonSession`: external comparison sessions, stored files, eye/face tracking availability metadata.
- `Prediction`: immutable pupil-profession calculation history. Every explicit recalculation currently creates a new row linked to `Pupil`, `Profession`, the nearest `Specialist`, and `PredictionType`; changing that history policy requires a product decision.

## Controllers and notable API

- `AuthController`: registration, login, active role retrieval and password update.
- `PupilController`: own profile, school selection from the directory, server-side pupil pagination/filters and predictions. A curator request is always restricted to the school linked to the authenticated curator.
- `SchoolController` / `CuratorController`: school create/update, curator create/list/update, own curator profile, school-scoped result export and a school-scoped pupil password reset to the temporary value `123123`.
- `SpecialistController`: specialist profiles, professions, reference-data administration and server-side list filters.
- `CompanyController`: informational company directory and the specialist's own workplace.
- `PsychTestsController`: result submission/reads plus admin activation of test types.
- `VRTestController`: per-account VR tests plus admin activation of VR test types.
- `SimulationController`: authenticated file ingestion and admin filtering by date, user, profession, scenario, type and source.
- `ComparisonController`: authenticated external session ingestion/ownership plus admin collection/session views.
- `PredictionController`: pupil calculation/latest result and school-scoped curator/admin reads.

Request/response contracts live in `dto`; the matching frontend clients live in `src/services/api`. Pupil and specialist result exports use `AccountTestsDTO`, which includes role-specific profile fields. The curator result endpoint never accepts a school identifier from the client: it resolves the school from the authenticated account. External multipart simulation ingestion keeps `POST /api/simulations/create` with `metadata` and `file`, but now requires JWT and enforces pupil ownership unless the caller is ADMIN.

## Frontend navigation

`src/App.tsx` defines route groups. `routing/roleAccess.ts`, `components/ui/menu/optionsData.ts` and `routing/RolesProtectedRoute.tsx` keep route and menu access aligned:

- `PUPIL`: psychological/VR tests, educational profile, profession matching, own results and profile.
- `SPECIALIST`: psychological/VR tests, own results and profile. Pupil-only prediction and grade routes are not exposed.
- `CURATOR`: separate `/curator` workspace plus `/profile`; no test or admin navigation.
- `ADMIN`: `/admin` only for domain administration and exports.

Admin pupil and specialist pages combine list, server-side filters, pagination, XLSX registration and download actions. The school page manages schools and their curator accounts. Test-type activity, simulations, companies and safe reference CRUD remain separate admin areas. The curator workspace lists only the server-scoped pupils, supports explicit filters, confirmation before password reset, and JSON/XLSX result export for the curator's school.

Common page headings, filter bars, confirmation dialogs, loading/empty states and action styles live under `components/ui/common`. API failures are translated to human-readable messages by `services/api/error.ts`.

## Prediction integration

The supported synchronous flow and its concrete implementation are:

```text
Frontend Results
  src/components/predictions/Predictions.tsx
  src/services/api/predictionApi.ts
-> Java POST /api/predictions/predict
  PredictionController.predict()
-> Java integration and persistence
  PredictionService.predictByAccount()
  PredictionRequest / PredictionResponse
-> FastAPI POST /predict
  main.py -> prediction.py
-> Python mapping and calculation
  mapping.py -> clusters.py -> prediction.py
-> Java validates pupilId/domain references, stores Prediction, and returns the stored response
-> Frontend renders the new result
```

The Java endpoint accepts no pupil ID from the browser. It derives the current `Pupil` from the authenticated account and rejects a Python response carrying another `pupilId`. Python cluster generation reads specialist profiles from the protected legacy-compatible `GET /api/specialists/reference-data` endpoint through a dedicated ADMIN service account; credentials belong only in the Python service's untracked `.env`.

Java configuration is externalized as `PREDICTION_SERVICE_URL`, `PREDICTION_SERVICE_CONNECT_TIMEOUT_MS`, and `PREDICTION_SERVICE_READ_TIMEOUT_MS`; safe local examples are in `.env.example`. Python runtime setup, `/predict`, `/health`, generated model lifecycle, and its 24-hour TTL are documented in the Prediction Service `README.md`.

## Database and files

Flyway is declared in Gradle but disabled in the current local properties. The additive migration is under `src/main/resources/db/migration`; follow `DATABASE_MIGRATION_PLAN.md` before enabling it on a restored database copy. Destructive legacy cleanup is isolated under `database/manual` and must not run automatically.

Simulation uploads are handled by `SimulationService`/`FileStorageService`. Eye/face session uploads are handled by `ComparisonSessionService`. EEG or other channels without dedicated typed entities remain represented by external uploaded files/metadata and are not fabricated by the web application.

## Commands

Backend (JDK 17): `./gradlew test` and `./gradlew build` (Windows: `gradlew.bat`). Set `PREDICTION_SERVICE_URL=http://127.0.0.1:8000/predict` for the local Python service.

Frontend: `npm ci`, `npx tsc --noEmit`, `npm test -- --runInBand`, and `npm run build`. Jest covers the role matrix and the pupil/curator API boundaries under `src/**/*.test.ts`.

Visual verification should cover the public home, login and registration flows at desktop, tablet and mobile widths. Protected role workspaces additionally require an authenticated test environment with non-production fixture data; do not point this workflow at a production database.
