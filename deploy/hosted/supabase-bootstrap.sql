-- Run manually once in a NEW Supabase project, as postgres, after review.
-- No password is embedded. After running, set irene_app's password privately
-- with psql's \password irene_app, or your private Supabase administration UI.
-- Never grant this login to browsers. It owns only Irene's application schema.
BEGIN;
CREATE ROLE irene_app LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION;
-- Supabase administrator may manage this dedicated role and its schema.
GRANT irene_app TO postgres;
CREATE SCHEMA irene AUTHORIZATION irene_app;
GRANT CONNECT ON DATABASE postgres TO irene_app;
REVOKE ALL ON SCHEMA irene FROM PUBLIC, anon, authenticated, service_role;
ALTER ROLE irene_app SET search_path = irene;
-- These defaults apply to all tables/sequences created by Flyway as irene_app.
ALTER DEFAULT PRIVILEGES FOR ROLE irene_app IN SCHEMA irene
  REVOKE ALL ON TABLES FROM PUBLIC, anon, authenticated, service_role;
ALTER DEFAULT PRIVILEGES FOR ROLE irene_app IN SCHEMA irene
  REVOKE ALL ON SEQUENCES FROM PUBLIC, anon, authenticated, service_role;
COMMIT;
-- Keep 'irene' OUT of the Supabase Data API's exposed schemas.
-- Do not rerun on an existing schema or grant anon/authenticated schema access.
