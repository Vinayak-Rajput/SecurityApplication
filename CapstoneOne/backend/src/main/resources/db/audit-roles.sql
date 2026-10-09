-- Run once as a PostgreSQL administrator. These credentials have no passwords
-- until set interactively with psql's \password command or your secret manager.
CREATE ROLE capstone_audit_owner NOLOGIN;
CREATE ROLE capstone_audit_writer LOGIN;
CREATE ROLE capstone_audit_reader LOGIN;
