CREATE TABLE request_log (
  idempotency_key  VARCHAR(50) PRIMARY KEY, 
  request_state    VARCHAR(1), 
  service_type     VARCHAR(100), 
  request_payload  VARCHAR(100), 
  response_payload VARCHAR(100),
  version          INT
);

CREATE TABLE post (
  post_id VARCHAR(50) PRIMARY KEY,
  author  VARCHAR(100),
  message VARCHAR(100),
  version INT
);