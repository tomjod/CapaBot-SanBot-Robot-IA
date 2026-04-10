from __future__ import annotations

from dataclasses import dataclass
from typing import Any


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
    telegram_chat_id: str | None = None
    email: str | None = None
    email_enabled: bool = False

    def __post_init__(self) -> None:
        object.__setattr__(self, "id", _require_non_empty(self.id, "id"))
        object.__setattr__(self, "display_name", _require_non_empty(self.display_name, "display_name"))
        object.__setattr__(self, "telegram_chat_id", _optional_string(self.telegram_chat_id, "telegram_chat_id"))
        object.__setattr__(self, "email", _optional_string(self.email, "email"))

        if not isinstance(self.email_enabled, bool):
            raise ValueError("email_enabled must be a boolean")

    @classmethod
    def from_dict(cls, payload: dict[str, Any]) -> "Contact":
        if not isinstance(payload, dict):
            raise ValueError("contact payload must be an object")

        return cls(
            id=payload.get("id"),
            display_name=payload.get("display_name"),
            telegram_chat_id=payload.get("telegram_chat_id"),
            email=payload.get("email"),
            email_enabled=payload.get("email_enabled", False),
        )

    @property
    def telegram_available(self) -> bool:
        return self.telegram_chat_id is not None

    @property
    def email_available(self) -> bool:
        return self.email is not None and self.email_enabled

    @property
    def available(self) -> bool:
        return self.telegram_available or self.email_available

    def to_summary(self) -> dict[str, Any]:
        return {
            "id": self.id,
            "display_name": self.display_name,
            "channels": {
                "telegram": self.telegram_available,
                "email": self.email_available,
            },
            "available": self.available,
        }
