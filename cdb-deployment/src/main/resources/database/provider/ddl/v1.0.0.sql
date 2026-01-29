CREATE DATABASE cdb_provider_registry_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE cdb_provider_registry_db;

-- Providers table
CREATE TABLE cdb_providers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(2048),
    status VARCHAR(255) NOT NULL DEFAULT 'PENDING',
    owner_user_id BIGINT,
    contact_email VARCHAR(255),
    contact_phone VARCHAR(255),
    tags VARCHAR(1000),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,
    created_by BIGINT DEFAULT 0,
    updated_by BIGINT DEFAULT 0,
    created_by_provider BIGINT DEFAULT 0,
    updated_by_provider BIGINT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE NOT NULL
);

-- Provider requests table
CREATE TABLE cdb_user_provider_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    user_email VARCHAR(255) NOT NULL,
    provider_id BIGINT NOT NULL,
    provider_code VARCHAR(255) NOT NULL,
    request_message VARCHAR(1000),
    status VARCHAR(255) NOT NULL DEFAULT 'PENDING',
    approved_by BIGINT,
    approval_notes VARCHAR(1000),
    requested_role VARCHAR(255) DEFAULT 'USER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,
    created_by BIGINT DEFAULT 0,
    updated_by BIGINT DEFAULT 0,
    created_by_provider BIGINT DEFAULT 0,
    updated_by_provider BIGINT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE NOT NULL
);

-- Provider keys table
CREATE TABLE cdb_provider_keys (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    key_id VARCHAR(64) NOT NULL UNIQUE,
    key_type ENUM('ENCRYPTION', 'SIGNING') NOT NULL,
    key_status ENUM('ACTIVE', 'NOT_ACTIVE') NOT NULL,
    environment ENUM('PRODUCTION', 'SANDBOX') NOT NULL DEFAULT 'SANDBOX',
    provider_id BIGINT NOT NULL,
    title VARCHAR(256),
    public_key_pem TEXT NOT NULL,
    private_key_checksum VARCHAR(128) NOT NULL,
    issued_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP,
    scopes VARCHAR(1024),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,
    created_by BIGINT DEFAULT 0,
    updated_by BIGINT DEFAULT 0,
    created_by_provider BIGINT DEFAULT 0,
    updated_by_provider BIGINT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE NOT NULL,
    FOREIGN KEY (provider_id) REFERENCES cdb_providers(id),
    INDEX idx_providerkey_provider (provider_id),
    INDEX idx_providerkey_keyid (key_id),
    INDEX idx_providerkey_type_status (provider_id, key_type, key_status)
);

-- Provider configuration table
CREATE TABLE provider_configuration (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_code VARCHAR(255) NOT NULL,
    config_label VARCHAR(255) NOT NULL,
    config_value VARCHAR(255),
    status ENUM('ACTIVE', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    visibility ENUM('PUBLIC', 'PRIVATE') NOT NULL DEFAULT 'PRIVATE',
    config_data_type ENUM('STRING', 'NUMBER', 'DOUBLE', 'BOOLEAN') NOT NULL DEFAULT 'STRING',
    has_list BOOLEAN NOT NULL DEFAULT FALSE,
    provider_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,
    created_by BIGINT DEFAULT 0,
    updated_by BIGINT DEFAULT 0,
    created_by_provider BIGINT DEFAULT 0,
    updated_by_provider BIGINT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE NOT NULL,
    FOREIGN KEY (provider_id) REFERENCES cdb_providers(id)
);

-- Provider configuration data table
CREATE TABLE provider_config_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_value VARCHAR(255) NOT NULL,
    config_value_sequence INT NOT NULL,
    provider_configuration_id BIGINT,
    FOREIGN KEY (provider_configuration_id) REFERENCES provider_configuration(id)
);

-- Provider environment table
CREATE TABLE provider_environment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    provider_id BIGINT,
    environment_type ENUM('PRODUCTION', 'SANDBOX'),
    base_url VARCHAR(255),
    uptime_status VARCHAR(255),
    rate_limit INT,
    remarks VARCHAR(1000),
    FOREIGN KEY (provider_id) REFERENCES cdb_providers(id)
);
-- Add client_id and private_key_pem columns to provider keys table
ALTER TABLE cdb_provider_keys
    ADD COLUMN client_id VARCHAR(100) NOT NULL DEFAULT 'default',
ADD COLUMN private_key_pem TEXT;

-- Create new unique index including client_id
CREATE INDEX idx_providerkey_unique_active ON cdb_provider_keys (provider_id, key_type, key_status, environment, client_id);