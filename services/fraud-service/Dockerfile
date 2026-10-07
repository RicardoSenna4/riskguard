FROM python:3.12-slim
WORKDIR /app
ENV PYTHONPATH=/app/src
COPY pyproject.toml README.md ./
COPY src ./src
COPY migrations ./migrations
COPY alembic.ini ./
RUN pip install --no-cache-dir .
EXPOSE 8090
# Apply Alembic migrations before serving; the Kafka worker reuses this image with its own command.
CMD ["sh", "-c", "alembic upgrade head && exec uvicorn riskguard_fraud.app:app --app-dir src --host 0.0.0.0 --port 8090"]
