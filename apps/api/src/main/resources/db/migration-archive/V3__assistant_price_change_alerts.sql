ALTER TABLE outbox_event ADD COLUMN deduplication_key VARCHAR(180) NULL;
CREATE UNIQUE INDEX uq_outbox_deduplication_key ON outbox_event(deduplication_key);

DROP TABLE auth_challenge;
DROP TABLE payment_pause;
DROP TABLE saga_step;
DROP TABLE saga_instance;
DROP TABLE action_plan;
DROP TABLE subscription_wallet;

UPDATE subscription SET status = 'ACTIVE' WHERE status <> 'ACTIVE';

CREATE TABLE assistant_conversation (
    id VARCHAR(64) PRIMARY KEY,
    customer_id VARCHAR(64) NOT NULL UNIQUE,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL
);

CREATE TABLE assistant_message (
    id VARCHAR(64) PRIMARY KEY,
    conversation_id VARCHAR(64) NOT NULL,
    message_role VARCHAR(20) NOT NULL,
    source VARCHAR(32) NOT NULL,
    content VARCHAR(4000) NOT NULL,
    source_key VARCHAR(180) NULL UNIQUE,
    created_at TIMESTAMP(6) NOT NULL,
    read_at TIMESTAMP(6) NULL,
    CONSTRAINT fk_assistant_message_conversation
        FOREIGN KEY (conversation_id) REFERENCES assistant_conversation(id)
);
CREATE INDEX idx_assistant_message_conversation_created
    ON assistant_message(conversation_id, created_at);
CREATE INDEX idx_assistant_message_unread
    ON assistant_message(conversation_id, message_role, read_at);

CREATE TABLE subscription_price_observation (
    id VARCHAR(64) PRIMARY KEY,
    event_id VARCHAR(100) NOT NULL UNIQUE,
    subscription_id VARCHAR(64) NOT NULL,
    previous_amount DECIMAL(15, 2) NOT NULL,
    observed_amount DECIMAL(15, 2) NOT NULL,
    observed_at TIMESTAMP(6) NOT NULL,
    applied BOOLEAN NOT NULL,
    price_increased BOOLEAN NOT NULL,
    CONSTRAINT fk_price_observation_subscription
        FOREIGN KEY (subscription_id) REFERENCES subscription(id)
);
CREATE INDEX idx_price_observation_subscription_date
    ON subscription_price_observation(subscription_id, applied, observed_at);
