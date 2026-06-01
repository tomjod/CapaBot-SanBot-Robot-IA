# CapaBot-SanBot-Robot-IA

AI-enhanced visitor interaction and notification system for the Sanbot robot.

The robot greets visitors, registers them through a conversational flow, and dispatches notifications to internal staff across multiple channels (Telegram, Email, WhatsApp). It combines on-device speech/face interaction with a backend that orchestrates messaging and a WhatsApp microservice for delivery.

## Repository Layout

## Repository Layout

| Path | Description |
|------|-------------|
| `apps/android/` | Android app (Java) for the Sanbot robot. Gradle project root. |
| `services/backend/` | Python FastAPI notification service (Telegram, Email, WhatsApp). |
| `services/whatsapp-bridge/` | Node/TypeScript microservice wrapping Baileys for WhatsApp delivery. |

See [CLAUDE.md](CLAUDE.md) for build commands and architecture details.

## Pipeline Implemented
![Alt text](readme-images/Pipeline.jpg?raw=true "Pipeline")

See [CLAUDE.md](CLAUDE.md) for build commands, architecture details, and conventions.

## Architecture

```
┌─────────────────┐         ┌──────────────────┐         ┌──────────────────┐
│  Sanbot Robot   │  HTTP   │  Backend (Py)    │  HTTP   │ whatsapp-bridge  │
│  apps/android/  │ ──────▶ │ services/backend │ ──────▶ │   (Node/TS)      │
└─────────────────┘         └──────────────────┘         └──────────────────┘
                                    │                            │
                                    ▼                            ▼
                            Telegram + SMTP             WhatsApp (Baileys)
```

- Android app calls backend via `VISITOR_API_BASE_URL` (configured in `apps/android/local.properties`).
- Backend calls whatsapp-bridge using `INTERNAL_API_KEY` header.

## Build & Run

### Android (Sanbot robot)

Requires Android Studio + Android SDK. Sanbot OpenSDK is vendored under `apps/android/libs/`.

```bash
cd apps/android
./gradlew assembleDebug          # build APK
./gradlew installDebug           # install on robot (adb)
```

Robot must be in Developer Mode: Settings → About → tap "Robot Version" 7×. Connect via USB.

Create `apps/android/local.properties` with the required keys before building (gitignored):

```properties
OPENAI_API_KEY=sk-...
ORGANIZATION_ID=...
PROJECT_ID=...
VISITOR_API_BASE_URL=http://your-backend:8000
VISITOR_DEVICE_ID=sanbot-01
VISITOR_LOCATION_LABEL=Reception
VISITOR_FLOW_ENABLED=true
```

### Backend (Python / FastAPI)

```bash
pip install -r services/backend/requirements.txt
uvicorn backend.app.main:app --reload    # run from repo root or with PYTHONPATH=services
pytest services/backend/tests            # tests
```

Environment variables follow the `VISITOR_NOTIFY_*` prefix. See `services/backend/app/bootstrap/settings.py`.

### whatsapp-bridge (Node / TypeScript)

```bash
cd services/whatsapp-bridge
npm install
npm run dev      # tsx watch
npm run build    # tsc -> dist/
npm start        # node dist/main.js
```

Required env: `INTERNAL_API_KEY`, `AUTH_DIR`. Optional: `PORT` (default 3001), `RECONNECT_INTERVAL_MS`. First run shows a QR on stdout to pair the WhatsApp account; the session is persisted under `AUTH_DIR`.

## Stack

- **Android**: Java, Gradle, Sanbot OpenSDK 2.0.1.10, AIML (Ab engine), OpenAI SDK.
- **Backend**: Python 3.11+, FastAPI, uvicorn, Jinja2, python-telegram-bot, Mailtrap / SMTP.
- **whatsapp-bridge**: Node 18+, TypeScript, Express, Baileys.

## License

MIT — see [LICENSE](LICENSE).

## Credits

Originally based on the [CapaBot](https://amslaurea.unibo.it/19120/) project by Igor Lirussi (ISR / University of Lisbon). This fork extends the platform with a multi-channel notification backend, a WhatsApp bridge, and AI-driven visitor interaction.
