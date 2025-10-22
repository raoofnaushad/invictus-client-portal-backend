CREATE TABLE IF NOT EXISTS user_credential_table (
    id VARCHAR(255) PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    phone_number VARCHAR(255),
    iam_user_id VARCHAR(255) NOT NULL,
    activation_token VARCHAR(255),
    activation_token_expiry TIMESTAMP,
    refresh_token VARCHAR(255),
    totp_secret VARCHAR(255),
    status VARCHAR(50),
    mfa_activated BOOLEAN,
    mfa_channel VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS principal_table (
    id VARCHAR(255) PRIMARY KEY,
    alias VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    user_credential_id VARCHAR(255),
    FOREIGN KEY (user_credential_id) REFERENCES user_credential_table(id)
);

commit;