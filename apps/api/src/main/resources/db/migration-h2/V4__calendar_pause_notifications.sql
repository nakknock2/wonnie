ALTER TABLE chat_message ADD COLUMN metadata VARCHAR(8000) NULL;
ALTER TABLE chat_message DROP CONSTRAINT ck_chat_source;
ALTER TABLE chat_message ADD CONSTRAINT ck_chat_source CHECK (
    (role = 'USER' AND source = 'USER_INPUT') OR
    (role = 'ASSISTANT' AND source IN ('ASSISTANT_REPLY','PRICE_INCREASE','FINANCE_NOTICE','CALENDAR_PAUSE'))
);

CREATE TABLE oauth2_authorized_client (
    client_registration_id VARCHAR(100) NOT NULL,
    principal_name VARCHAR(200) NOT NULL,
    access_token_type VARCHAR(100) NOT NULL,
    access_token_value BLOB NOT NULL,
    access_token_issued_at TIMESTAMP NOT NULL,
    access_token_expires_at TIMESTAMP NOT NULL,
    access_token_scopes VARCHAR(1000) DEFAULT NULL,
    refresh_token_value BLOB DEFAULT NULL,
    refresh_token_issued_at TIMESTAMP DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    PRIMARY KEY (client_registration_id, principal_name)
);

CREATE TABLE calendar_sync_state (
    customer_id VARCHAR(64) PRIMARY KEY,
    principal_name VARCHAR(200) NOT NULL,
    channel_id VARCHAR(100) NULL UNIQUE,
    channel_token VARCHAR(200) NULL,
    resource_id VARCHAR(255) NULL,
    watch_expires_at TIMESTAMP(6) NULL,
    last_synced_at TIMESTAMP(6) NULL,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_calendar_sync_customer FOREIGN KEY (customer_id) REFERENCES customer(id)
);
