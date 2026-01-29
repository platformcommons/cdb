
use cdb_auth_registry_db;

-- MASTER Role
INSERT INTO `cdb_auth_registry_db`.`role_master` (`id`, `created_at`, `is_active`, `updated_at`, `code`, `label`, `type`) VALUES (1, now(), 1, now(), 'PROLE.CDB_PLATFORM_ADMIN', 'CDB Platform Admin', 'SYSTEM');
INSERT INTO `cdb_auth_registry_db`.`role_master` (`id`, `created_at`, `is_active`, `updated_at`, `code`, `label`, `type`) VALUES (2, now(), 1, now(), 'PROLE.PROVIDER_ADMIN', 'Provider Admin', 'SYSTEM');


-- Default user with bcrypt hashed password (round 12)
-- Password: admin123

INSERT INTO `cdb_auth_registry_db`.`users`
(`id`, `username`, `email`, `password_hash`, `enabled`, `mfa_enabled`, `created_at`,`updated_at`)
VALUES (1, 'admin', 'admin@cdb.platformcommons.com',
        '$2a$12$daYeVdhK7HzW7APNta2VCeH6u3x2.oA3kxlua9qKVHiMwVbEXmd.i', -- hashed 'admin123'
        1, 0, now(), now());

-- Map admin user to provider
INSERT INTO `cdb_auth_registry_db`.`user_provider_mapping`
(`id`, `created_at`, `is_active`, `updated_at`, `provider_id`,  `provider_code`,`user_id`,`mapped_at`,`status`)
VALUES (1, now(), 1, now(), 1, 'CDB_PLATFORM', 1,now(),'ACTIVE');

-- Map admin user to provider role
INSERT INTO `cdb_auth_registry_db`.`user_provider_role_map`(`user_provider_mapping_id`,`role_master_id`)VALUES(1,1);



-- Setup OAuth2 client for API Registry
INSERT INTO oauth2_clients (client_id, client_name, description, require_pkce, require_consent) 
VALUES ('cdb_api_registry', 'CDB API Registry', 'Official CDB API Registry application', true, true);

-- Get the client ID for foreign key references
SET @client_pk = (SELECT id FROM oauth2_clients WHERE client_id = 'cdb_api_registry');

-- Add redirect URIs
INSERT IGNORE INTO oauth2_client_redirect_uris (client_id, redirect_uri) VALUES 
(@client_pk, 'http://localhost:8082/auth/callback'),
(@client_pk, 'https://cdb.platformcommons.org/auth/callback');

-- Add scopes
INSERT IGNORE INTO oauth2_client_scopes (client_id, scope) VALUES 
(@client_pk, 'read'),
(@client_pk, 'write');

-- Add grant types
INSERT IGNORE INTO oauth2_client_grant_types (client_id, grant_type) VALUES 
(@client_pk, 'authorization_code');



-- Setup OAuth2 client for API Registry
INSERT INTO oauth2_clients (client_id, client_name, description, require_pkce, require_consent)
VALUES ('cdb_provider_registry', 'CDB Provider Registry', 'Official CDB Provider Registry application', true, true);

-- Get the client ID for foreign key references
SET @client_pk = (SELECT id FROM oauth2_clients WHERE client_id = 'cdb_provider_registry');

-- Add redirect URIs
INSERT IGNORE INTO oauth2_client_redirect_uris (client_id, redirect_uri) VALUES
                                                                             (@client_pk, 'http://localhost:8082/auth/callback'),
                                                                             (@client_pk, 'https://cdb.platformcommons.org/provider-registry/auth/callback');

-- Add scopes
INSERT IGNORE INTO oauth2_client_scopes (client_id, scope) VALUES
                                                               (@client_pk, 'read'),
                                                               (@client_pk, 'write');

-- Add grant types
INSERT IGNORE INTO oauth2_client_grant_types (client_id, grant_type) VALUES
    (@client_pk, 'authorization_code');