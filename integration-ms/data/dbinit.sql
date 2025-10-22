CREATE TABLE IF NOT EXISTS integration_table (
    id CHARACTER VARYING(255) NOT NULL,
    access_token CHARACTER VARYING(2000),
    principal_id CHARACTER VARYING(255) NOT NULL,
    products CHARACTER LARGE OBJECT,
    updated_at TIMESTAMP,
    created_at TIMESTAMP
);

commit;

