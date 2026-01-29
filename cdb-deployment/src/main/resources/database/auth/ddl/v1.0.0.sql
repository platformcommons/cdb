-- create database
CREATE DATABASE IF NOT EXISTS cdb_auth_registry_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE cdb_auth_registry_db;

-- ============================================================================
-- Schema: cdb_auth_registry_db (Auth Registry)
-- This DDL is derived from JPA entities in cdb-auth-registry and BaseEntity.
-- MySQL 8.x compatible.
-- ============================================================================

-- Helper: common audit columns used by entities extending BaseEntity
--   created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
--   updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
--   created_by BIGINT DEFAULT 0
--   updated_by BIGINT DEFAULT 0
--   created_by_provider BIGINT DEFAULT 0
--   updated_by_provider BIGINT DEFAULT 0
--   is_active BOOLEAN NOT NULL DEFAULT 1

-- ============================================================================
-- 1) users (does not extend BaseEntity)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
                                     id BIGINT NOT NULL AUTO_INCREMENT,
                                     username VARCHAR(250) NOT NULL,
    email VARCHAR(250) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL,
    mfa_enabled BOOLEAN NOT NULL,
    last_login DATETIME(6) NULL,
    -- audit columns
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- 2) authority_master (extends BaseEntity)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS authority_master (
                                                id BIGINT NOT NULL AUTO_INCREMENT,
                                                name VARCHAR(150) NOT NULL,
    code VARCHAR(100) NOT NULL,
    process_area VARCHAR(150) NULL,
    -- audit columns
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT DEFAULT 0,
    updated_by BIGINT DEFAULT 0,
    created_by_provider BIGINT DEFAULT 0,
    updated_by_provider BIGINT DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT 1,
    CONSTRAINT pk_authority_master PRIMARY KEY (id),
    CONSTRAINT uk_authority_master_code UNIQUE (code)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- 3) role_master (extends BaseEntity)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS role_master (
                                           id BIGINT NOT NULL AUTO_INCREMENT,
                                           code VARCHAR(100) NOT NULL,
    label VARCHAR(150) NOT NULL,
    type VARCHAR(100) NULL,
    -- audit columns
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT DEFAULT 0,
    updated_by BIGINT DEFAULT 0,
    created_by_provider BIGINT DEFAULT 0,
    updated_by_provider BIGINT DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT 1,
    CONSTRAINT pk_role_master PRIMARY KEY (id),
    CONSTRAINT uk_role_master_code UNIQUE (code)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- 4) role_authorities (join table RoleMaster <-> AuthorityMaster)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS role_authorities (
                                                role_id BIGINT NOT NULL,
                                                authority_id BIGINT NOT NULL,
                                                CONSTRAINT pk_role_authorities PRIMARY KEY (role_id, authority_id),
    CONSTRAINT fk_role_authorities_role FOREIGN KEY (role_id) REFERENCES role_master(id) ON DELETE CASCADE,
    CONSTRAINT fk_role_authorities_auth FOREIGN KEY (authority_id) REFERENCES authority_master(id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- 5) oauth2_clients (extends BaseEntity)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS oauth2_clients (
                                              id BIGINT NOT NULL AUTO_INCREMENT,
                                              client_id VARCHAR(100) NOT NULL,
    client_secret VARCHAR(255) NULL,
    client_name VARCHAR(200) NOT NULL,
    require_pkce BOOLEAN NOT NULL,
    require_consent BOOLEAN NOT NULL,
    logo_url VARCHAR(500) NULL,
    description VARCHAR(1000) NULL,
    -- audit columns
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT DEFAULT 0,
    updated_by BIGINT DEFAULT 0,
    created_by_provider BIGINT DEFAULT 0,
    updated_by_provider BIGINT DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT 1,
    CONSTRAINT pk_oauth2_clients PRIMARY KEY (id),
    CONSTRAINT uk_oauth2_clients_client_id UNIQUE (client_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- oauth2 client element collections (each references oauth2_clients.id via client_id)
CREATE TABLE IF NOT EXISTS oauth2_client_redirect_uris (
                                                           client_id BIGINT NOT NULL,
                                                           redirect_uri VARCHAR(500) NOT NULL,
    CONSTRAINT pk_oauth2_client_redirect_uris PRIMARY KEY (client_id, redirect_uri),
    CONSTRAINT fk_oauth2_client_redirect_uris_client FOREIGN KEY (client_id) REFERENCES oauth2_clients(id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS oauth2_client_scopes (
                                                    client_id BIGINT NOT NULL,
                                                    scope VARCHAR(100) NOT NULL,
    CONSTRAINT pk_oauth2_client_scopes PRIMARY KEY (client_id, scope),
    CONSTRAINT fk_oauth2_client_scopes_client FOREIGN KEY (client_id) REFERENCES oauth2_clients(id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS oauth2_client_grant_types (
                                                         client_id BIGINT NOT NULL,
                                                         grant_type VARCHAR(50) NOT NULL,
    CONSTRAINT pk_oauth2_client_grant_types PRIMARY KEY (client_id, grant_type),
    CONSTRAINT fk_oauth2_client_grant_types_client FOREIGN KEY (client_id) REFERENCES oauth2_clients(id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- 6) oauth2_authorization_codes (extends BaseEntity)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS oauth2_authorization_codes (
                                                          id BIGINT NOT NULL AUTO_INCREMENT,
                                                          code VARCHAR(255) NOT NULL,
    client_id VARCHAR(100) NOT NULL,
    user_id BIGINT NOT NULL,
    redirect_uri VARCHAR(500) NOT NULL,
    scope VARCHAR(500) NULL,
    code_challenge VARCHAR(255) NULL,
    code_challenge_method VARCHAR(10) NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    used BOOLEAN NOT NULL,
    -- audit columns
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT DEFAULT 0,
    updated_by BIGINT DEFAULT 0,
    created_by_provider BIGINT DEFAULT 0,
    updated_by_provider BIGINT DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT 1,
    CONSTRAINT pk_oauth2_authorization_codes PRIMARY KEY (id),
    CONSTRAINT uk_oauth2_authorization_codes_code UNIQUE (code),
    CONSTRAINT fk_oauth2_auth_codes_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    -- Note: client_id is the public string identifier, not FK to oauth2_clients.id
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- 7) user_provider_mapping (extends BaseEntity)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_provider_mapping (
                                                     id BIGINT NOT NULL AUTO_INCREMENT,
                                                     user_id BIGINT NOT NULL,
                                                     provider_id BIGINT NULL,
                                                     provider_code VARCHAR(100) NOT NULL,
    mapped_at TIMESTAMP(6) NOT NULL,
    status VARCHAR(20) NOT NULL,
    -- audit columns
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT DEFAULT 0,
    updated_by BIGINT DEFAULT 0,
    created_by_provider BIGINT DEFAULT 0,
    updated_by_provider BIGINT DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT 1,
    CONSTRAINT pk_user_provider_mapping PRIMARY KEY (id),
    CONSTRAINT fk_user_provider_mapping_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_upm_user_provider (user_id, provider_code, status)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Join table for user_provider_mapping -> role_master (Set<RoleMaster>)
CREATE TABLE IF NOT EXISTS user_provider_role_map (
                                                      user_provider_mapping_id BIGINT NOT NULL,
                                                      role_master_id BIGINT NOT NULL,
                                                      CONSTRAINT pk_user_provider_role_map PRIMARY KEY (user_provider_mapping_id, role_master_id),
    CONSTRAINT fk_uprm_upm FOREIGN KEY (user_provider_mapping_id) REFERENCES user_provider_mapping(id) ON DELETE CASCADE,
    CONSTRAINT fk_uprm_role FOREIGN KEY (role_master_id) REFERENCES role_master(id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
