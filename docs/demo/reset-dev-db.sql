-- =====================================================================
-- RESET the local dev database to the fresh README sample data.
--
-- When: before taking screenshots, or after testing payments/registrations
--       when you want the original data back.
-- How : 1. Stop the backend.
--       2. pgAdmin -> database "healthdekho" -> Query Tool -> run this file.
--       3. Start the backend: Flyway sees an empty schema and re-runs V1 + V2.
--
-- DEV ONLY. This deletes every table and row in the healthdekho database.
-- =====================================================================

DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
