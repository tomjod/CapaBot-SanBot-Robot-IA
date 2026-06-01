from __future__ import annotations

import os
from dataclasses import dataclass
from pathlib import Path
from typing import Mapping


def _parse_bool(value: str | None, default: bool) -> bool:
    if value is None:
        return default
    return value.strip().lower() in {"1", "true", "yes", "on"}


def _resolve_backend_root(backend_root: Path | None = None) -> Path:
    return backend_root or Path(__file__).resolve().parents[2]


def _resolve_runtime_data_path(
    values: Mapping[str, str],
    *,
    env_var: str,
    file_name: str,
    backend_root: Path,
) -> Path:
    override = values.get(env_var)
    if override:
        return Path(override)

    runtime_default = backend_root / "runtime_data" / file_name
    legacy_default = backend_root / "app" / "data" / file_name
    if runtime_default.exists():
        return runtime_default
    if legacy_default.exists():
        return legacy_default
    return runtime_default


@dataclass(frozen=True)
class BackendSettings:
    contacts_path: Path
    pending_registrations_path: Path
    telegram_bot_token: str | None
    telegram_api_base_url: str
    telegram_timeout_seconds: float
    telegram_registration_help_text: str
    email_mailtrap_token: str | None
    email_smtp_host: str | None
    email_from: str | None
    email_from_name: str | None
    email_smtp_port: int
    email_timeout_seconds: float
    allow_stub_delivery: bool

    @classmethod
    def from_env(
        cls,
        env: Mapping[str, str] | None = None,
        *,
        backend_root: Path | None = None,
    ) -> "BackendSettings":
        values = env or os.environ
        resolved_backend_root = _resolve_backend_root(backend_root)
        return cls(
            contacts_path=_resolve_runtime_data_path(
                values,
                env_var="VISITOR_NOTIFY_CONTACTS_PATH",
                file_name="contacts.json",
                backend_root=resolved_backend_root,
            ),
            pending_registrations_path=_resolve_runtime_data_path(
                values,
                env_var="VISITOR_NOTIFY_PENDING_REGISTRATIONS_PATH",
                file_name="pending_registrations.json",
                backend_root=resolved_backend_root,
            ),
            telegram_bot_token=values.get("VISITOR_NOTIFY_TELEGRAM_BOT_TOKEN") or None,
            telegram_api_base_url=values.get("VISITOR_NOTIFY_TELEGRAM_API_BASE_URL", "https://api.telegram.org"),
            telegram_timeout_seconds=float(values.get("VISITOR_NOTIFY_TELEGRAM_TIMEOUT_SECONDS", "10")),
            telegram_registration_help_text=values.get(
                "VISITOR_NOTIFY_TELEGRAM_REGISTRATION_HELP_TEXT",
                "Si necesitás el alta, escribile al administrador de recepciones de tu empresa.",
            ),
            email_mailtrap_token=values.get("VISITOR_NOTIFY_EMAIL_MAILTRAP_TOKEN") or None,
            email_smtp_host=values.get("VISITOR_NOTIFY_EMAIL_SMTP_HOST") or None,
            email_from=values.get("VISITOR_NOTIFY_EMAIL_FROM") or None,
            email_from_name=values.get("VISITOR_NOTIFY_EMAIL_FROM_NAME") or None,
            email_smtp_port=int(values.get("VISITOR_NOTIFY_EMAIL_SMTP_PORT", "25")),
            email_timeout_seconds=float(values.get("VISITOR_NOTIFY_EMAIL_TIMEOUT_SECONDS", "10")),
            allow_stub_delivery=_parse_bool(values.get("VISITOR_NOTIFY_ALLOW_STUB_DELIVERY"), False),
        )
