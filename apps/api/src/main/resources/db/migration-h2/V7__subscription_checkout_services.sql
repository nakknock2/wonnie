-- Extend checkout without changing existing customer subscriptions or card benefits.
INSERT INTO service (id,name,category,billing_channel,typical_price,has_trial,trial_days,
                     usage_merchant_group,rag_doc_key,brand_color,initial_text)
SELECT 'svc-spotify','스포티파이','MUSIC','WEB',11990,FALSE,NULL,NULL,'SVC-SPOTIFY','#1db954','S'
WHERE NOT EXISTS (SELECT 1 FROM service WHERE id='svc-spotify');

UPDATE service SET name='ChatGPT',typical_price=29000 WHERE id='svc-chatgpt';

INSERT INTO merchant (id,raw_name,category,service_id,is_aggregator,merchant_group)
SELECT 'mch-spotify','SPOTIFY','SUBSCRIPTION','svc-spotify',FALSE,NULL
WHERE NOT EXISTS (SELECT 1 FROM merchant WHERE id='mch-spotify');

INSERT INTO merchant (id,raw_name,category,service_id,is_aggregator,merchant_group)
SELECT 'mch-chatgpt','OPENAI*CHATGPT','SUBSCRIPTION','svc-chatgpt',FALSE,NULL
WHERE NOT EXISTS (SELECT 1 FROM merchant WHERE id='mch-chatgpt');
