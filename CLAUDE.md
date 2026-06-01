# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository Layout

Three independent codebases coexist in this monorepo:

- `apps/android/` — Android app (Java) running on the Sanbot robot (Android 21+, Sanbot OpenSDK). Built via Gradle. Currently mid-migration to Clean Architecture + MVVM (see commit `924a7fe`).
- `services/backend/` — Python FastAPI service ("Visitor Notify Assistant MVP"). Brokers notifications across Telegram, Email and WhatsApp; hosts a Telegram onboarding bot; serves an admin HTTP UI.
- `services/whatsapp-bridge/` — Node/TypeScript microservice wrapping Baileys (`@whiskeysockets/baileys`). Exposes a small REST API the backend calls to send WhatsApp messages.

The robot Android app calls the backend over HTTP (`VISITOR_API_BASE_URL`, configured via Gradle `local.properties`). The backend calls the whatsapp-bridge over HTTP using an `INTERNAL_API_KEY` header.

## Common Commands

### Backend (Python / FastAPI)

```bash
# from repo root
pip install -r services/backend/requirements.txt

# run dev server (module path matters — main.py uses `backend.app.*` imports)
# Option A: run from services/ so backend.app.* resolves correctly
cd services && uvicorn backend.app.main:app --reload
# Option B: run from repo root with explicit PYTHONPATH
PYTHONPATH=services uvicorn backend.app.main:app --reload

# run all tests (from repo root)
pytest services/backend/tests

# run one test file / one test
pytest services/backend/tests/unit/test_notification_service.py
pytest services/backend/tests/unit/test_notification_service.py::test_submit_routes_whatsapp
```

`services/backend/pyproject.toml` sets `pythonpath = ["../"]` for pytest, so `from backend.app.*` imports resolve when running `pytest services/backend/tests` from the repo root.

### whatsapp-bridge (Node / TypeScript)

```bash
cd services/whatsapp-bridge
npm install
npm run dev      # tsx watch src/main.ts
npm run build    # tsc → dist/
npm start        # node dist/main.js
```

Requires env: `INTERNAL_API_KEY`, `AUTH_DIR`, optional `PORT` (default 3001), `RECONNECT_INTERVAL_MS`. First run produces a QR code on stdout to pair the WhatsApp account; session persisted under `AUTH_DIR`.

### Android app (Sanbot robot)

```bash
cd apps/android
./gradlew assembleDebug          # build APK
./gradlew installDebug           # install on connected robot (adb)
```

Build-time secrets are passed through `local.properties` (or `-P` flags) and surfaced as `BuildConfig` fields: `OPENAI_API_KEY`, `ORGANIZATION_ID`, `PROJECT_ID`, `VISITOR_API_BASE_URL`, `VISITOR_DEVICE_ID`, `VISITOR_LOCATION_LABEL`, `VISITOR_FLOW_ENABLED`. The robot must be in Developer Mode (Settings → About → tap "Robot Version" 7×) and connected by USB.

`local.properties` must be placed at `apps/android/local.properties` (gitignored). The Gradle project root is `apps/android/`.

Target SDK: `compileSdkVersion 26`, `minSdkVersion 21`. The Sanbot SDK is vendored under `apps/android/libs/SanbotOpenSDK_2.0.1.10.aar`.

## Backend Architecture

The backend follows Clean / Hexagonal Architecture. Layers are physical directories under `services/backend/app/`:

- `domain/` — entities + ports (Protocols). No framework imports. Key types:
  - `Contact`, `NotificationRequest`, `NotificationOutcome`, `ChannelDelivery`
  - `providers.py` defines `TelegramProvider`, `EmailProvider`, `WhatsappProvider` Protocols
  - `NotificationService` orchestrates fan-out across the three channels and is the single business-logic entry point for "send a notification"
  - `TelegramOnboardingService` handles the contact-registration flow used by the Telegram bot
- `application/` — use-case orchestration over domain services (e.g. `telegram_onboarding_service.py` shaping bot responses).
- `infrastructure/` (new) and `infra/` (older parallel tree) — adapters implementing the domain Protocols:
  - `infrastructure/json/` JSON-file repositories for contacts and pending registrations
  - `infrastructure/providers/` and `infra/providers/` Telegram, Email (Mailtrap / SMTP / Stub), WhatsApp (`WhatsAppBridgeProvider` → HTTP to whatsapp-bridge) implementations
  - Both trees currently exist mid-migration; new code should land under `infrastructure/`. When editing, check both before assuming a file is unique.
- `api/routes/` — FastAPI route registrars (`register_*_routes(app, ...)`). Routers receive their dependencies (repositories, services) as constructor-style args — no global state, no FastAPI `Depends`.
- `presentation/http/` — alternative HTTP router modules (newer style; parallels `api/routes`).
- `presentation/telegram/`, `bot/` — `python-telegram-bot` `Application` factory and polling driver for the onboarding bot. `bot/` re-exports from `presentation/telegram/`.
- `bootstrap/` — composition root.
  - `settings.py` (`BackendSettings.from_env`) resolves every env var (prefix: `VISITOR_NOTIFY_*`) and runtime data paths. It checks `services/backend/runtime_data/<file>.json` first, then legacy `services/backend/app/data/<file>.json`.
  - `runtime.py` (`build_runtime`) wires the entire object graph: repositories → providers (real vs. stub based on whether credentials are present and `ALLOW_STUB_DELIVERY`) → services. Returns a frozen `BackendRuntime` dataclass.
- `main.py` — FastAPI factory `create_app()`; builds the runtime once and hands services to each `register_*_routes` call. `app` is `None` if FastAPI isn't installed, so tests can import `main` without the dependency.

Provider selection rule (see `bootstrap/runtime.py`): when no credentials are present, providers fall back to `Stub*` implementations whose status is `"accepted"` if `ALLOW_STUB_DELIVERY=true` else `"unavailable"`/`"skipped"`. Tests rely on this.

When adding a new channel, follow the existing pattern: define the port in `domain/providers.py`, implement the adapter under `infrastructure/providers/`, wire it in `bootstrap/runtime.py`, inject into `NotificationService`, and extend `NotificationOutcome` if a new per-channel `ChannelDelivery` field is needed.

## whatsapp-bridge Architecture

- `src/main.ts` — bootstrap: start Express, then start the WhatsApp service.
- `src/app.ts` — Express app. Global middleware enforces `x-api-key` against `INTERNAL_API_KEY` for every route except `/health`.
- `src/routes/{health,session,messages}.ts` — route handlers.
- `src/services/whatsapp.service.ts` — singleton wrapping the Baileys client lifecycle (connect, QR, reconnect, send).
- `src/adapters/baileys/client.ts` — Baileys-specific glue.
- `src/config/env.ts` — env loading with `dotenv`; throws on missing required vars at startup.

The backend's `WhatsAppBridgeProvider` (`services/backend/app/infra/providers/whatsapp_provider.py`) is the only intended client.

## Android App Architecture

The Java codebase is split between legacy and the in-progress Clean/MVVM refactor:

- Legacy code lives directly under `com.mundosvirtuales.visitorassistant.*` as `My*Activity.java` files (Calendar, Weather, Web, Dialog, Charge, ProjectStory, etc.) — these are the original Sanbot interaction activities.
- `core/mvvm/` — base MVVM scaffolding being introduced by the migration.
- `infra/{robot,storage,api,legacy}/` — adapter layer; `infra/api/` includes the backend HTTP client.
- `features/visitor/`, `app/visitor/`, `visitorflow/` — the visitor-notification feature implemented on top of the new architecture. `visitorflow/` exposes ports (`*Gateway`, `RobotSpeechPort`) and state machines (`Visitor*State`) talking to the backend via `BackendApiClient`.
- `AIML/ab/` — vendored Ab AIML conversational engine.
- `Models/`, `Controllers/`, `Services/`, `Utils/`, `video/` — legacy supporting code.

When touching the visitor flow, prefer the `visitorflow/` ports + the `features/visitor/` slice over editing legacy `My*Activity` files. Voice/face interaction code still lives in the legacy tree.

## Conventions

- Conventional Commits only. Do not add AI / Co-Authored-By attributions to commits.
- Match the language the user writes in (Spanish or English).
- Backend domain layer must not import FastAPI, httpx, or any framework — only stdlib + other domain modules. Side-effect-free.
- Stub providers exist deliberately to keep `create_app()` working without credentials; do not remove them when adding new channels.
- The duplicate `infra/` vs `infrastructure/` and `api/routes` vs `presentation/http` trees are an in-progress migration — when modifying a module, check whether a newer twin exists before editing.
