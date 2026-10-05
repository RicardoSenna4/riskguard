import os
import time

from fastapi import FastAPI
from prometheus_client import make_asgi_app

from .db import FraudAnalysisRecord, create_session
from .models import build_risk_model
from .observability import ANALYSES, LATENCY, REQUESTS, configure_logging
from .schemas import FraudAnalysisResult, TransactionCreated

app = FastAPI(title="RiskGuard Fraud Service", version="0.1.0")
configure_logging()
model = build_risk_model()
SessionLocal = create_session(os.getenv("DATABASE_URL", "sqlite:///./riskguard-fraud.db"))
app.mount("/metrics", make_asgi_app())

@app.middleware("http")
async def observability(request, call_next):
    correlation_id=request.headers.get("X-Correlation-Id") or os.urandom(16).hex()
    started=time.perf_counter()
    response=await call_next(request)
    REQUESTS.labels(request.method,request.url.path,str(response.status_code)).inc()
    response.headers["X-Correlation-Id"]=correlation_id
    LATENCY.observe(time.perf_counter()-started)
    return response

@app.get("/health")
def health() -> dict[str, str]: return {"status": "ok", "service": "fraud-service"}

@app.post("/api/v1/analyze", response_model=FraudAnalysisResult)
def analyze(event: TransactionCreated) -> FraudAnalysisResult:
    session = SessionLocal()
    try:
        existing = session.query(FraudAnalysisRecord).filter_by(transaction_id=str(event.transactionId)).first()
        if existing:
            return FraudAnalysisResult(eventId=event.eventId, correlationId=event.correlationId, occurredAt=existing.created_at, transactionId=event.transactionId, riskScore=existing.risk_score, decision=existing.decision, reasons=existing.reasons, modelVersion=existing.model_version)
        result = model.analyze(event)
        ANALYSES.labels(result.decision,result.modelVersion).inc()
        session.add(FraudAnalysisRecord(transaction_id=str(event.transactionId), event_id=str(result.eventId), risk_score=float(result.riskScore), decision=result.decision, reasons=result.reasons, model_version=result.modelVersion, correlation_id=result.correlationId))
        session.commit()
        return result
    finally: session.close()
