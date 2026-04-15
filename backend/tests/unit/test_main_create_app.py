from __future__ import annotations

import types
import unittest
from builtins import __import__ as original_import
from unittest.mock import patch

from backend.app import main


class FakeFastAPI:
    def __init__(self, *, title: str) -> None:
        self.title = title


class CreateAppCompatibilityTest(unittest.TestCase):
    def test_create_app_wires_runtime_into_legacy_entry_point(self) -> None:
        runtime = types.SimpleNamespace(
            repository=object(),
            notification_service=object(),
            pending_registration_repository=object(),
            telegram_onboarding_service=object(),
        )
        fake_fastapi_module = types.ModuleType("fastapi")
        fake_fastapi_module.FastAPI = FakeFastAPI

        with patch.dict("sys.modules", {"fastapi": fake_fastapi_module}):
            with patch("backend.app.main.build_runtime", return_value=runtime) as build_runtime:
                with patch("backend.app.main.register_contacts_routes") as register_contacts_routes:
                    with patch("backend.app.main.register_notification_routes") as register_notification_routes:
                        with patch("backend.app.main.register_admin_contacts_routes") as register_admin_contacts_routes:
                            with patch("backend.app.main.register_telegram_onboarding_routes") as register_telegram_onboarding_routes:
                                app = main.create_app()

        self.assertIsInstance(app, FakeFastAPI)
        self.assertEqual(app.title, "Visitor Notify Assistant MVP")
        build_runtime.assert_called_once_with()
        register_contacts_routes.assert_called_once_with(app, runtime.repository)
        register_notification_routes.assert_called_once_with(app, runtime.notification_service)
        register_admin_contacts_routes.assert_called_once_with(
            app,
            runtime.repository,
            runtime.pending_registration_repository,
        )
        register_telegram_onboarding_routes.assert_called_once_with(app, runtime.telegram_onboarding_service)

    def test_create_app_raises_clear_error_when_fastapi_is_missing(self) -> None:
        with patch.dict("sys.modules", {}, clear=False):
            with patch("builtins.__import__", side_effect=self._fail_fastapi_import):
                with self.assertRaises(RuntimeError) as raised:
                    main.create_app()

        self.assertIn("FastAPI is not installed", str(raised.exception))

    @staticmethod
    def _fail_fastapi_import(name, globals=None, locals=None, fromlist=(), level=0):
        if name == "fastapi":
            raise ModuleNotFoundError("No module named 'fastapi'")
        return original_import(name, globals, locals, fromlist, level)


if __name__ == "__main__":
    unittest.main()
