INSERT INTO subscription (
    id, customer_id, name, category, amount, billing_cycle, next_payment_date,
    status, brand_color, initial_text, created_at, updated_at
) VALUES
    ('sub-netflix', 'customer-demo', 'Netflix', '영상', 17000, '매월', '2026-11-02', 'ACTIVE', '#e50914', 'N', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('sub-youtube', 'customer-demo', 'YouTube Premium', '음악·영상', 14900, '매월', '2026-11-08', 'ACTIVE', '#ff0033', 'Y', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('sub-icloud', 'customer-demo', 'iCloud+', '클라우드', 4400, '매월', '2026-11-12', 'ACTIVE', '#4c8bf5', 'i', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('sub-millie', 'customer-demo', '밀리의 서재', '도서', 11900, '매월', '2026-11-19', 'ACTIVE', '#ffb800', '밀', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO subscription_wallet (
    id, subscription_id, balance, auto_funding_enabled, status,
    virtual_card_last4, virtual_card_status, monthly_limit, version, updated_at
) VALUES
    ('wallet-netflix', 'sub-netflix', 0, TRUE, 'ACTIVE', '2048', 'ACTIVE', 20000, 0, CURRENT_TIMESTAMP),
    ('wallet-youtube', 'sub-youtube', 0, TRUE, 'ACTIVE', '3812', 'ACTIVE', 18000, 0, CURRENT_TIMESTAMP),
    ('wallet-icloud', 'sub-icloud', 0, TRUE, 'ACTIVE', '7714', 'ACTIVE', 6000, 0, CURRENT_TIMESTAMP),
    ('wallet-millie', 'sub-millie', 0, TRUE, 'ACTIVE', '9320', 'ACTIVE', 15000, 0, CURRENT_TIMESTAMP);

INSERT INTO calendar_signal (
    id, customer_id, title, starts_on, ends_on, signal_type, confidence
) VALUES
    ('signal-exam', 'customer-demo', '중간고사 집중 기간', '2026-11-03', '2026-11-14', 'EXAM_PERIOD', 0.9100);
