from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime, timezone
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


def _parse_requested_at(value: Any) -> datetime:
    if not isinstance(value, str):
        raise ValueError("requested_at must be an ISO-8601 string")

    normalized = value.strip()
    if normalized.endswith("Z"):
        normalized = normalized[:-1] + "+00:00"

    try:
        parsed = datetime.fromisoformat(normalized)
    except ValueError as exc:
        raise ValueError("requested_at must be an ISO-8601 string") from exc

    if parsed.tzinfo is None:
        parsed = parsed.replace(tzinfo=timezone.utc)

    return parsed.astimezone(timezone.utc)


@dataclass(frozen=True)
class NotificationRequest:
    contact_id: str
    device_id: str
    requested_at: datetime
    visitor_name: str | None = None
    location: str | None = None
    reason: str | None = None
    message: str | None = None

    def __post_init__(self) -> None:
        object.__setattr__(self, "contact_id", _require_non_empty(self.contact_id, "contact_id"))
        object.__setattr__(self, "device_id", _require_non_empty(self.device_id, "device_id"))
        object.__setattr__(self, "visitor_name", _optional_string(self.visitor_name, "visitor_name"))
        object.__setattr__(self, "location", _optional_string(self.location, "location"))
        object.__setattr__(self, "reason", _optional_string(self.reason, "reason"))
        object.__setattr__(self, "message", _optional_string(self.message, "message"))

        if not isinstance(self.requested_at, datetime):
            raise ValueError("requested_at must be a datetime")

    @classmethod
    def from_dict(cls, payload: dict[str, Any]) -> "NotificationRequest":
        if not isinstance(payload, dict):
            raise ValueError("notification payload must be an object")

        return cls(
            contact_id=payload.get("contact_id"),
            device_id=payload.get("device_id"),
            requested_at=_parse_requested_at(payload.get("requested_at")),
            visitor_name=payload.get("visitor_name"),
            location=payload.get("location"),
            reason=payload.get("reason"),
            message=payload.get("message"),
        )
