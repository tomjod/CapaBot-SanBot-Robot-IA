from __future__ import annotations

import unittest
from datetime import datetime, timezone

from backend.app.api.routes.notifications import register_notification_routes, submit_notification_response
from backend.app.presentation.http.notifications_router import (
    register_notification_routes as presentation_register_notification_routes,
    submit_notification_response as presentation_submit_notification_response,
)
from backend.app.domain.providers import ChannelDelivery, NotificationOutcome


class StubNotificationService:
    def __init__(self, outcome: NotificationOutcome | None = None, error: Exception | None = None) -> None:
        self._outcome = outcome
        self._error = error
        self.last_request = None

    def submit(self, request):
        if self._error is not None:
            raise self._error
        self.last_request = request
        return self._outcome


class NotificationsRouteContractTest(unittest.TestCase):
    def test_legacy_module_bridges_to_presentation_router_contracts(self) -> None:
        self.assertIs(submit_notification_response, presentation_submit_notification_response)
        self.assertIs(register_notification_routes, presentation_register_notification_routes)

    def _payload(self) -> dict[str, str]:
        return {
            "contact_id": "ventas-1",
            "device_id": "sanbot-01",
            "requested_at": datetime(2026, 4, 9, 18, 0, tzinfo=timezone.utc).isoformat(),
        }

    def test_returns_accepted_business_contract(self) -> None:
        service = StubNotificationService(
            outcome=NotificationOutcome(
                status="accepted",
                telegram=ChannelDelivery("accepted"),
                email=ChannelDelivery("skipped"),
                retryable=False,
                detail="Listo, avisamos a Ventas por Telegram.",
            )
        )

        status_code, response = submit_notification_response(service, self._payload())

        self.assertEqual(status_code, 200)
        self.assertEqual(response, {
            "status": "accepted",
            "channels": {"telegram": "accepted", "email": "skipped"},
            "retryable": False,
            "detail": "Listo, avisamos a Ventas por Telegram.",
        })

    def test_returns_unavailable_business_contract(self) -> None:
        service = StubNotificationService(
            outcome=NotificationOutcome(
                status="unavailable",
                telegram=ChannelDelivery("unavailable"),
                email=ChannelDelivery("unavailable"),
                retryable=False,
                detail="Ventas no tiene Telegram disponible y el email no está habilitado.",
            )
        )

        status_code, response = submit_notification_response(service, self._payload())

        self.assertEqual(status_code, 200)
        self.assertEqual(response["status"], "unavailable")
        self.assertEqual(response["channels"], {"telegram": "unavailable", "email": "unavailable"})
        self.assertIn("no tiene Telegram disponible", response["detail"])

    def test_returns_dual_channel_business_contract_when_email_is_attempted(self) -> None:
        service = StubNotificationService(
            outcome=NotificationOutcome(
                status="delivered_or_queued",
                telegram=ChannelDelivery("sent"),
                email=ChannelDelivery("accepted"),
                retryable=False,
                detail="Listo, avisamos a Ventas por Telegram y email.",
            )
        )

        status_code, response = submit_notification_response(service, self._payload())

        self.assertEqual(status_code, 200)
        self.assertEqual(response, {
            "status": "delivered_or_queued",
            "channels": {"telegram": "sent", "email": "accepted"},
            "retryable": False,
            "detail": "Listo, avisamos a Ventas por Telegram y email.",
        })

    def test_maps_validation_errors_to_400(self) -> None:
        status_code, response = submit_notification_response(StubNotificationService(), {"device_id": "sanbot-01"})

        self.assertEqual(status_code, 400)
        self.assertEqual(response["status"], "failed")
        self.assertFalse(response["retryable"])
        self.assertIn("requested_at", response["detail"])

    def test_maps_timeout_and_unexpected_errors(self) -> None:
        timeout_code, timeout_response = submit_notification_response(
            StubNotificationService(error=TimeoutError()),
            self._payload(),
        )
        error_code, error_response = submit_notification_response(
            StubNotificationService(error=RuntimeError("boom")),
            self._payload(),
        )

        self.assertEqual(timeout_code, 504)
        self.assertTrue(timeout_response["retryable"])
        self.assertIn("demoró demasiado", timeout_response["detail"])
        self.assertEqual(error_code, 500)
        self.assertEqual(error_response["channels"], {"telegram": "failed", "email": "failed"})
        self.assertIn("error inesperado", error_response["detail"])

    def test_preserves_leave_message_reason_and_message_in_api_contract(self) -> None:
        service = StubNotificationService(
            outcome=NotificationOutcome(
                status="accepted",
                telegram=ChannelDelivery("accepted"),
                email=ChannelDelivery("skipped"),
                retryable=False,
                detail="Listo, avisamos a Ventas por Telegram.",
            )
        )
        payload = self._payload() | {
            "visitor_name": "  Ada ",
            "location": " Recepción ",
            "reason": " leave_message ",
            "message": "  Necesito que me contacten mañana.  ",
        }

        status_code, response = submit_notification_response(service, payload)

        self.assertEqual(status_code, 200)
        self.assertEqual(response["status"], "accepted")
        self.assertIsNotNone(service.last_request)
        self.assertEqual(service.last_request.visitor_name, "Ada")
        self.assertEqual(service.last_request.location, "Recepción")
        self.assertEqual(service.last_request.reason, "leave_message")
        self.assertEqual(service.last_request.message, "Necesito que me contacten mañana.")


if __name__ == "__main__":
    unittest.main()
