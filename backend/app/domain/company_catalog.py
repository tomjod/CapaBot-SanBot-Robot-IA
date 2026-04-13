from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class CompanyCatalogEntry:
    id: str
    label: str
    contact_id_prefix: str


_COMPANY_CATALOG: tuple[CompanyCatalogEntry, ...] = (
    CompanyCatalogEntry(id="transformapp", label="Transformapp", contact_id_prefix="ta"),
    CompanyCatalogEntry(id="tps", label="TPS", contact_id_prefix="tps"),
    CompanyCatalogEntry(id="quimica_mavar", label="Química Mavar", contact_id_prefix="qm"),
    CompanyCatalogEntry(id="mundos_virtuales", label="Mundos Virtuales", contact_id_prefix="mv"),
    CompanyCatalogEntry(id="tra", label="TRA", contact_id_prefix="tra"),
    CompanyCatalogEntry(id="data_center", label="Data Center", contact_id_prefix="dc"),
    CompanyCatalogEntry(id="yerbas_buenas", label="Agrícola Yerbas Buenas", contact_id_prefix="yb"),
    CompanyCatalogEntry(id="lb", label="LB", contact_id_prefix="lb"),
    CompanyCatalogEntry(id="micro_renta", label="Micro Renta", contact_id_prefix="mr"),
)

_COMPANY_LABELS = {entry.id: entry.label for entry in _COMPANY_CATALOG}
_COMPANY_ID_PREFIXES = {entry.id: entry.contact_id_prefix for entry in _COMPANY_CATALOG}


def list_company_catalog() -> list[CompanyCatalogEntry]:
    return list(_COMPANY_CATALOG)


def is_known_company(company_id: str) -> bool:
    return company_id in _COMPANY_LABELS


def company_label(company_id: str | None) -> str | None:
    if company_id is None:
        return None
    return _COMPANY_LABELS.get(company_id)


def company_contact_id_prefix(company_id: str) -> str:
    normalized_company_id = company_id.strip()
    prefix = _COMPANY_ID_PREFIXES.get(normalized_company_id)
    if prefix is None:
        raise ValueError("company must be one of the configured catalog entries")
    return prefix
