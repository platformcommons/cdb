USE cdb_provider_registry_db;


INSERT INTO cdb_providers (name,
                           code,
                           description,
                           status,
                           owner_user_id,
                           contact_email,
                           contact_phone,
                           tags,
                           created_by,
                           updated_by,
                           created_by_provider,
                           updated_by_provider,
                           is_active)
VALUES ('CDB Platform',
        'CDB_PLATFORM',
        'CDB Platform Provider.',
        'ACTIVE',
        1,
        'aashish@platformcommons.com',
        '+918792237725',
        'platform,technology,innovation,governance,digital,cdb',
        1,
        1,
        0,
        0,
        1);