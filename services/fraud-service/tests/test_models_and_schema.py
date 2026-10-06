from datetime import UTC, datetime
from decimal import Decimal
from uuid import uuid4

import pytest
from pydantic import ValidationError

from riskguard_fraud.models import build_risk_model, features
from riskguard_fraud.schemas import TransactionCreated


def event(**changes):
    data={"eventId":uuid4(),"eventType":"transaction.created.v1","schemaVersion":1,"correlationId":"corr-test","occurredAt":datetime.now(UTC),"transactionId":uuid4(),"customerId":uuid4(),"amount":Decimal(100),"currency":"BRL","merchant":"Store"}
    data.update(changes)
    return TransactionCreated(**data)

def test_features_are_stable_and_capture_velocity_and_device():
    values=features(event(customerAverage=Decimal(50),recentTransactionCount=4,isNewDevice=True,latitude=10,previousLatitude=2))
    assert values[0]==100.0 and values[2]==4.0 and values[4]==1.0 and values[-1]==8.0

def test_invalid_event_version_and_amount_are_rejected():
    with pytest.raises(ValidationError): event(schemaVersion=2)
    with pytest.raises(ValidationError): event(amount=Decimal(0))

def test_missing_ml_artifact_falls_back_to_rules(monkeypatch):
    monkeypatch.setenv("RISK_ENGINE","ml")
    monkeypatch.setenv("RISK_MODEL_PATH","/tmp/does-not-exist-riskguard.joblib")
    assert build_risk_model().model_version.startswith("rules-")
