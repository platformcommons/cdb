use cdb_api_registry_db;


CREATE TABLE cdb_api_registry_db.cdb_api_domains (
                                 api_id BIGINT NOT NULL,
                                 domains VARCHAR(255),
                                 FOREIGN KEY (api_id) REFERENCES cdb_api(id)
);


-- Index for tags table
CREATE INDEX idx_api_tags_value ON cdb_api_registry_db.cdb_api_tags(tags);

-- Index for domains table
CREATE INDEX idx_api_domains_value ON cdb_api_registry_dbcdb_api_domains(domains);

-- Composite indexes for better join performance
CREATE INDEX idx_api_tags_api_id ON cdb_api_registry_db.cdb_api_tags(api_id);
CREATE INDEX idx_api_domains_api_id ON cdb_api_registry_db.cdb_api_domains(api_id);
