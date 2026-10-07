"""Alembic migrations must build the schema the ORM expects.

Runs against FRAUD_TEST_DATABASE_URL when set (CI points it at PostgreSQL),
otherwise against a throwaway SQLite file.
"""
import os
from pathlib import Path

from alembic import command
from alembic.config import Config
from sqlalchemy import create_engine, inspect, text

from riskguard_fraud.db import Base

SERVICE_ROOT = Path(__file__).resolve().parents[1]


def test_migrations_create_orm_tables_and_honor_database_url(tmp_path, monkeypatch):
    url = os.getenv("FRAUD_TEST_DATABASE_URL") or f"sqlite:///{tmp_path / 'fraud.db'}"
    monkeypatch.setenv("DATABASE_URL", url)
    config = Config(str(SERVICE_ROOT / "alembic.ini"))
    config.set_main_option("script_location", str(SERVICE_ROOT / "migrations"))

    command.downgrade(config, "base")
    command.upgrade(config, "head")

    engine = create_engine(url)
    try:
        inspector = inspect(engine)
        for table in Base.metadata.sorted_tables:
            assert table.name in inspector.get_table_names()
            assert {column["name"] for column in inspector.get_columns(table.name)} == set(table.columns.keys())
        with engine.connect() as connection:
            assert connection.execute(text("SELECT version_num FROM alembic_version")).scalar_one() == "0001_initial"
    finally:
        engine.dispose()
