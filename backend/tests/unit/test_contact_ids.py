from __future__ import annotations

import unittest

from backend.app.domain.contact_ids import generate_contact_id


class ContactIdGenerationTest(unittest.TestCase):
    def test_generates_company_and_job_based_identifier(self) -> None:
        generated = generate_contact_id(
            company_id="transformapp",
            job_title="Recepcionista Senior",
            existing_ids=[],
        )

        self.assertEqual(generated, "ta-recepcionista-senior-001")

    def test_uses_next_suffix_when_duplicate_exists(self) -> None:
        generated = generate_contact_id(
            company_id="transformapp",
            job_title="Recepcionista",
            existing_ids=["ta-recepcionista-001", "ta-recepcionista-002", "tps-recepcionista-001"],
        )

        self.assertEqual(generated, "ta-recepcionista-003")

    def test_normalizes_accents_and_symbols(self) -> None:
        generated = generate_contact_id(
            company_id="quimica_mavar",
            job_title="Jefe de Operación & Soporte",
            existing_ids=[],
        )

        self.assertEqual(generated, "qm-jefe-de-operacion-soporte-001")


if __name__ == "__main__":
    unittest.main()
