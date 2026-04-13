from __future__ import annotations

import unittest

from backend.app.api.routes.telegram_onboarding import submit_telegram_onboarding_response
from backend.app.domain.contact import Contact
from backend.app.domain.telegram_onboarding import PendingTelegramRegistration, TelegramOnboardingResult


class StubTelegramOnboardingService:
    def __init__(self, result: TelegramOnboardingResult | None = None, error: Exception | None = None) -> None:
        self._result = result
        self._error = error

    def onboard(self, request):
        if self._error is not None:
            raise self._error
        return self._result


class TelegramOnboardingRouteContractTest(unittest.TestCase):
    def test_returns_matched_contract(self) -> None:
        status_code, response = submit_telegram_onboarding_response(
            StubTelegramOnboardingService(
                result=TelegramOnboardingResult(
                    status="matched",
                    detail="Contacto Ventas vinculado correctamente a Telegram.",
                    normalized_phone="56912345678",
                    contact=Contact(
                        id="ventas",
                        display_name="Ventas",
                        job_title="Ejecutiva comercial",
                        company="transformapp",
                        phone="+56 9 1234 5678",
                        telegram_chat_id="555",
                        telegram_user_id="444",
                        telegram_username="ventas_contacto",
                    ),
                )
            ),
            {
                "phone_number": "+56 9 1234 5678",
                "telegram_user_id": "444",
                "telegram_chat_id": "555",
            },
        )

        self.assertEqual(status_code, 200)
        self.assertEqual(response["status"], "matched")
        self.assertEqual(response["contact"]["company_label"], "Transformapp")

    def test_returns_company_selection_contract(self) -> None:
        status_code, response = submit_telegram_onboarding_response(
            StubTelegramOnboardingService(
                result=TelegramOnboardingResult(
                    status="awaiting_company_selection",
                    detail="Recibimos tu teléfono. Ahora seleccioná la empresa para continuar con el preregistro.",
                    normalized_phone="56900000000",
                    pending_registration=PendingTelegramRegistration(
                        phone_number="+56 9 0000 0000",
                        normalized_phone="56900000000",
                        telegram_user_id="444",
                        telegram_chat_id="555",
                        telegram_username="ada",
                        first_name="Ada",
                        flow_state="awaiting_company",
                        created_at="2026-04-13T12:00:00Z",
                    ),
                    available_companies=[{"id": "transformapp", "label": "Transformapp"}],
                )
            ),
            {
                "phone_number": "+56 9 0000 0000",
                "telegram_user_id": "444",
                "telegram_chat_id": "555",
                "first_name": "Ada",
            },
        )

        self.assertEqual(status_code, 200)
        self.assertEqual(response["status"], "awaiting_company_selection")
        self.assertEqual(response["normalized_phone"], "56900000000")
        self.assertEqual(response["pending_registration"]["first_name"], "Ada")
        self.assertEqual(response["pending_registration"]["flow_state"], "awaiting_company")
        self.assertEqual(response["available_companies"][0]["id"], "transformapp")

    def test_maps_validation_and_unexpected_errors(self) -> None:
        invalid_status, invalid_response = submit_telegram_onboarding_response(
            StubTelegramOnboardingService(),
            {"telegram_user_id": "444", "telegram_chat_id": "555"},
        )
        error_status, error_response = submit_telegram_onboarding_response(
            StubTelegramOnboardingService(error=RuntimeError("boom")),
            {
                "phone_number": "+56 9 1234 5678",
                "telegram_user_id": "444",
                "telegram_chat_id": "555",
            },
        )

        self.assertEqual(invalid_status, 400)
        self.assertEqual(invalid_response["status"], "failed")
        self.assertIn("phone_number", invalid_response["detail"])
        self.assertEqual(error_status, 500)
        self.assertEqual(error_response["status"], "failed")


if __name__ == "__main__":
    unittest.main()
