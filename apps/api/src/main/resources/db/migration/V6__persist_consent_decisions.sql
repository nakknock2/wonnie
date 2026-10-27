ALTER TABLE consent ADD COLUMN decided_at DATETIME(6) NULL;

UPDATE consent
SET decided_at = agreed_at
WHERE agreed = TRUE AND agreed_at IS NOT NULL;

INSERT INTO consent (id, customer_id, consent_type, required, agreed)
SELECT CONCAT('cons-push-price-', customer.id), customer.id, 'PUSH_PRICE_CHANGE', FALSE, FALSE
FROM customer
WHERE NOT EXISTS (
    SELECT 1
    FROM consent
    WHERE consent.customer_id = customer.id
      AND consent.consent_type = 'PUSH_PRICE_CHANGE'
);
