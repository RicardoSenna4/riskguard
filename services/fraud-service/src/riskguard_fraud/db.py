from datetime import UTC, datetime

from sqlalchemy import JSON, DateTime, Numeric, String, create_engine
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column, sessionmaker


class Base(DeclarativeBase): pass
class ProcessedEvent(Base):
    __tablename__ = "processed_events"
    event_id: Mapped[str] = mapped_column(String(36), primary_key=True)
    processed_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=lambda: datetime.now(UTC))
class FraudAnalysisRecord(Base):
    __tablename__ = "fraud_analyses"
    transaction_id: Mapped[str] = mapped_column(String(36), primary_key=True)
    event_id: Mapped[str] = mapped_column(String(36), unique=True)
    risk_score: Mapped[float] = mapped_column(Numeric(5, 4))
    decision: Mapped[str] = mapped_column(String(20))
    reasons: Mapped[list] = mapped_column(JSON)
    model_version: Mapped[str] = mapped_column(String(80))
    correlation_id: Mapped[str] = mapped_column(String(100))
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=lambda: datetime.now(UTC))

def create_session(database_url: str):
    engine = create_engine(database_url, future=True)
    Base.metadata.create_all(engine)
    return sessionmaker(engine, expire_on_commit=False)
