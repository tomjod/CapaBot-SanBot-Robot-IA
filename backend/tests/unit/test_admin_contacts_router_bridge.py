from __future__ import annotations

import unittest

from backend.app.api.routes.admin_contacts import (
    _admin_approval_form_state,
    _admin_form_state,
    _build_admin_contacts_redirect,
    register_admin_contacts_routes,
)
from backend.app.presentation.http.admin_contacts_router import (
    _admin_approval_form_state as presentation_admin_approval_form_state,
    _admin_form_state as presentation_admin_form_state,
    _build_admin_contacts_redirect as presentation_build_admin_contacts_redirect,
    register_admin_contacts_routes as presentation_register_admin_contacts_routes,
)


class AdminContactsRouterBridgeTest(unittest.TestCase):
    def test_legacy_module_bridges_to_presentation_router_contracts(self) -> None:
        self.assertIs(_build_admin_contacts_redirect, presentation_build_admin_contacts_redirect)
        self.assertIs(_admin_form_state, presentation_admin_form_state)
        self.assertIs(_admin_approval_form_state, presentation_admin_approval_form_state)
        self.assertIs(register_admin_contacts_routes, presentation_register_admin_contacts_routes)


if __name__ == "__main__":
    unittest.main()
