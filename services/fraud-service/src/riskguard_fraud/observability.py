from __future__ import annotations

import json
import logging
import time

from prometheus_client import Counter, Histogram

REQUESTS=Counter('riskguard_fraud_requests_total','Fraud API requests',['method','path','status'])
ANALYSES=Counter('riskguard_fraud_analysis_total','Fraud analysis decisions',['decision','model_version'])
LATENCY=Histogram('riskguard_fraud_analysis_latency_seconds','Fraud analysis latency')
class JsonFormatter(logging.Formatter):
    def format(self,record:logging.LogRecord)->str:
        return json.dumps({'timestamp':self.formatTime(record,'%Y-%m-%dT%H:%M:%S%z'),'level':record.levelname,'service':'fraud-service','message':record.getMessage(),**getattr(record,'context',{})},ensure_ascii=False)
def configure_logging()->None:
    handler=logging.StreamHandler();handler.setFormatter(JsonFormatter());root=logging.getLogger();root.handlers.clear();root.addHandler(handler);root.setLevel(logging.INFO)
def observe_analysis(fn):
    def wrapped(*args,**kwargs):
        started=time.perf_counter();result=fn(*args,**kwargs);LATENCY.observe(time.perf_counter()-started);ANALYSES.labels(result.decision,result.modelVersion).inc();return result
    return wrapped
