-- Google OIDC identities are kept separate from the financial customer profile.
-- Google does not provide the profile fields below, so newly provisioned customers
-- leave them unknown until a later onboarding flow collects them.
ALTER TABLE customer
    MODIFY birth_date DATE NULL,
    MODIFY region_sido VARCHAR(20) NULL,
    MODIFY region_sigungu VARCHAR(30) NULL,
    MODIFY occupation VARCHAR(30) NULL,
    MODIFY income_band VARCHAR(20) NULL,
    MODIFY member_since DATE NULL;

CREATE TABLE oauth_identity (
    id                  VARCHAR(64)  PRIMARY KEY,
    provider            VARCHAR(30)  NOT NULL,
    provider_subject    VARCHAR(255) NOT NULL,
    customer_id         VARCHAR(64)  NOT NULL,
    email               VARCHAR(320) NULL,
    email_verified      BOOLEAN      NULL,
    picture_url         VARCHAR(1000) NULL,
    last_login_at       DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_at          DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_oauth_identity_customer FOREIGN KEY (customer_id) REFERENCES customer(id),
    CONSTRAINT uq_oauth_identity_provider_subject UNIQUE (provider, provider_subject),
    CONSTRAINT uq_oauth_identity_customer_provider UNIQUE (customer_id, provider)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
