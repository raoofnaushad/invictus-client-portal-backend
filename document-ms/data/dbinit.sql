CREATE TABLE IF NOT EXISTS document_table (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255),
    description VARCHAR(1000),
    file_path VARCHAR(500),
    document_type VARCHAR(100),
    document_status VARCHAR(100),
    client_id VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    extracted_data CLOB
);