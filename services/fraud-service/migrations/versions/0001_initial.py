from alembic import op
import sqlalchemy as sa

revision = "0001_initial"
down_revision = None


def upgrade():
    op.create_table("processed_events", sa.Column("event_id", sa.String(36), primary_key=True), sa.Column("processed_at", sa.DateTime(timezone=True), nullable=False))
    op.create_table("fraud_analyses", sa.Column("transaction_id", sa.String(36), primary_key=True), sa.Column("event_id", sa.String(36), nullable=False, unique=True), sa.Column("risk_score", sa.Numeric(5, 4), nullable=False), sa.Column("decision", sa.String(20), nullable=False), sa.Column("reasons", sa.JSON(), nullable=False), sa.Column("model_version", sa.String(80), nullable=False), sa.Column("correlation_id", sa.String(100), nullable=False), sa.Column("created_at", sa.DateTime(timezone=True), nullable=False))

def downgrade():
    op.drop_table("fraud_analyses")
    op.drop_table("processed_events")
