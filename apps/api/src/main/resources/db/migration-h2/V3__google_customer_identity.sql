ALTER TABLE customer ALTER COLUMN birth_date DROP NOT NULL;
ALTER TABLE customer ALTER COLUMN region_sido DROP NOT NULL;
ALTER TABLE customer ALTER COLUMN region_sigungu DROP NOT NULL;
ALTER TABLE customer ALTER COLUMN occupation DROP NOT NULL;
ALTER TABLE customer ALTER COLUMN income_band DROP NOT NULL;
ALTER TABLE customer ALTER COLUMN member_since DROP NOT NULL;

CREATE TABLE oauth_identity (
    id                  VARCHAR(64)  PRIMARY KEY,
    provider            VARCHAR(30)  NOT NULL,
    provider_subject    VARCHAR(255) NOT NULL,
    customer_id         VARCHAR(64)  NOT NULL,
    email               VARCHAR(320) NULL,
    email_verified      BOOLEAN      NULL,
    picture_url         VARCHAR(1000) NULL,
    last_login_at       TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at          TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_oauth_identity_customer FOREIGN KEY (customer_id) REFERENCES customer(id),
    CONSTRAINT uq_oauth_identity_provider_subject UNIQUE (provider, provider_subject),
    CONSTRAINT uq_oauth_identity_customer_provider UNIQUE (customer_id, provider)
);
