UPDATE card_product
SET issuer_name = '우리카드'
WHERE id IN ('prd-routine', 'prd-daily');

UPDATE card_product SET annual_fee = 28000 WHERE id = 'prd-routine';

INSERT INTO card_product (id,name,card_type,annual_fee,summary,rag_doc_key,issuer_name)
VALUES ('prd-jeongseok2','카드의정석2','CREDIT',22000,
        '국내외 가맹점 1.2% 청구할인과 분기별 이용실적 추가 할인','CARD-JEONGSEOK2','우리카드');

INSERT INTO card_product_benefit
    (id,card_product_id,benefit_area,service_id,discount_rate,monthly_cap,min_prev_month_spend,note)
VALUES ('ben-jeongseok2-netflix','prd-jeongseok2','ALL','svc-netflix',1.2,36000,500000,
        '국내외 가맹점 기본 할인 · 월 이용금액별 할인한도는 실제 약관 확인 필요');
