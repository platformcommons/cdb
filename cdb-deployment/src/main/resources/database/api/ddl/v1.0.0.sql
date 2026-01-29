-- ============================================
-- Database
-- ============================================
CREATE DATABASE IF NOT EXISTS cdb_api_registry_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE cdb_api_registry_db;

-- ============================================
-- Main API Table
-- ============================================
CREATE TABLE cdb_api (
                         id BIGINT AUTO_INCREMENT PRIMARY KEY,
                         name VARCHAR(255),
                         owner VARCHAR(255),
                         description TEXT,
                         detailed_description LONGTEXT,
                         base_path VARCHAR(255),
                         version VARCHAR(255),
                         status VARCHAR(255),
                         open_api_spec LONGTEXT,
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                         updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,
                         created_by BIGINT DEFAULT 0,
                         updated_by BIGINT DEFAULT 0,
                         created_by_provider BIGINT DEFAULT 0,
                         updated_by_provider BIGINT DEFAULT 0,
                         is_active BOOLEAN DEFAULT TRUE NOT NULL
);

-- Indexes for performance
CREATE INDEX idx_api_name ON cdb_api(name);
CREATE INDEX idx_api_owner ON cdb_api(owner);
CREATE INDEX idx_api_status ON cdb_api(status);
CREATE INDEX idx_api_version ON cdb_api(version);
CREATE INDEX idx_api_active ON cdb_api(is_active);

-- ============================================
-- API Tags Table (for @ElementCollection)
-- ============================================
CREATE TABLE cdb_api_tags (
                              api_id BIGINT NOT NULL,
                              tags VARCHAR(255) NOT NULL,
                              PRIMARY KEY (api_id, tags),
                              FOREIGN KEY (api_id) REFERENCES cdb_api(id) ON DELETE CASCADE
);

-- Optional index for searching by tag
CREATE INDEX idx_api_tags_tag ON cdb_api_tags(tags);

-- ============================================
-- API Audit Log Table
-- ============================================
CREATE TABLE cdb_api_audit_log (
                                   id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                   api_id BIGINT NOT NULL,
                                   version VARCHAR(64),
                                   action VARCHAR(64),
                                   changed_by VARCHAR(128),
                                   changed_at TIMESTAMP NOT NULL,
                                   old_values_json LONGTEXT,
                                   new_values_json LONGTEXT,
                                   change_description VARCHAR(1024),
                                   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                   updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,
                                   created_by BIGINT DEFAULT 0,
                                   updated_by BIGINT DEFAULT 0,
                                   created_by_provider BIGINT DEFAULT 0,
                                   updated_by_provider BIGINT DEFAULT 0,
                                   is_active BOOLEAN DEFAULT TRUE NOT NULL,
                                   FOREIGN KEY (api_id) REFERENCES cdb_api(id)
);

-- Indexes for audit table
CREATE INDEX idx_audit_api_id ON cdb_api_audit_log(api_id);
CREATE INDEX idx_audit_changed_at ON cdb_api_audit_log(changed_at);
CREATE INDEX idx_audit_action ON cdb_api_audit_log(action);
CREATE INDEX idx_audit_active ON cdb_api_audit_log(is_active);
