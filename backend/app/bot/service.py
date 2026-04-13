from __future__ import annotations

from dataclasses import dataclass
from typing import Any, Protocol

from telegram import InlineKeyboardButton, InlineKeyboardMarkup, KeyboardButton, ReplyKeyboardMarkup

from backend.app.domain.contact import Contact

HELP_BUTTON_TEXT = "Necesito ayuda"
SHARE_CONTACT_BUTTON_TEXT = "Compartir mi teléfono"
RESHARE_CONTACT_BUTTON_TEXT = "Actualizar mi teléfono"


@dataclass(frozen=True)
class TelegramActor:
    user_id: str
    chat_id: str
    username: str | None = None
    first_name: str | None = None


@dataclass(frozen=True)
class TelegramBotReply:
    text: str
    reply_markup: ReplyKeyboardMarkup | InlineKeyboardMarkup


class TelegramContactLookup(Protocol):
    def find_contact_by_telegram_user_id(self, telegram_user_id: str) -> Contact | None:
        ...


class TelegramOnboardingContractClient(Protocol):
    def submit(self, payload: dict[str, Any]) -> tuple[int, dict[str, Any]]:
        ...

    def select_company(self, payload: dict[str, Any]) -> tuple[int, dict[str, Any]]:
        ...

    def submit_job_title(self, payload: dict[str, Any]) -> tuple[int, dict[str, Any]]:
        ...


def _menu_markup(share_button_text: str) -> ReplyKeyboardMarkup:
    return ReplyKeyboardMarkup(
        keyboard=[
            [KeyboardButton(text=share_button_text, request_contact=True)],
            [KeyboardButton(text=HELP_BUTTON_TEXT)],
        ],
        resize_keyboard=True,
    )


def _company_markup(companies: list[dict[str, Any]]) -> InlineKeyboardMarkup:
    rows = [
        [
            InlineKeyboardButton(
                text=str(company.get("label") or company.get("id") or "Empresa"),
                callback_data=f"prereg_company:{company['id']}",
            )
        ]
        for company in companies
        if isinstance(company.get("id"), str) and company.get("id", "").strip()
    ]
    return InlineKeyboardMarkup(rows)


def _person_label(contact: Contact) -> str:
    parts = [contact.display_name]
    if contact.job_title:
        parts.append(contact.job_title)
    if contact.company_label:
        parts.append(contact.company_label)
    return " · ".join(parts)


class TelegramBotFlowService:
    def __init__(
        self,
        *,
        contact_lookup: TelegramContactLookup,
        onboarding_client: TelegramOnboardingContractClient,
        registration_help_text: str,
    ) -> None:
        self._contact_lookup = contact_lookup
        self._onboarding_client = onboarding_client
        self._registration_help_text = registration_help_text.strip()

    def build_start_reply(self, actor: TelegramActor) -> TelegramBotReply:
        linked_contact = self._contact_lookup.find_contact_by_telegram_user_id(actor.user_id)
        greeting_name = actor.first_name or "hola"
        if linked_contact is not None:
            return TelegramBotReply(
                text=(
                    f"Hola, {greeting_name}. Tu cuenta ya está vinculada a {_person_label(linked_contact)}.\n\n"
                    "Si necesitás actualizar el número asociado, compartí tu contacto otra vez desde el botón de abajo."
                ),
                reply_markup=_menu_markup(RESHARE_CONTACT_BUTTON_TEXT),
            )

        return TelegramBotReply(
            text=(
                f"Hola, {greeting_name}. Soy el asistente de recepciones.\n\n"
                "Puedo vincular tu cuenta de Telegram para que recibas notificaciones de recepción en este chat. "
                "Para continuar, compartí tu teléfono desde el botón de abajo. Telegram sólo permite usar el número si vos lo autorizás explícitamente."
            ),
            reply_markup=_menu_markup(SHARE_CONTACT_BUTTON_TEXT),
        )

    def build_help_reply(self, actor: TelegramActor) -> TelegramBotReply:
        return TelegramBotReply(
            text=(
                "Si todavía no figurás en la agenda o tus datos cambiaron, pedile al administrador que cargue o actualice tu teléfono "
                f"y después repetí la vinculación desde este chat.\n\n{self._registration_help_text}"
            ),
            reply_markup=_menu_markup(SHARE_CONTACT_BUTTON_TEXT),
        )

    def build_contact_reply(
        self,
        *,
        actor: TelegramActor,
        phone_number: str | None,
        shared_contact_user_id: str | None,
    ) -> TelegramBotReply:
        if shared_contact_user_id is not None and shared_contact_user_id != actor.user_id:
            return TelegramBotReply(
                text=(
                    "Necesito que compartas TU propio contacto desde el botón del menú. "
                    "Telegram no me deja vincular números enviados manualmente o de otra persona."
                ),
                reply_markup=_menu_markup(SHARE_CONTACT_BUTTON_TEXT),
            )

        status_code, response = self._onboarding_client.submit(
            {
                "phone_number": phone_number,
                "telegram_user_id": actor.user_id,
                "telegram_chat_id": actor.chat_id,
                "telegram_username": actor.username,
                "first_name": actor.first_name,
            }
        )
        if status_code != 200:
            return TelegramBotReply(
                text=(
                    "No pude completar la vinculación en este momento. Probá de nuevo en unos minutos. "
                    "Si el problema sigue, contactá al administrador de recepciones."
                ),
                reply_markup=_menu_markup(SHARE_CONTACT_BUTTON_TEXT),
            )

        if response.get("status") == "matched":
            contact_payload = response.get("contact") or {}
            profile = contact_payload.get("display_name") or "tu perfil"
            job_title = contact_payload.get("job_title")
            company_label = contact_payload.get("company_label")
            profile_parts = [str(profile)]
            if isinstance(job_title, str) and job_title.strip():
                profile_parts.append(job_title.strip())
            if isinstance(company_label, str) and company_label.strip():
                profile_parts.append(company_label.strip())
            profile_text = " · ".join(profile_parts)
            return TelegramBotReply(
                text=(
                    "✅ Vinculación completada.\n\n"
                    f"Tu cuenta quedó asociada a {profile_text}. "
                    "A partir de ahora vas a recibir por este chat las notificaciones de recepción que te correspondan."
                ),
                reply_markup=_menu_markup(RESHARE_CONTACT_BUTTON_TEXT),
            )

        if response.get("status") == "awaiting_company_selection":
            return TelegramBotReply(
                text=(
                    "📝 Recibimos tu teléfono.\n\n"
                    "Para completar el preregistro, seleccioná la empresa a la que pertenecés."
                ),
                reply_markup=_company_markup(response.get("available_companies") or []),
            )

        pending_statuses = {"pending_registration", "pending_registration_ready", "unregistered"}
        if response.get("status") in pending_statuses:
            return TelegramBotReply(
                text=(
                    "📝 Recibimos tu solicitud de registro.\n\n"
                    "Todavía no encontramos un contacto aprobado con ese teléfono. "
                    "Un administrador debe crearte o aprobar tu contacto antes de activar las notificaciones en este chat.\n\n"
                    f"{self._registration_help_text}"
                ),
                reply_markup=_menu_markup(SHARE_CONTACT_BUTTON_TEXT),
            )

        return TelegramBotReply(
            text=(
                "No pude interpretar la respuesta del alta de Telegram. Probá de nuevo en unos minutos o contactá al administrador de recepciones."
            ),
            reply_markup=_menu_markup(SHARE_CONTACT_BUTTON_TEXT),
        )

    def build_company_selection_reply(self, *, actor: TelegramActor, company: str) -> TelegramBotReply:
        status_code, _ = self._onboarding_client.select_company(
            {
                "telegram_user_id": actor.user_id,
                "company": company,
            }
        )
        if status_code != 200:
            return TelegramBotReply(
                text=(
                    "No pude registrar la empresa en este momento. Probá nuevamente desde los botones o contactá al administrador."
                ),
                reply_markup=_menu_markup(SHARE_CONTACT_BUTTON_TEXT),
            )

        return TelegramBotReply(
            text=(
                "Perfecto. Ahora escribí tu cargo tal como querés que quede registrado.\n\n"
                "Ejemplo: Recepcionista, Ejecutiva comercial, Jefe de operaciones."
            ),
            reply_markup=_menu_markup(SHARE_CONTACT_BUTTON_TEXT),
        )

    def build_job_title_reply(self, *, actor: TelegramActor, message_text: str) -> TelegramBotReply | None:
        status_code, response = self._onboarding_client.submit_job_title(
            {
                "telegram_user_id": actor.user_id,
                "job_title": message_text,
            }
        )
        if status_code == 400 and response.get("detail") == "Pending Telegram registration not found":
            return None
        if status_code != 200:
            return TelegramBotReply(
                text=(
                    "No pude guardar tu cargo en este momento. Escribilo nuevamente en un solo mensaje o contactá al administrador."
                ),
                reply_markup=_menu_markup(SHARE_CONTACT_BUTTON_TEXT),
            )

        return TelegramBotReply(
            text=(
                "✅ Preregistro completado.\n\n"
                "Recibimos tu empresa y tu cargo. Un administrador va a revisar la solicitud y, una vez aprobada, tu contacto quedará habilitado automáticamente."
            ),
            reply_markup=_menu_markup(SHARE_CONTACT_BUTTON_TEXT),
        )
