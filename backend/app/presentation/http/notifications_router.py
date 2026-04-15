from __future__ import annotations

from typing import Any, Protocol

from backend.app.domain.notification import NotificationRequest


class NotificationSubmissionService(Protocol):
    def submit(self, request: NotificationRequest) -> Any:
        ...


def submit_notification_response(service: NotificationSubmissionService, payload: dict[str, Any]) -> tuple[int, dict[str, Any]]:
    try:
        request = NotificationRequest.from_dict(payload)
        outcome = service.submit(request)
        return 200, outcome.to_dict()
    except ValueError as error:
        return 400, {
            "status": "failed",
            "channels": {"telegram": "failed", "email": "failed"},
            "retryable": False,
            "detail": str(error),
        }
    except TimeoutError:
        return 504, {
            "status": "failed",
            "channels": {"telegram": "failed", "email": "failed"},
            "retryable": True,
            "detail": "El backend demoró demasiado en procesar la notificación.",
        }
    except Exception:
        return 500, {
            "status": "failed",
            "channels": {"telegram": "failed", "email": "failed"},
            "retryable": True,
            "detail": "Ocurrió un error inesperado al procesar la notificación.",
        }


def register_notification_routes(app: Any, service: NotificationSubmissionService) -> None:
    from fastapi import APIRouter
    from fastapi.responses import JSONResponse

    router = APIRouter()

    @router.post("/notifications")
    def post_notification(payload: dict[str, Any]) -> Any:
        status_code, response = submit_notification_response(service, payload)
        if status_code == 200:
            return response
        return JSONResponse(status_code=status_code, content=response)

    app.include_router(router)
