-- ============================================================================
-- PostgreSQL Initialization Script for CookMate Multi-DB Architecture
-- ============================================================================
-- This script initializes the keycloak database and user during postgres 
-- container startup. It runs automatically when the container starts for 
-- the first time and creates a separate logical database for Keycloak with
-- appropriate permissions.
-- ============================================================================

-- Create keycloak database user (if not exists)
DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_user WHERE usename = 'keycloak') THEN
    CREATE USER keycloak WITH PASSWORD 'keycloak';
  END IF;
END $$;

-- Create keycloak database
CREATE DATABASE keycloak OWNER keycloak ENCODING 'UTF8' TEMPLATE template0;

-- Grant necessary privileges to keycloak user on the database
GRANT ALL PRIVILEGES ON DATABASE keycloak TO keycloak;

-- Set up default privileges for the keycloak database
-- This must be done as the database owner or superuser
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL PRIVILEGES ON TABLES TO keycloak;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL PRIVILEGES ON SEQUENCES TO keycloak;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO keycloak;
