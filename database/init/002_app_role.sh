#!/usr/bin/env bash
set -Eeuo pipefail

: "${POSTGRES_USER:?POSTGRES_USER must identify the schema owner}"
: "${POSTGRES_DB:?POSTGRES_DB must identify the application database}"
: "${POSTGRES_APP_USER:?POSTGRES_APP_USER must identify the restricted application role}"
: "${POSTGRES_APP_PASSWORD:?POSTGRES_APP_PASSWORD must be set}"

if [[ "$POSTGRES_APP_USER" == "$POSTGRES_USER" ]]; then
  echo "POSTGRES_APP_USER must differ from the schema owner" >&2
  exit 64
fi
if [[ ${#POSTGRES_APP_PASSWORD} -lt 20 || "$POSTGRES_APP_PASSWORD" == *CHANGE_ME* ]]; then
  echo "POSTGRES_APP_PASSWORD must be at least 20 characters and changed from the example" >&2
  exit 64
fi

psql --set=ON_ERROR_STOP=1 --username="$POSTGRES_USER" --dbname="$POSTGRES_DB" <<'SQL'
\getenv app_user POSTGRES_APP_USER
\getenv app_password POSTGRES_APP_PASSWORD

BEGIN;

SELECT format('CREATE ROLE %I', :'app_user')
 WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = :'app_user')
\gexec

ALTER ROLE :"app_user" WITH
  LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOREPLICATION
  PASSWORD :'app_password';

SELECT (
  EXISTS (
    SELECT 1
      FROM pg_class object
      JOIN pg_roles owner ON owner.oid = object.relowner
      JOIN pg_namespace namespace ON namespace.oid = object.relnamespace
     WHERE owner.rolname = :'app_user'
       AND namespace.nspname NOT IN ('pg_catalog', 'information_schema')
       AND namespace.nspname !~ '^pg_toast'
  ) OR EXISTS (
    SELECT 1
      FROM pg_proc object
      JOIN pg_roles owner ON owner.oid = object.proowner
      JOIN pg_namespace namespace ON namespace.oid = object.pronamespace
     WHERE owner.rolname = :'app_user'
       AND namespace.nspname NOT IN ('pg_catalog', 'information_schema')
       AND namespace.nspname !~ '^pg_toast'
  ) OR EXISTS (
    SELECT 1
      FROM pg_namespace object
      JOIN pg_roles owner ON owner.oid = object.nspowner
     WHERE owner.rolname = :'app_user'
       AND object.nspname NOT IN ('pg_catalog', 'information_schema')
       AND object.nspname !~ '^pg_toast'
  ) OR EXISTS (
    SELECT 1
      FROM pg_database object
      JOIN pg_roles owner ON owner.oid = object.datdba
     WHERE owner.rolname = :'app_user'
       AND object.datname = current_database()
  ) OR EXISTS (
    SELECT 1
      FROM pg_extension object
      JOIN pg_roles owner ON owner.oid = object.extowner
     WHERE owner.rolname = :'app_user'
  )
) AS app_role_owns_objects
\gset

\if :app_role_owns_objects
  DO $block$
  BEGIN
    RAISE EXCEPTION 'Restricted application role owns database objects; transfer ownership through a reviewed migration before continuing.';
  END
  $block$;
\endif

SELECT format('REVOKE %I FROM %I', granted_role.rolname, member_role.rolname)
  FROM pg_auth_members membership
  JOIN pg_roles granted_role ON granted_role.oid = membership.roleid
  JOIN pg_roles member_role ON member_role.oid = membership.member
 WHERE member_role.rolname = :'app_user'
\gexec

SELECT format('REVOKE ALL PRIVILEGES ON DATABASE %I FROM %I', current_database(), :'app_user')
\gexec
REVOKE ALL PRIVILEGES ON SCHEMA public FROM :"app_user";
REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA public FROM :"app_user";
REVOKE ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public FROM :"app_user";
REVOKE ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA public FROM :"app_user";

ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL PRIVILEGES ON TABLES FROM :"app_user";
ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL PRIVILEGES ON SEQUENCES FROM :"app_user";
ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL PRIVILEGES ON FUNCTIONS FROM :"app_user";

SELECT format('GRANT CONNECT ON DATABASE %I TO %I', current_database(), :'app_user')
\gexec
GRANT USAGE ON SCHEMA public TO :"app_user";

GRANT SELECT ON
  admins, admin_login_challenges, admin_sessions, installations, activation_codes,
  trial_entitlements, license_sessions, app_settings, audit_logs
TO :"app_user";

GRANT INSERT (email, password_hash, role) ON admins TO :"app_user";
GRANT UPDATE (last_totp_counter, updated_at) ON admins TO :"app_user";

GRANT INSERT (admin_id, token_hash, ip_address, user_agent, expires_at)
  ON admin_login_challenges TO :"app_user";
GRANT UPDATE (attempts, consumed_at) ON admin_login_challenges TO :"app_user";
GRANT DELETE ON admin_login_challenges TO :"app_user";

GRANT INSERT (admin_id, token_hash, ip_address, user_agent, expires_at)
  ON admin_sessions TO :"app_user";
GRANT UPDATE (revoked_at) ON admin_sessions TO :"app_user";

GRANT INSERT (installation_hash, platform, app_version) ON installations TO :"app_user";
GRANT UPDATE (platform, app_version, last_seen_at) ON installations TO :"app_user";

GRANT INSERT
  (code_hash, code_suffix, license_kind, pre_activation_expires_at, customer_name,
   phone_number, external_reference, admin_label, internal_note, created_by)
  ON activation_codes TO :"app_user";
GRANT UPDATE
  (status, activated_at, grant_starts_at, grant_expires_at, bound_installation_id,
   revoked_at, updated_at)
  ON activation_codes TO :"app_user";

GRANT INSERT (installation_id, starts_at, expires_at) ON trial_entitlements TO :"app_user";
GRANT UPDATE (revoked_at) ON trial_entitlements TO :"app_user";

GRANT INSERT
  (installation_id, activation_code_id, trial_entitlement_id, token_hash, expires_at)
  ON license_sessions TO :"app_user";
GRANT UPDATE (revoked_at) ON license_sessions TO :"app_user";

GRANT UPDATE (trial_enabled, updated_at) ON app_settings TO :"app_user";
GRANT INSERT (admin_id, action, entity_type, entity_id, metadata, ip_address)
  ON audit_logs TO :"app_user";
GRANT USAGE, SELECT ON SEQUENCE audit_logs_id_seq TO :"app_user";

GRANT EXECUTE ON FUNCTION audit_log_hash(bigint, uuid, text, text, text, jsonb, inet, timestamptz, text)
  TO :"app_user";
GRANT EXECUTE ON FUNCTION verify_audit_log_chain() TO :"app_user";

COMMIT;
SQL

echo "Restricted PostgreSQL application role configured: $POSTGRES_APP_USER"
