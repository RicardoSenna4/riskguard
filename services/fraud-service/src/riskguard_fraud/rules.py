from dataclasses import dataclass
from datetime import UTC, datetime
from decimal import Decimal
from math import sqrt
from uuid import uuid4

from .schemas import FraudAnalysisResult, TransactionCreated


@dataclass(frozen=True)
class Thresholds:
    high_amount: Decimal = Decimal(1000)
    high_amount_score: Decimal = Decimal("0.35")
    average_multiplier: Decimal = Decimal(3)
    new_device_score: Decimal = Decimal("0.12")
    country_score: Decimal = Decimal("0.18")
    new_merchant_score: Decimal = Decimal("0.10")
    velocity_score: Decimal = Decimal("0.15")
    unusual_hour_score: Decimal = Decimal("0.08")
    failed_attempt_score: Decimal = Decimal("0.12")
    distance_score: Decimal = Decimal("0.18")
    review_threshold: Decimal = Decimal("0.45")
    block_threshold: Decimal = Decimal("0.80")

class RiskEngine:
    model_version = "rules-v1"
    def __init__(self, thresholds: Thresholds | None = None): self.thresholds = thresholds or Thresholds()
    def analyze(self, event: TransactionCreated) -> FraudAnalysisResult:
        score = Decimal(0); reasons: list[str] = []; t = self.thresholds
        def add(amount: Decimal, reason: str):
            nonlocal score
            score += amount; reasons.append(reason)
        if event.amount >= t.high_amount: add(t.high_amount_score, "high_amount")
        if event.customerAverage and event.amount >= event.customerAverage * t.average_multiplier: add(Decimal("0.18"), "amount_far_above_customer_average")
        if event.isNewDevice: add(t.new_device_score, "new_device")
        if event.country and event.previousCountry and event.country != event.previousCountry: add(t.country_score, "country_changed")
        if not event.merchantSeenBefore: add(t.new_merchant_score, "new_merchant")
        if event.recentTransactionCount >= 5: add(t.velocity_score, "high_velocity")
        if event.occurredAt.hour < 6 or event.occurredAt.hour >= 23: add(t.unusual_hour_score, "unusual_hour")
        if event.failedAttempts >= 3: add(t.failed_attempt_score, "sequence_of_failures")
        if event.latitude is not None and event.previousLatitude is not None:
            distance = sqrt((event.latitude - event.previousLatitude) ** 2)
            if distance > 10: add(t.distance_score, "unusual_distance_from_previous_transaction")
        score = min(Decimal(1), score.quantize(Decimal("0.0001")))
        decision = "BLOCKED" if score >= t.block_threshold else "REVIEW" if score >= t.review_threshold else "APPROVED"
        return FraudAnalysisResult(eventId=uuid4(), correlationId=event.correlationId, occurredAt=datetime.now(UTC), transactionId=event.transactionId, riskScore=score, decision=decision, reasons=reasons, modelVersion=self.model_version)
