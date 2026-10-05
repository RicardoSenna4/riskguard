from datetime import UTC, datetime
from decimal import Decimal
from uuid import uuid4

from riskguard_fraud.rules import RiskEngine
from riskguard_fraud.schemas import TransactionCreated


def event(**changes):
    data = {"eventId": uuid4(), "eventType": "transaction.created.v1", "schemaVersion": 1, "correlationId": "corr-1", "occurredAt": datetime.now(UTC), "transactionId": uuid4(), "customerId": uuid4(), "amount": Decimal(10), "currency": "BRL", "merchant": "Store"}
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
