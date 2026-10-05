from datetime import datetime
from decimal import Decimal
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field


class TransactionCreated(BaseModel):
    model_config = ConfigDict(extra="ignore")
    eventId: UUID
    eventType: str = Field(pattern=r"^transaction\.created\.v1$")
    schemaVersion: int = Field(ge=1, le=1)
    correlationId: str = Field(min_length=1, max_length=100)
    occurredAt: datetime
    transactionId: UUID
    customerId: UUID
    amount: Decimal = Field(gt=0)
    currency: str = Field(min_length=3, max_length=3)
    merchant: str = Field(min_length=1, max_length=160)
    deviceId: str | None = None
    country: str | None = None
    customerAverage: Decimal | None = Field(default=None, ge=0)
    previousCountry: str | None = None
    previousLatitude: float | None = None
    latitude: float | None = None
    failedAttempts: int = Field(default=0, ge=0)
    recentTransactionCount: int = Field(default=0, ge=0)
    isNewDevice: bool = False
    merchantSeenBefore: bool = True

class FraudAnalysisResult(BaseModel):
    eventId: UUID
    eventType: str = "fraud.analysis.completed.v1"
    schemaVersion: int = 1
    correlationId: str
    occurredAt: datetime
    transactionId: UUID
    riskScore: Decimal = Field(ge=0, le=1)
    decision: str = Field(pattern=r"^(APPROVED|REVIEW|BLOCKED)$")
    reasons: list[str]
    modelVersion: str
