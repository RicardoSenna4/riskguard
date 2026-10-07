from datetime import UTC, datetime
from decimal import Decimal
from uuid import uuid4

from riskguard_fraud.rules import RiskEngine
from riskguard_fraud.schemas import TransactionCreated

# Fixed business-hours timestamp: the engine flags 23:00-06:00 UTC as unusual_hour,
# so using the wall clock made these tests fail when CI ran at night.
DAYTIME = datetime(2026, 1, 15, 14, 0, tzinfo=UTC)

def event(**changes):
    data = {"eventId": uuid4(), "eventType": "transaction.created.v1", "schemaVersion": 1, "correlationId": "corr-1", "occurredAt": DAYTIME, "transactionId": uuid4(), "customerId": uuid4(), "amount": Decimal(10), "currency": "BRL", "merchant": "Store"}
    data.update(changes)
    return TransactionCreated(**data)

def test_normal_transaction_is_approved():
    result = RiskEngine().analyze(event())
    assert result.decision == "APPROVED" and result.riskScore == 0

def test_multiple_rules_block_high_risk_transaction():
    result = RiskEngine().analyze(event(amount=Decimal(2000), isNewDevice=True, merchantSeenBefore=False, recentTransactionCount=8, failedAttempts=4, country="US", previousCountry="BR", latitude=50, previousLatitude=0))
    assert result.decision == "BLOCKED"
    assert {"high_amount", "new_device", "new_merchant", "high_velocity", "sequence_of_failures"}.issubset(result.reasons)

def test_medium_score_goes_to_review():
    result = RiskEngine().analyze(event(amount=Decimal(1200), isNewDevice=True))
    assert result.decision == "REVIEW"
    assert Decimal(0) <= result.riskScore <= Decimal(1)

def test_night_transaction_is_flagged_as_unusual_hour():
    result = RiskEngine().analyze(event(occurredAt=datetime(2026, 1, 15, 2, 0, tzinfo=UTC)))
    assert "unusual_hour" in result.reasons
