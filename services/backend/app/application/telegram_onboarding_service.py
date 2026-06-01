from __future__ import annotations

from typing import Any, Protocol

from backend.app.domain.telegram_onboarding import (
    TelegramCompanySelectionRequest,
    TelegramJobTitleRequest,
    TelegramOnboardingRequest,
)


class TelegramOnboardingApplicationService(Protocol):
    def onboard(self, request: TelegramOnboardingRequest) -> Any:
        ...

    def select_company(self, request: TelegramCompanySelectionRequest) -> Any:
        ...

    def submit_job_title(self, request: TelegramJobTitleRequest) -> Any:
        ...


def _required_string(payload: dict[str, Any], key: str) -> str:
    value = payload.get(key)
    if not isinstance(value, str) or not value.strip():
        raise ValueError(f"{key} is required")
    return value.strip()


def _optional_string(payload: dict[str, Any], key: str) -> str | None:
    value = payload.get(key)
    if not isinstance(value, str):
        return None
    normalized = value.strip()
    return normalized or None


def _run_contract(callback) -> tuple[int, dict[str, Any]]:
    try:
        result = callback()
        return 200, result.to_dict()
    except ValueError as exc:
        return 400, {"status": "failed", "detail": str(exc)}
    except Exception:
        return 500, {
            "status": "failed",
            "detail": "Ocurrió un error inesperado al procesar la vinculación de Telegram.",
        }


def submit_telegram_onboarding_response(
    service: TelegramOnboardingApplicationService,
    payload: dict[str, Any],
) -> tuple[int, dict[str, Any]]:
    return _run_contract(
        lambda: service.onboard(
            TelegramOnboardingRequest(
                phone_number=_required_string(payload, "phone_number"),
                telegram_user_id=_required_string(payload, "telegram_user_id"),
                telegram_chat_id=_required_string(payload, "telegram_chat_id"),
                telegram_username=_optional_string(payload, "telegram_username"),
                first_name=_optional_string(payload, "first_name"),
            )
        )
    )


def select_company_response(
    service: TelegramOnboardingApplicationService,
    payload: dict[str, Any],
) -> tuple[int, dict[str, Any]]:
    return _run_contract(
        lambda: service.select_company(
            TelegramCompanySelectionRequest(
                telegram_user_id=_required_string(payload, "telegram_user_id"),
                company=_required_string(payload, "company"),
            )
        )
    )


def submit_job_title_response(
    service: TelegramOnboardingApplicationService,
    payload: dict[str, Any],
) -> tuple[int, dict[str, Any]]:
    return _run_contract(
        lambda: service.submit_job_title(
            TelegramJobTitleRequest(
                telegram_user_id=_required_string(payload, "telegram_user_id"),
                job_title=_required_string(payload, "job_title"),
            )
        )
    )
