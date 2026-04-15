from __future__ import annotations

import unittest

from backend.app.application.telegram_onboarding_service import (
    select_company_response,
    submit_job_title_response,
    submit_telegram_onboarding_response,
)
from backend.app.bot.application import (
    LocalTelegramOnboardingClient,
    create_telegram_onboarding_application,
)
from backend.app.bot.service import HELP_BUTTON_TEXT, SHARE_CONTACT_BUTTON_TEXT, TelegramActor, TelegramBotFlowService
from backend.app.domain.contact import Contact
from backend.app.domain.telegram_onboarding import (
    PendingTelegramRegistration,
    TelegramOnboardingResult,
)
from backend.app.presentation.telegram.application import (
    LocalTelegramOnboardingClient as PresentationLocalTelegramOnboardingClient,
    create_telegram_onboarding_application as create_presentation_telegram_onboarding_application,
)


class StubContactLookup:
    def __init__(self, contact: Contact | None = None) -> None:
        self.contact = contact
        self.requests: list[str] = []

    def find_contact_by_telegram_user_id(self, telegram_user_id: str) -> Contact | None:
        self.requests.append(telegram_user_id)
        return self.contact


class StubOnboardingClient:
    def __init__(self, status_code: int = 200, response: dict | None = None) -> None:
        self.status_code = status_code
        self.response = response or {"status": "matched", "contact": {"display_name": "Ventas"}}
        self.payloads: list[dict] = []
        self.company_payloads: list[dict] = []
        self.job_title_payloads: list[dict] = []

    def submit(self, payload: dict) -> tuple[int, dict]:
        self.payloads.append(payload)
        return self.status_code, self.response

    def select_company(self, payload: dict) -> tuple[int, dict]:
        self.company_payloads.append(payload)
        return self.status_code, {"status": "awaiting_job_title"}

    def submit_job_title(self, payload: dict) -> tuple[int, dict]:
        self.job_title_payloads.append(payload)
        return self.status_code, {"status": "pending_registration_ready"}


class StubTelegramOnboardingApplicationService:
    def onboard(self, request):
        return TelegramOnboardingResult(
            status="matched",
            detail="Contacto Ada vinculado correctamente a Telegram.",
            normalized_phone="56912345678",
            contact=Contact(
                id="ventas",
                display_name="Ada Lovelace",
                job_title="Recepción",
                company="transformapp",
                telegram_chat_id="555",
                telegram_user_id="444",
                telegram_username="ventas_contacto",
            ),
        )

    def select_company(self, request):
        return TelegramOnboardingResult(
            status="awaiting_job_title",
            detail="Perfecto. Ahora escribí tu cargo para completar el preregistro.",
            normalized_phone="56900000000",
            pending_registration=PendingTelegramRegistration(
                phone_number="+56 9 0000 0000",
                normalized_phone="56900000000",
                telegram_user_id=request.telegram_user_id,
                telegram_chat_id="555",
                company=request.company,
                flow_state="awaiting_job_title",
                created_at="2026-04-13T12:00:00Z",
            ),
        )

    def submit_job_title(self, request):
        return TelegramOnboardingResult(
            status="pending_registration_ready",
            detail="Gracias. Tu preregistro quedó completo y será revisado por un administrador.",
            normalized_phone="56900000000",
            pending_registration=PendingTelegramRegistration(
                phone_number="+56 9 0000 0000",
                normalized_phone="56900000000",
                telegram_user_id=request.telegram_user_id,
                telegram_chat_id="555",
                company="transformapp",
                job_title=request.job_title,
                flow_state="ready_for_approval",
                created_at="2026-04-13T12:00:00Z",
            ),
        )


class LocalTelegramOnboardingClientContractTest(unittest.TestCase):
    def setUp(self) -> None:
        self.service = StubTelegramOnboardingApplicationService()
        self.client = LocalTelegramOnboardingClient(self.service)

    def test_legacy_module_bridges_to_presentation_telegram_application(self) -> None:
        self.assertIs(LocalTelegramOnboardingClient, PresentationLocalTelegramOnboardingClient)
        self.assertIs(
            create_telegram_onboarding_application,
            create_presentation_telegram_onboarding_application,
        )

    def test_submit_uses_shared_application_contract(self) -> None:
        payload = {
            "phone_number": "+56 9 1234 5678",
            "telegram_user_id": "444",
            "telegram_chat_id": "555",
            "telegram_username": "ventas_contacto",
            "first_name": "Ada",
        }

        self.assertEqual(self.client.submit(payload), submit_telegram_onboarding_response(self.service, payload))

    def test_select_company_uses_shared_application_contract(self) -> None:
        payload = {"telegram_user_id": "444", "company": "transformapp"}

        self.assertEqual(self.client.select_company(payload), select_company_response(self.service, payload))

    def test_submit_job_title_uses_shared_application_contract(self) -> None:
        payload = {"telegram_user_id": "444", "job_title": "Recepcionista"}

        self.assertEqual(self.client.submit_job_title(payload), submit_job_title_response(self.service, payload))


class TelegramBotFlowServiceTest(unittest.TestCase):
    def _actor(self) -> TelegramActor:
        return TelegramActor(user_id="444", chat_id="555", username="ventas_contacto", first_name="Ada")

    def _service(
        self,
        *,
        contact: Contact | None = None,
        status_code: int = 200,
        response: dict | None = None,
    ) -> tuple[TelegramBotFlowService, StubContactLookup, StubOnboardingClient]:
        lookup = StubContactLookup(contact=contact)
        client = StubOnboardingClient(status_code=status_code, response=response)
        return (
            TelegramBotFlowService(
                contact_lookup=lookup,
                onboarding_client=client,
                registration_help_text="Contactá al administrador de recepciones.",
            ),
            lookup,
            client,
        )

    def test_start_explains_linking_flow_for_unknown_user(self) -> None:
        service, lookup, _ = self._service()

        reply = service.build_start_reply(self._actor())

        self.assertEqual(lookup.requests, ["444"])
        self.assertIn("Puedo vincular tu cuenta de Telegram", reply.text)
        self.assertEqual(reply.reply_markup.keyboard[0][0].text, SHARE_CONTACT_BUTTON_TEXT)
        self.assertTrue(reply.reply_markup.keyboard[0][0].request_contact)
        self.assertEqual(reply.reply_markup.keyboard[1][0].text, HELP_BUTTON_TEXT)

    def test_start_shows_minimal_menu_for_already_linked_user(self) -> None:
        service, _, _ = self._service(
            contact=Contact(
                id="ventas",
                display_name="Ada Lovelace",
                job_title="Recepción",
                company="transformapp",
                telegram_user_id="444",
                telegram_chat_id="555",
            )
        )

        reply = service.build_start_reply(self._actor())

        self.assertIn("ya está vinculada", reply.text)
        self.assertIn("Ada Lovelace · Recepción · Transformapp", reply.text)
        self.assertTrue(reply.reply_markup.keyboard[0][0].request_contact)

    def test_contact_share_calls_onboarding_contract_and_confirms_match(self) -> None:
        service, _, client = self._service(
            response={
                "status": "matched",
                "contact": {
                    "display_name": "Ada Lovelace",
                    "job_title": "Recepción",
                    "company_label": "Transformapp",
                },
            }
        )

        reply = service.build_contact_reply(
            actor=self._actor(),
            phone_number="+56 9 1234 5678",
            shared_contact_user_id="444",
        )

        self.assertEqual(
            client.payloads,
            [
                {
                    "phone_number": "+56 9 1234 5678",
                    "telegram_user_id": "444",
                    "telegram_chat_id": "555",
                    "telegram_username": "ventas_contacto",
                    "first_name": "Ada",
                }
            ],
        )
        self.assertIn("✅ Vinculación completada", reply.text)
        self.assertIn("Ada Lovelace · Recepción · Transformapp", reply.text)

    def test_contact_share_prompts_company_selection_when_phone_is_not_registered(self) -> None:
        service, _, _ = self._service(
            response={
                "status": "awaiting_company_selection",
                "available_companies": [{"id": "transformapp", "label": "Transformapp"}],
            }
        )

        reply = service.build_contact_reply(
            actor=self._actor(),
            phone_number="+56 9 0000 0000",
            shared_contact_user_id="444",
        )

        self.assertIn("seleccioná la empresa", reply.text)
        self.assertEqual(reply.reply_markup.inline_keyboard[0][0].text, "Transformapp")

    def test_company_selection_requests_job_title(self) -> None:
        service, _, client = self._service()

        reply = service.build_company_selection_reply(actor=self._actor(), company="transformapp")

        self.assertEqual(client.company_payloads, [{"telegram_user_id": "444", "company": "transformapp"}])
        self.assertIn("Ahora escribí tu cargo", reply.text)

    def test_job_title_submission_completes_preregistration(self) -> None:
        service, _, client = self._service()

        reply = service.build_job_title_reply(actor=self._actor(), message_text="Recepcionista")

        assert reply is not None
        self.assertEqual(client.job_title_payloads, [{"telegram_user_id": "444", "job_title": "Recepcionista"}])
        self.assertIn("Preregistro completado", reply.text)

    def test_rejects_contact_shared_for_another_user(self) -> None:
        service, _, client = self._service()

        reply = service.build_contact_reply(
            actor=self._actor(),
            phone_number="+56 9 1234 5678",
            shared_contact_user_id="999",
        )

        self.assertIn("TU propio contacto", reply.text)
        self.assertEqual(client.payloads, [])

    def test_help_reply_explains_registration_path(self) -> None:
        service, _, _ = self._service()

        reply = service.build_help_reply(self._actor())

        self.assertIn("pedile al administrador", reply.text)
        self.assertIn("Contactá al administrador de recepciones.", reply.text)


if __name__ == "__main__":
    unittest.main()
