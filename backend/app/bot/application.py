from __future__ import annotations

import re
from typing import Any

from telegram.ext import (
    Application,
    ApplicationBuilder,
    CallbackQueryHandler,
    CommandHandler,
    ContextTypes,
    MessageHandler,
    filters,
)

from backend.app.api.routes.telegram_onboarding import submit_telegram_onboarding_response
from backend.app.bot.service import HELP_BUTTON_TEXT, TelegramActor, TelegramBotFlowService
from backend.app.domain.telegram_onboarding import TelegramCompanySelectionRequest, TelegramJobTitleRequest
from backend.app.runtime import BackendRuntime

BOT_SERVICE_KEY = "telegram_bot_flow_service"


class LocalTelegramOnboardingClient:
    def __init__(self, service: Any) -> None:
        self._service = service

    def submit(self, payload: dict[str, Any]) -> tuple[int, dict[str, Any]]:
        return submit_telegram_onboarding_response(self._service, payload)

    def select_company(self, payload: dict[str, Any]) -> tuple[int, dict[str, Any]]:
        try:
            result = self._service.select_company(
                TelegramCompanySelectionRequest(
                    telegram_user_id=str(payload.get("telegram_user_id") or "").strip(),
                    company=str(payload.get("company") or "").strip(),
                )
            )
            return 200, result.to_dict()
        except ValueError as exc:
            return 400, {"status": "failed", "detail": str(exc)}

    def submit_job_title(self, payload: dict[str, Any]) -> tuple[int, dict[str, Any]]:
        try:
            result = self._service.submit_job_title(
                TelegramJobTitleRequest(
                    telegram_user_id=str(payload.get("telegram_user_id") or "").strip(),
                    job_title=str(payload.get("job_title") or "").strip(),
                )
            )
            return 200, result.to_dict()
        except ValueError as exc:
            return 400, {"status": "failed", "detail": str(exc)}


def _actor_from_update(update: Any) -> TelegramActor:
    user = update.effective_user
    chat = update.effective_chat
    return TelegramActor(
        user_id=str(user.id),
        chat_id=str(chat.id),
        username=getattr(user, "username", None),
        first_name=getattr(user, "first_name", None),
    )


def _service_from_context(context: ContextTypes.DEFAULT_TYPE) -> TelegramBotFlowService:
    service = context.application.bot_data.get(BOT_SERVICE_KEY)
    if service is None:
        raise RuntimeError("telegram bot flow service is not configured")
    return service


async def start_command(update: Any, context: ContextTypes.DEFAULT_TYPE) -> None:
    service = _service_from_context(context)
    reply = service.build_start_reply(_actor_from_update(update))
    await update.effective_message.reply_text(reply.text, reply_markup=reply.reply_markup)


async def help_message(update: Any, context: ContextTypes.DEFAULT_TYPE) -> None:
    service = _service_from_context(context)
    reply = service.build_help_reply(_actor_from_update(update))
    await update.effective_message.reply_text(reply.text, reply_markup=reply.reply_markup)


async def contact_shared(update: Any, context: ContextTypes.DEFAULT_TYPE) -> None:
    service = _service_from_context(context)
    contact = update.effective_message.contact
    reply = service.build_contact_reply(
        actor=_actor_from_update(update),
        phone_number=getattr(contact, "phone_number", None),
        shared_contact_user_id=str(contact.user_id) if getattr(contact, "user_id", None) is not None else None,
    )
    await update.effective_message.reply_text(reply.text, reply_markup=reply.reply_markup)


async def company_selected(update: Any, context: ContextTypes.DEFAULT_TYPE) -> None:
    service = _service_from_context(context)
    query = update.callback_query
    await query.answer()
    raw_data = str(query.data or "")
    _, _, company = raw_data.partition(":")
    reply = service.build_company_selection_reply(actor=_actor_from_update(update), company=company)
    await query.edit_message_text("✅ Empresa seleccionada.")
    await update.effective_chat.send_message(reply.text, reply_markup=reply.reply_markup)


async def text_message(update: Any, context: ContextTypes.DEFAULT_TYPE) -> None:
    service = _service_from_context(context)
    text = getattr(update.effective_message, "text", None)
    if text is None:
        return

    reply = service.build_job_title_reply(actor=_actor_from_update(update), message_text=text)
    if reply is None:
        return
    await update.effective_message.reply_text(reply.text, reply_markup=reply.reply_markup)


def create_telegram_onboarding_application(runtime: BackendRuntime) -> Application:
    token = runtime.settings.telegram_bot_token
    if token is None:
        raise ValueError("VISITOR_NOTIFY_TELEGRAM_BOT_TOKEN is required to run the Telegram bot")

    builder = ApplicationBuilder().token(token)
    if runtime.settings.telegram_api_base_url.rstrip("/") != "https://api.telegram.org":
        builder = builder.base_url(f"{runtime.settings.telegram_api_base_url.rstrip('/')}/bot")

    application = builder.build()
    application.bot_data[BOT_SERVICE_KEY] = TelegramBotFlowService(
        contact_lookup=runtime.repository,
        onboarding_client=LocalTelegramOnboardingClient(runtime.telegram_onboarding_service),
        registration_help_text=runtime.settings.telegram_registration_help_text,
    )
    application.add_handler(CommandHandler("start", start_command))
    application.add_handler(CommandHandler("help", help_message))
    application.add_handler(MessageHandler(filters.CONTACT, contact_shared))
    application.add_handler(MessageHandler(filters.Regex(f"^{re.escape(HELP_BUTTON_TEXT)}$"), help_message))
    application.add_handler(CallbackQueryHandler(company_selected, pattern=r"^prereg_company:"))
    application.add_handler(MessageHandler(filters.TEXT & ~filters.COMMAND, text_message))
    return application
