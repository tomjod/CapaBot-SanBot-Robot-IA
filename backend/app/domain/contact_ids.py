from __future__ import annotations

import re
import unicodedata

from backend.app.domain.company_catalog import company_contact_id_prefix


def _slugify(value: str) -> str:
    normalized = unicodedata.normalize("NFKD", value)
    ascii_value = normalized.encode("ascii", "ignore").decode("ascii")
    slug = re.sub(r"[^a-z0-9]+", "-", ascii_value.lower()).strip("-")
    return slug or "contacto"


def generate_contact_id(*, company_id: str, job_title: str, existing_ids: list[str]) -> str:
    company_prefix = company_contact_id_prefix(company_id)
    cargo_slug = _slugify(job_title)
    base = f"{company_prefix}-{cargo_slug}"
    pattern = re.compile(rf"^{re.escape(base)}-(\d{{3}})$")
    suffixes = [int(match.group(1)) for contact_id in existing_ids if (match := pattern.match(contact_id.strip()))]
    next_suffix = (max(suffixes) + 1) if suffixes else 1
    return f"{base}-{next_suffix:03d}"
