from __future__ import annotations

from dataclasses import dataclass, replace
from typing import Any

from backend.app.domain.company_catalog import company_label, is_known_company
from backend.app.domain.phone_numbers import normalize_phone_number


def _require_non_empty(value: Any, field_name: str) -> str:
    if not isinstance(value, str):
        raise ValueError(f"{field_name} must be a non-empty string")

    normalized = value.strip()
    if not normalized:
        raise ValueError(f"{field_name} must be a non-empty string")

    return normalized


def _optional_string(value: Any, field_name: str) -> str | None:
    if value is None:
        return None
    if not isinstance(value, str):
        raise ValueError(f"{field_name} must be a string when provided")

    normalized = value.strip()
    return normalized or None


@dataclass(frozen=True)
class Contact:
    id: str
    display_name: str
    job_title: str | None = None
    company: str | None = None
    enabled: bool = True
    phone: str | None = None
    telegram_chat_id: str | None = None
    telegram_user_id: str | None = None
    telegram_username: str | None = None
    email: str | None = None
    email_enabled: bool = False

    def __post_init__(self) -> None:
        object.__setattr__(self, "id", _require_non_empty(self.id, "id"))
        object.__setattr__(self, "display_name", _require_non_empty(self.display_name, "display_name"))
        object.__setattr__(self, "job_title", _optional_string(self.job_title, "job_title"))
        object.__setattr__(self, "company", _optional_string(self.company, "company"))
        object.__setattr__(self, "phone", _optional_string(self.phone, "phone"))
        object.__setattr__(self, "telegram_chat_id", _optional_string(self.telegram_chat_id, "telegram_chat_id"))
        object.__setattr__(self, "telegram_user_id", _optional_string(self.telegram_user_id, "telegram_user_id"))
        object.__setattr__(self, "telegram_username", _optional_string(self.telegram_username, "telegram_username"))
        object.__setattr__(self, "email", _optional_string(self.email, "email"))

        if self.company is not None and not is_known_company(self.company):
            raise ValueError("company must be one of the configured catalog entries")

        if not isinstance(self.enabled, bool):
            raise ValueError("enabled must be a boolean")

        if not isinstance(self.email_enabled, bool):
            raise ValueError("email_enabled must be a boolean")

    @classmethod
    def from_dict(cls, payload: dict[str, Any]) -> "Contact":
        if not isinstance(payload, dict):
            raise ValueError("contact payload must be an object")

        return cls(
            id=payload.get("id"),
            display_name=payload.get("display_name"),
            job_title=payload.get("job_title") or payload.get("cargo"),
            company=payload.get("company") or payload.get("empresa"),
            enabled=payload.get("enabled", payload.get("available_for_visits", True)),
            phone=payload.get("phone") or payload.get("telefono"),
            telegram_chat_id=payload.get("telegram_chat_id"),
            telegram_user_id=payload.get("telegram_user_id"),
            telegram_username=payload.get("telegram_username"),
            email=payload.get("email"),
            email_enabled=payload.get("email_enabled", False),
        )

    @property
    def normalized_phone(self) -> str | None:
        return normalize_phone_number(self.phone)

    @property
    def telegram_available(self) -> bool:
        return self.telegram_chat_id is not None

    @property
    def email_available(self) -> bool:
        return self.email is not None and self.email_enabled

    @property
    def available(self) -> bool:
        return self.enabled and (self.telegram_available or self.email_available)

    @property
    def company_label(self) -> str | None:
        return company_label(self.company)

    def with_telegram_binding(
        self,
        *,
        telegram_chat_id: str,
        telegram_user_id: str,
        telegram_username: str | None = None,
    ) -> "Contact":
        return replace(
            self,
            telegram_chat_id=telegram_chat_id,
            telegram_user_id=telegram_user_id,
            telegram_username=telegram_username,
        )

    def to_summary(self) -> dict[str, Any]:
        return {
            "id": self.id,
            "display_name": self.display_name,
            "job_title": self.job_title,
            "company": self.company,
            "company_label": self.company_label,
            "enabled": self.enabled,
            "phone": self.phone,
            "channels": {
                "telegram": self.telegram_available,
                "email": self.email_available,
            },
            "available": self.available,
        }
