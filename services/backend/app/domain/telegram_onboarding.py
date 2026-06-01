from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime, timezone
from typing import Callable, Protocol

from backend.app.domain.company_catalog import company_label, is_known_company, list_company_catalog
from backend.app.domain.contact import Contact
from backend.app.domain.phone_numbers import normalize_phone_number


@dataclass(frozen=True)
class TelegramOnboardingRequest:
    phone_number: str
    telegram_user_id: str
    telegram_chat_id: str
    telegram_username: str | None = None
    first_name: str | None = None


@dataclass(frozen=True)
class PendingTelegramRegistration:
    phone_number: str
    normalized_phone: str
    telegram_user_id: str
    telegram_chat_id: str
    telegram_username: str | None = None
    first_name: str | None = None
    company: str | None = None
    job_title: str | None = None
    flow_state: str = "awaiting_company"
    status: str = "pending"
    created_at: str = ""

    def __post_init__(self) -> None:
        created_at = self.created_at.strip() if isinstance(self.created_at, str) else ""
        if not created_at:
            created_at = datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")
        object.__setattr__(self, "created_at", created_at)

    @classmethod
    def from_dict(cls, payload: dict[str, object]) -> "PendingTelegramRegistration":
        return cls(
            phone_number=str(payload.get("phone_number") or ""),
            normalized_phone=str(payload.get("normalized_phone") or ""),
            telegram_user_id=str(payload.get("telegram_user_id") or ""),
            telegram_chat_id=str(payload.get("telegram_chat_id") or ""),
            telegram_username=payload.get("telegram_username") if isinstance(payload.get("telegram_username"), str) else None,
            first_name=payload.get("first_name") if isinstance(payload.get("first_name"), str) else None,
            company=payload.get("company") if isinstance(payload.get("company"), str) else None,
            job_title=payload.get("job_title") if isinstance(payload.get("job_title"), str) else None,
            flow_state=str(payload.get("flow_state") or "awaiting_company"),
            status=str(payload.get("status") or "pending"),
            created_at=str(payload.get("created_at") or ""),
        )

    def to_dict(self) -> dict[str, object]:
        return {
            "phone_number": self.phone_number,
            "normalized_phone": self.normalized_phone,
            "telegram_user_id": self.telegram_user_id,
            "telegram_chat_id": self.telegram_chat_id,
            "telegram_username": self.telegram_username,
            "first_name": self.first_name,
            "company": self.company,
            "job_title": self.job_title,
            "flow_state": self.flow_state,
            "status": self.status,
            "created_at": self.created_at,
        }

    @property
    def company_label(self) -> str | None:
        return company_label(self.company)


@dataclass(frozen=True)
class TelegramCompanySelectionRequest:
    telegram_user_id: str
    company: str


@dataclass(frozen=True)
class TelegramJobTitleRequest:
    telegram_user_id: str
    job_title: str


@dataclass(frozen=True)
class TelegramOnboardingResult:
    status: str
    detail: str
    contact: Contact | None = None
    normalized_phone: str | None = None
    pending_registration: PendingTelegramRegistration | None = None
    available_companies: list[dict[str, str]] | None = None

    def to_dict(self) -> dict[str, object]:
        payload: dict[str, object] = {
            "status": self.status,
            "detail": self.detail,
        }
        if self.normalized_phone is not None:
            payload["normalized_phone"] = self.normalized_phone
        if self.contact is not None:
            payload["contact"] = {
                "id": self.contact.id,
                "display_name": self.contact.display_name,
                "job_title": self.contact.job_title,
                "company": self.contact.company,
                "company_label": self.contact.company_label,
                "phone": self.contact.phone,
                "telegram_chat_id": self.contact.telegram_chat_id,
                "telegram_user_id": self.contact.telegram_user_id,
                "telegram_username": self.contact.telegram_username,
            }
        if self.pending_registration is not None:
            payload["pending_registration"] = self.pending_registration.to_dict()
        if self.available_companies is not None:
            payload["available_companies"] = self.available_companies
        return payload


class TelegramOnboardingRepository(Protocol):
    def find_contact_by_phone(self, phone_number: str) -> Contact | None:
        ...

    def bind_contact_telegram(
        self,
        *,
        contact_id: str,
        telegram_chat_id: str,
        telegram_user_id: str,
        telegram_username: str | None = None,
    ) -> Contact:
        ...


class PendingTelegramRegistrationRepository(Protocol):
    def upsert_pending_registration(self, registration: PendingTelegramRegistration) -> PendingTelegramRegistration:
        ...

    def get_pending_registration_by_telegram_user_id(self, telegram_user_id: str) -> PendingTelegramRegistration | None:
        ...


class TelegramOnboardingService:
    def __init__(
        self,
        repository: TelegramOnboardingRepository,
        pending_registration_repository: PendingTelegramRegistrationRepository,
        *,
        now_provider: Callable[[], datetime] | None = None,
    ) -> None:
        self._repository = repository
        self._pending_registration_repository = pending_registration_repository
        self._now_provider = now_provider or (lambda: datetime.now(timezone.utc).replace(microsecond=0))

    def onboard(self, request: TelegramOnboardingRequest) -> TelegramOnboardingResult:
        normalized_phone = normalize_phone_number(request.phone_number)
        if normalized_phone is None:
            raise ValueError("phone_number must include at least one digit")

        contact = self._repository.find_contact_by_phone(request.phone_number)
        if contact is None:
            pending_registration = self._pending_registration_repository.upsert_pending_registration(
                PendingTelegramRegistration(
                    phone_number=request.phone_number,
                    normalized_phone=normalized_phone,
                    telegram_user_id=request.telegram_user_id,
                    telegram_chat_id=request.telegram_chat_id,
                    telegram_username=request.telegram_username,
                    first_name=request.first_name,
                    company=None,
                    job_title=None,
                    flow_state="awaiting_company",
                    status="pending",
                    created_at=self._now_provider().isoformat().replace("+00:00", "Z"),
                )
            )
            return TelegramOnboardingResult(
                status="awaiting_company_selection",
                detail="Recibimos tu teléfono. Ahora seleccioná la empresa para continuar con el preregistro.",
                normalized_phone=normalized_phone,
                pending_registration=pending_registration,
                available_companies=self._company_options(),
            )

        bound_contact = self._repository.bind_contact_telegram(
            contact_id=contact.id,
            telegram_chat_id=request.telegram_chat_id,
            telegram_user_id=request.telegram_user_id,
            telegram_username=request.telegram_username,
        )
        return TelegramOnboardingResult(
            status="matched",
            detail=f"Contacto {bound_contact.display_name} vinculado correctamente a Telegram.",
            contact=bound_contact,
            normalized_phone=normalized_phone,
        )

    def select_company(self, request: TelegramCompanySelectionRequest) -> TelegramOnboardingResult:
        pending_registration = self._require_pending_registration(request.telegram_user_id)
        company = request.company.strip()
        if not is_known_company(company):
            raise ValueError("company must be one of the configured catalog entries")

        updated = self._pending_registration_repository.upsert_pending_registration(
            PendingTelegramRegistration(
                phone_number=pending_registration.phone_number,
                normalized_phone=pending_registration.normalized_phone,
                telegram_user_id=pending_registration.telegram_user_id,
                telegram_chat_id=pending_registration.telegram_chat_id,
                telegram_username=pending_registration.telegram_username,
                first_name=pending_registration.first_name,
                company=company,
                job_title=pending_registration.job_title,
                flow_state="awaiting_job_title",
                status=pending_registration.status,
                created_at=pending_registration.created_at,
            )
        )
        return TelegramOnboardingResult(
            status="awaiting_job_title",
            detail="Perfecto. Ahora escribí tu cargo para completar el preregistro.",
            normalized_phone=updated.normalized_phone,
            pending_registration=updated,
        )

    def submit_job_title(self, request: TelegramJobTitleRequest) -> TelegramOnboardingResult:
        pending_registration = self._require_pending_registration(request.telegram_user_id)
        job_title = request.job_title.strip()
        if not job_title:
            raise ValueError("job_title must be a non-empty string")
        if pending_registration.company is None:
            raise ValueError("Pending Telegram registration requires a selected company before job title")

        updated = self._pending_registration_repository.upsert_pending_registration(
            PendingTelegramRegistration(
                phone_number=pending_registration.phone_number,
                normalized_phone=pending_registration.normalized_phone,
                telegram_user_id=pending_registration.telegram_user_id,
                telegram_chat_id=pending_registration.telegram_chat_id,
                telegram_username=pending_registration.telegram_username,
                first_name=pending_registration.first_name,
                company=pending_registration.company,
                job_title=job_title,
                flow_state="ready_for_approval",
                status=pending_registration.status,
                created_at=pending_registration.created_at,
            )
        )
        return TelegramOnboardingResult(
            status="pending_registration_ready",
            detail=(
                "Gracias. Tu preregistro quedó completo y será revisado por un administrador. "
                "Te avisaremos cuando tu contacto quede habilitado."
            ),
            normalized_phone=updated.normalized_phone,
            pending_registration=updated,
        )

    @staticmethod
    def _company_options() -> list[dict[str, str]]:
        return [{"id": item.id, "label": item.label} for item in list_company_catalog()]

    def _require_pending_registration(self, telegram_user_id: str) -> PendingTelegramRegistration:
        pending_registration = self._pending_registration_repository.get_pending_registration_by_telegram_user_id(
            telegram_user_id
        )
        if pending_registration is None:
            raise ValueError("Pending Telegram registration not found")
        return pending_registration
