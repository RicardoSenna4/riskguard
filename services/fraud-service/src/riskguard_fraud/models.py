from __future__ import annotations

import os
from decimal import Decimal
from pathlib import Path
from typing import Protocol

from .rules import RiskEngine
from .schemas import FraudAnalysisResult, TransactionCreated

FEATURE_NAMES=("amount","hour","recent_transaction_count","failed_attempts","is_new_device","country_changed","merchant_seen_before","average_ratio","distance")
class RiskModel(Protocol):
    model_version: str
    def analyze(self,event: TransactionCreated)->FraudAnalysisResult: ...

def features(event: TransactionCreated)->list[float]:
    average_ratio=float(event.amount/(event.customerAverage or event.amount))
    distance=abs((event.latitude or 0)-(event.previousLatitude or 0)) if event.latitude is not None and event.previousLatitude is not None else 0.0
    return [float(event.amount),float(event.occurredAt.hour),float(event.recentTransactionCount),float(event.failedAttempts),float(event.isNewDevice),float(bool(event.country and event.previousCountry and event.country!=event.previousCountry)),float(event.merchantSeenBefore),average_ratio,distance]

class RuleBasedRiskModel:
    def __init__(self,engine:RiskEngine|None=None): self.engine=engine or RiskEngine(); self.model_version=self.engine.model_version
    def analyze(self,event:TransactionCreated)->FraudAnalysisResult: return self.engine.analyze(event)

class MachineLearningRiskModel:
    def __init__(self,artifact_path:str):
        try:
            import joblib
        except ImportError as exc: raise RuntimeError("ML dependencies are not installed") from exc
        artifact=Path(artifact_path)
        if not artifact.exists(): raise FileNotFoundError(f"ML artifact not found: {artifact}")
        bundle=joblib.load(artifact); self.model=bundle["model"]; self.model_version=bundle["model_version"]; self.thresholds=bundle.get("thresholds",{"review":0.45,"block":0.80})
        if bundle.get("features")!=list(FEATURE_NAMES): raise ValueError("Unsupported feature contract for ML artifact")
    def analyze(self,event:TransactionCreated)->FraudAnalysisResult:
        score=float(self.model.predict_proba([features(event)])[0][1]); reasons=["ml_high_probability"] if score>=self.thresholds["review"] else []
        if event.isNewDevice: reasons.append("new_device_signal")
        if event.recentTransactionCount>=5: reasons.append("velocity_signal")
        decision="BLOCKED" if score>=self.thresholds["block"] else "REVIEW" if score>=self.thresholds["review"] else "APPROVED"
        from datetime import UTC, datetime
        from uuid import uuid4
        return FraudAnalysisResult(eventId=uuid4(),correlationId=event.correlationId,occurredAt=datetime.now(UTC),transactionId=event.transactionId,riskScore=Decimal(str(score)),decision=decision,reasons=reasons,modelVersion=self.model_version)

def build_risk_model()->RiskModel:
    mode=os.getenv("RISK_ENGINE","rules").lower()
    if mode=="ml":
        try: return MachineLearningRiskModel(os.getenv("RISK_MODEL_PATH","ml/artifacts/riskguard.joblib"))
        except (FileNotFoundError,RuntimeError,ValueError):
            if os.getenv("RISK_MODEL_FAIL_FAST","false").lower()=="true": raise
    return RuleBasedRiskModel()
