# PostgreSQL audit log setup

The application uses Flyway to version and apply the JPA tables (`users` and
`auth_sessions`) at startup. Hibernate is set to `validate`, so startup fails
if those tables do not match the entity mappings. Flyway validates recorded
migration checksums before applying migrations. New installations start with
`V1`; an existing non-empty database is baselined at version `0`, so `V1`
runs without replacing existing tables and Hibernate checks their structure.
Keep future schema changes in new `db/migration/V...` files; never edit a
migration after it has been applied.

Audit log table ownership and grants remain a separate DBA-managed setup. This
is intentional: the audit table has a different owner and restricted writer / reader roles.
The backend maps the audit table to an immutable JPA entity and scans that
repository with a dedicated persistence unit using the reader-only datasource;
audit events are written through a separate Spring Data JPA persistence unit
using the restricted writer datasource and a bounded asynchronous queue.

Run these scripts once against the application database as a PostgreSQL administrator:

1. Run `audit-roles.sql` to create the non-login owner role and separate writer and reader login roles.
2. Set distinct passwords for `capstone_audit_writer` and `capstone_audit_reader` interactively with `psql`'s `\password` command. Do not put these passwords in the SQL files.
3. Run `audit-log-schema.sql` as a superuser (or a role allowed to assume `capstone_audit_owner`). It transfers ownership of the existing `public.application_logs` table and identity sequence to the non-login owner, adds the actor, subject, and correlation ID columns, installs bounded retention, and grants the writer insert access and the reader select access. Re-run this script when upgrading an existing audit table so its column grants include `correlation_id`.
4. Set `AUDIT_DB_WRITER_USERNAME`, `AUDIT_DB_WRITER_PASSWORD`, `AUDIT_DB_READER_USERNAME`, and `AUDIT_DB_READER_PASSWORD` in `.env`. The audit DB URL uses `DB_URL`, the same database used by the application datasource.

The main application database account does not need access to `application_logs`. PostgreSQL stores at most the latest 10,000 rows; a trigger owned by the non-login role enforces this without granting delete rights to the writer login. The SQL creates a separate `audit` schema for the retention function, while the table remains in `public` to preserve existing log rows.

The rolling file appender retains 30 days of files with a 1 GB total cap. Run the service under a dedicated OS account and restrict `logs/` to that account and approved operators (directory mode `0700`, files `0600` on Unix systems). Console output is controlled by the deployment platform; restrict who can read it and configure retention there as well. Every request gets an `X-Correlation-ID` response header, which is also included in application logs and audit rows. Queue overflow and database write failures are reported to the console through a dedicated logger that does not recurse into audit persistence; alert on `AUDIT_LOG_QUEUE_FULL` and `AUDIT_LOG_WRITE_FAILED`.

To bootstrap the first application administrator, register the intended account through `/signup`, then run `promote-initial-admin.sql` as a database administrator with that username supplied as a psql variable. Signup always creates a `USER`; it never accepts a requested role from a public caller.
