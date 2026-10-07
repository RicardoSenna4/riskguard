"""Worker wiring: what it publishes to Kafka for valid and invalid events."""
import asyncio
import json
from types import SimpleNamespace
from uuid import uuid4

from riskguard_fraud import worker
from riskguard_fraud.db import create_session


class FakeConsumer:
    def __init__(self, messages):
        self.messages = messages
        self.commits = 0

    async def start(self): pass
    async def stop(self): pass
    async def commit(self): self.commits += 1

    def __aiter__(self):
        return self._iterate()

    async def _iterate(self):
        for message in self.messages:
            yield message


class FakeProducer:
    def __init__(self):
        self.sent = []

    async def start(self): pass
    async def stop(self): pass

    async def send_and_wait(self, topic, value=None, key=None):
        self.sent.append({"topic": topic, "value": value, "key": key})


def run_worker(monkeypatch, tmp_path, messages):
    consumer, producer = FakeConsumer(messages), FakeProducer()
    monkeypatch.setattr(worker, "AIOKafkaConsumer", lambda *args, **kwargs: consumer)
    monkeypatch.setattr(worker, "AIOKafkaProducer", lambda *args, **kwargs: producer)
    monkeypatch.setattr(worker, "SessionLocal", create_session(f"sqlite:///{tmp_path / 'worker.db'}"))
    asyncio.run(worker.run())
    return consumer, producer


def transaction_created(transaction_id):
    return json.dumps({
        "eventId": str(uuid4()), "eventType": "transaction.created.v1", "schemaVersion": 1, "correlationId": "corr-worker",
        "occurredAt": "2026-01-15T14:00:00Z", "transactionId": transaction_id, "customerId": str(uuid4()),
        "amount": 25.0, "currency": "BRL", "merchant": "Store",
    }).encode()


def test_result_is_published_as_json_value_keyed_by_transaction_id(monkeypatch, tmp_path):
    transaction_id = str(uuid4())
    message = SimpleNamespace(key=transaction_id.encode(), value=transaction_created(transaction_id))

    consumer, producer = run_worker(monkeypatch, tmp_path, [message, message])

    assert len(producer.sent) == 1, "duplicate delivery must not publish a second result"
    sent = producer.sent[0]
    assert sent["topic"] == "fraud.analysis.completed.v1"
    assert sent["key"] == transaction_id.encode()
    payload = json.loads(sent["value"])
    assert payload["eventType"] == "fraud.analysis.completed.v1"
    assert payload["transactionId"] == transaction_id
    assert payload["correlationId"] == "corr-worker"
    assert consumer.commits == 2


def test_invalid_event_goes_to_dlq_with_error_and_original_payload(monkeypatch, tmp_path):
    message = SimpleNamespace(key=b"tx-key", value=b'{"eventType":"transaction.created.v1"}')

    _, producer = run_worker(monkeypatch, tmp_path, [message])

    assert [m["topic"] for m in producer.sent] == ["fraud.analysis.dlq.v1"]
    assert producer.sent[0]["key"] == b"tx-key"
    dlq = json.loads(producer.sent[0]["value"])
    assert dlq["payload"] == message.value.decode() and dlq["error"]
