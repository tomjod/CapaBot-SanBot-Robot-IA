from __future__ import annotations

from typing import Any, Protocol

from pydantic import BaseModel, field_validator

from backend.app.domain.telegram_onboarding import TelegramOnboardingRequest


class TelegramOnboardingSubmissionService(Protocol):
    def onboard(self, request: TelegramOnboardingRequest) -> Any:
        ...


class TelegramOnboardingPayload(BaseModel):
    phone_number: str
    telegram_user_id: str
    telegram_chat_id: str
    telegram_username: str | None = None
    first_name: str | None = None

    @field_validator("phone_number", "telegram_user_id", "telegram_chat_id", "telegram_username", "first_name", mode="before")
    @classmethod
    def strip_strings(cls, value: Any) -> Any:
        if isinstance(value, str):
            return value.strip() or None
        return value


def submit_telegram_onboarding_response(
    service: TelegramOnboardingSubmissionService,
    payload: dict[str, Any],
) -> tuple[int, dict[str, Any]]:
    try:
        request_payload = TelegramOnboardingPayload.model_validate(payload)
        result = service.onboard(
            TelegramOnboardingRequest(
                phone_number=request_payload.phone_number,
                telegram_user_id=request_payload.telegram_user_id,
                telegram_chat_id=request_payload.telegram_chat_id,
                telegram_username=request_payload.telegram_username,
                first_name=request_payload.first_name,
            )
        )
        return 200, result.to_dict()
    except ValueError as exc:
        return 400, {
            "status": "failed",
            "detail": str(exc),
        }
    except Exception:
        return 500, {
            "status": "failed",
            "detail": "Ocurrió un error inesperado al procesar la vinculación de Telegram.",
        }


def register_telegram_onboarding_routes(app: Any, service: TelegramOnboardingSubmissionService) -> None:
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
