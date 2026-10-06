ALTER TABLE cod_query ADD COLUMN status varchar(16);
UPDATE cod_query SET status = CASE WHEN completed THEN 'COMPLETED' ELSE 'FAILED' END;
ALTER TABLE cod_query ALTER COLUMN status SET NOT NULL;
ALTER TABLE cod_query ADD CONSTRAINT cod_query_status_check
    CHECK (status IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED'));
