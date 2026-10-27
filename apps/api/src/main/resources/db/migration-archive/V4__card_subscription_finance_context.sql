CREATE TABLE finance_customer (
    id VARCHAR(64) PRIMARY KEY,
    birth_date DATE,
    region VARCHAR(500),
    employment VARCHAR(500),
    monthly_income DECIMAL(15,4),
    account_balance DECIMAL(15,4),
    preferred_notice_hour INT
);

CREATE TABLE finance_card (
    id VARCHAR(64) PRIMARY KEY,
    customer_id VARCHAR(64),
    product_name VARCHAR(500),
    issuer VARCHAR(500),
    owned BOOLEAN,
    benefit_category VARCHAR(500),
    previous_month_spend DECIMAL(15,4),
    minimum_spend DECIMAL(15,4),
    discount_rate DECIMAL(15,4),
    monthly_cap DECIMAL(15,4),
    annual_fee DECIMAL(15,4),
    available_limit DECIMAL(15,4),
    outstanding DECIMAL(15,4),
    revolving_balance DECIMAL(15,4),
    revolving_apr DECIMAL(15,4),
    card_status VARCHAR(500),
    expires_on DATE,
    payment_on DATE,
    points_balance DECIMAL(15,4),
    points_expire_on DATE,
    annual_fee_on DATE,
    minimum_age INT,
    maximum_age INT,
    required_region VARCHAR(500),
    required_employment VARCHAR(500)
);

CREATE TABLE finance_transaction (
    id VARCHAR(64) PRIMARY KEY,
    customer_id VARCHAR(64),
    card_id VARCHAR(64),
    subscription_id VARCHAR(64),
    merchant_label VARCHAR(500),
    category VARCHAR(500),
    amount DECIMAL(15,4),
    original_amount DECIMAL(15,4),
    currency VARCHAR(500),
    exchange_rate DECIMAL(15,4),
    authorized_at TIMESTAMP(6),
    transaction_status VARCHAR(500),
    decline_reason VARCHAR(500)
);

CREATE TABLE subscription_context (
    id VARCHAR(64) PRIMARY KEY,
    card_id VARCHAR(64),
    service_family VARCHAR(500),
    plan_name VARCHAR(500),
    trial_ends_on DATE,
    annual_renewal_on DATE,
    contract_status VARCHAR(500),
    contract_verified_on DATE,
    cancelled_on DATE,
    usage_count30_days INT,
    usage_evidence VARCHAR(500),
    bundle_description VARCHAR(500),
    family_covered BOOLEAN,
    youth_plan_amount DECIMAL(15,4),
    pay_per_use_amount DECIMAL(15,4)
);

CREATE TABLE merchant_directory (
    id VARCHAR(64) PRIMARY KEY,
    merchant_label VARCHAR(500),
    display_name VARCHAR(500),
    possible_services VARCHAR(500),
    ambiguous BOOLEAN,
    verification_hint VARCHAR(500)
);

CREATE TABLE finance_alternative (
    id VARCHAR(64) PRIMARY KEY,
    category VARCHAR(500),
    region VARCHAR(500),
    name VARCHAR(500),
    monthly_cost DECIMAL(15,4),
    conditions VARCHAR(500)
);

CREATE INDEX idx_finance_transaction_customer_date ON finance_transaction(customer_id, authorized_at);

CREATE INDEX idx_finance_card_customer ON finance_card(customer_id);

CREATE TABLE finance_cohort_benchmark (
    id VARCHAR(64) PRIMARY KEY,
    minimum_age INT NOT NULL,
    maximum_age INT NOT NULL,
    region VARCHAR(500) NOT NULL,
    monthly_subscription_average DECIMAL(15,4) NOT NULL,
    sample_count INT NOT NULL
);

ALTER TABLE subscription_price_observation ADD COLUMN currency VARCHAR(255) NULL;
ALTER TABLE subscription_price_observation ADD COLUMN original_amount DECIMAL(15,2) NULL;
ALTER TABLE subscription_price_observation ADD COLUMN exchange_rate DECIMAL(15,6) NULL;
ALTER TABLE subscription_price_observation ADD COLUMN change_reason VARCHAR(255) NULL;
