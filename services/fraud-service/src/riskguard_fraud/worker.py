import asyncio
import json
import os

from aiokafka import AIOKafkaConsumer, AIOKafkaProducer
from pydantic import ValidationError

from .app import SessionLocal, model
from .db import FraudAnalysisRecord, ProcessedEvent
from .schemas import TransactionCreated


async def run() -> None:
    bootstrap = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092")
    consumer = AIOKafkaConsumer("transaction.created.v1", bootstrap_servers=bootstrap, group_id="riskguard-fraud-service", enable_auto_commit=False, auto_offset_reset="earliest")
    producer = AIOKafkaProducer(bootstrap_servers=bootstrap)
    await consumer.start(); await producer.start()
    try:
        async for message in consumer:
            try:
                event = TransactionCreated.model_validate_json(message.value)
                session = SessionLocal()
                try:
                    if session.get(ProcessedEvent, str(event.eventId)) or session.get(FraudAnalysisRecord, str(event.transactionId)):
                        await consumer.commit(); continue
                    result = model.analyze(event)
                    session.add(FraudAnalysisRecord(transaction_id=str(event.transactionId), event_id=str(result.eventId), risk_score=float(result.riskScore), decision=result.decision, reasons=result.reasons, model_version=result.modelVersion, correlation_id=result.correlationId))
                    session.add(ProcessedEvent(event_id=str(event.eventId)))
                    session.commit()
                finally: session.close()
                # Keyword arguments: send_and_wait(topic, value, key) - the key is the transaction id as text, like the Java outbox.
                await producer.send_and_wait("fraud.analysis.completed.v1", value=result.model_dump_json().encode(), key=str(result.transactionId).encode())
                await consumer.commit()
            except (ValidationError, ValueError, KeyError) as error:
                await producer.send_and_wait("fraud.analysis.dlq.v1", value=json.dumps({"error": str(error), "payload": message.value.decode()}).encode(), key=message.key)
                await consumer.commit()
            except (OSError, RuntimeError):
                await asyncio.sleep(1)
    finally:
        await consumer.stop(); await producer.stop()

if __name__ == "__main__": asyncio.run(run())
