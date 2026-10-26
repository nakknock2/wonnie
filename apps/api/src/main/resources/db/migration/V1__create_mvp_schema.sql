CREATE TABLE subscription (
    id VARCHAR(64) PRIMARY KEY,
    customer_id VARCHAR(64) NOT NULL,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    billing_cycle VARCHAR(30) NOT NULL,
    next_payment_date DATE NOT NULL,
    status VARCHAR(40) NOT NULL,
    brand_color VARCHAR(20) NOT NULL,
    initial_text VARCHAR(4) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE subscription_wallet (
    id VARCHAR(64) PRIMARY KEY,
    subscription_id VARCHAR(64) NOT NULL UNIQUE,
    balance DECIMAL(15, 2) NOT NULL,
    auto_funding_enabled BOOLEAN NOT NULL,
    status VARCHAR(30) NOT NULL,
    virtual_card_last4 VARCHAR(4) NOT NULL,
    virtual_card_status VARCHAR(30) NOT NULL,
    monthly_limit DECIMAL(15, 2) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_wallet_subscription FOREIGN KEY (subscription_id) REFERENCES subscription(id)
);

CREATE TABLE calendar_signal (
    id VARCHAR(64) PRIMARY KEY,
    customer_id VARCHAR(64) NOT NULL,
    title VARCHAR(200) NOT NULL,
    starts_on DATE NOT NULL,
    ends_on DATE NOT NULL,
    signal_type VARCHAR(40) NOT NULL,
    confidence DECIMAL(5, 4) NOT NULL
);

CREATE TABLE action_plan (
    id VARCHAR(64) PRIMARY KEY,
    customer_id VARCHAR(64) NOT NULL,
    subscription_id VARCHAR(64) NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    starts_on DATE NOT NULL,
    ends_on DATE NOT NULL,
    reason VARCHAR(500) NOT NULL,
    warning VARCHAR(500) NOT NULL,
    status VARCHAR(40) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_plan_subscription FOREIGN KEY (subscription_id) REFERENCES subscription(id)
);

CREATE TABLE auth_challenge (
    id VARCHAR(64) PRIMARY KEY,
    action_plan_id VARCHAR(64) NOT NULL,
    challenge_value VARCHAR(128) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    verified_at TIMESTAMP NULL,
    CONSTRAINT fk_auth_plan FOREIGN KEY (action_plan_id) REFERENCES action_plan(id)
);

CREATE TABLE payment_pause (
    id VARCHAR(64) PRIMARY KEY,
    subscription_id VARCHAR(64) NOT NULL,
    action_plan_id VARCHAR(64) NOT NULL,
    saga_id VARCHAR(64) NOT NULL,
    starts_at DATE NOT NULL,
    ends_at DATE NOT NULL,
    status VARCHAR(40) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_pause_subscription FOREIGN KEY (subscription_id) REFERENCES subscription(id),
    CONSTRAINT fk_pause_plan FOREIGN KEY (action_plan_id) REFERENCES action_plan(id)
);

CREATE TABLE saga_instance (
    id VARCHAR(64) PRIMARY KEY,
    saga_type VARCHAR(60) NOT NULL,
    aggregate_id VARCHAR(64) NOT NULL,
    current_state VARCHAR(60) NOT NULL,
    status VARCHAR(30) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    started_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP NULL,
    failed_reason VARCHAR(1000) NULL
);

CREATE TABLE saga_step (
    id VARCHAR(64) PRIMARY KEY,
    saga_id VARCHAR(64) NOT NULL,
    step_type VARCHAR(60) NOT NULL,
    status VARCHAR(30) NOT NULL,
    attempt_count INT NOT NULL,
    idempotency_key VARCHAR(160) NOT NULL UNIQUE,
    compensation_status VARCHAR(30) NOT NULL,
    executed_at TIMESTAMP NULL,
    last_error VARCHAR(1000) NULL,
    CONSTRAINT fk_step_saga FOREIGN KEY (saga_id) REFERENCES saga_instance(id)
);

CREATE TABLE outbox_event (
    id VARCHAR(64) PRIMARY KEY,
    aggregate_type VARCHAR(60) NOT NULL,
    aggregate_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    published_at TIMESTAMP NULL
);

CREATE INDEX idx_outbox_status_created ON outbox_event(status, created_at);
CREATE INDEX idx_subscription_customer ON subscription(customer_id);
CREATE INDEX idx_signal_customer_dates ON calendar_signal(customer_id, starts_on, ends_on);
