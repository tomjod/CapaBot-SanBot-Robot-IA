# Visitor Notify Assistant MVP Verification Notes

## Backend-only slice
- Export `VISITOR_NOTIFY_CONTACTS_PATH=backend/tests/fixtures/contacts.json`.
- Set `VISITOR_NOTIFY_TELEGRAM_BOT_TOKEN` for real Telegram delivery.
- Leave email unset for Telegram-first validation, or add `VISITOR_NOTIFY_EMAIL_SMTP_HOST`, `VISITOR_NOTIFY_EMAIL_FROM`, and `VISITOR_NOTIFY_EMAIL_SMTP_PORT` to exercise optional parallel email.
- Keep `VISITOR_NOTIFY_ALLOW_STUB_DELIVERY=false` in operational environments so missing credentials never produce false success.

## Android happy path slice
- Configure Gradle properties `VISITOR_API_BASE_URL`, `VISITOR_DEVICE_ID`, and `VISITOR_LOCATION_LABEL` for the robot deployment.
- Launch the guided visitor flow from `MyBaseActivity`; confirm `GET /contacts` returns at least one Telegram-enabled contact.
- Select a Telegram-enabled contact and verify the backend responds with `accepted` or `delivered_or_queued` plus a visitor-facing `detail` message.

## Android error path slice
- Point `VISITOR_API_BASE_URL` to an unreachable backend or use a contact with no Telegram/email channels.
- Verify the app shows explicit retry/back guidance and never reports success when the backend returns `unavailable` or `failed`.
