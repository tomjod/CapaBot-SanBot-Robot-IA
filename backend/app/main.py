from __future__ import annotations

from backend.app.api.routes.admin_contacts import register_admin_contacts_routes
from backend.app.api.routes.contacts import register_contacts_routes
from backend.app.api.routes.notifications import register_notification_routes
from backend.app.api.routes.telegram_onboarding import register_telegram_onboarding_routes
from backend.app.runtime import build_runtime


def create_app():
    try:
        from fastapi import FastAPI
    except ModuleNotFoundError as exc:
        raise RuntimeError(
            "FastAPI is not installed. Install backend dependencies before running the backend app."
        ) from exc

    app = FastAPI(title="Visitor Notify Assistant MVP")
    runtime = build_runtime()

    register_contacts_routes(app, runtime.repository)
    register_notification_routes(app, runtime.notification_service)
    register_admin_contacts_routes(app, runtime.repository, runtime.pending_registration_repository)
    register_telegram_onboarding_routes(app, runtime.telegram_onboarding_service)
    return app


app = None
try:
    app = create_app()
except RuntimeError:
    app = None
