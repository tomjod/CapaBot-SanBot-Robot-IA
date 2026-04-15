from __future__ import annotations

from typing import Any

from backend.app.application.telegram_onboarding_service import (
    TelegramOnboardingApplicationService,
    submit_telegram_onboarding_response,
)


def register_telegram_onboarding_routes(app: Any, service: TelegramOnboardingApplicationService) -> None:
    from fastapi import APIRouter
    from fastapi.responses import JSONResponse

    router = APIRouter()

    @router.post("/telegram/onboarding")
    def post_telegram_onboarding(payload: dict[str, Any]) -> Any:
        status_code, response = submit_telegram_onboarding_response(service, payload)
        if status_code == 200:
            return response
        return JSONResponse(status_code=status_code, content=response)

    app.include_router(router)
