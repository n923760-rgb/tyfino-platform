BEGIN;

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE admins (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  email text NOT NULL UNIQUE,
  password_hash text NOT NULL,
  role text NOT NULL DEFAULT 'admin' CHECK (role IN ('owner', 'admin', 'support')),
  is_active boolean NOT NULL DEFAULT true,
  last_totp_counter bigint,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE admin_login_challenges (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  admin_id uuid NOT NULL REFERENCES admins(id) ON DELETE CASCADE,
  token_hash text NOT NULL UNIQUE,
  attempts smallint NOT NULL DEFAULT 0 CHECK (attempts BETWEEN 0 AND 5),
  ip_address inet,
  user_agent text,
  expires_at timestamptz NOT NULL,
  consumed_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE admin_sessions (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  admin_id uuid NOT NULL REFERENCES admins(id) ON DELETE CASCADE,
  token_hash text NOT NULL UNIQUE,
  ip_address inet,
  user_agent text,
  expires_at timestamptz NOT NULL,
  revoked_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE installations (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  installation_hash text NOT NULL UNIQUE,
  platform text NOT NULL CHECK (platform = 'android'),
  app_version text NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  last_seen_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE activation_codes (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  code_hash text NOT NULL UNIQUE,
  code_suffix char(6) NOT NULL,
  license_kind text NOT NULL CHECK (license_kind IN ('one_year', 'lifetime')),
  status text NOT NULL DEFAULT 'unused' CHECK (status IN ('unused', 'active', 'revoked')),
  pre_activation_expires_at timestamptz,
  activated_at timestamptz,
  grant_starts_at timestamptz,
  grant_expires_at timestamptz,
  bound_installation_id uuid REFERENCES installations(id) ON DELETE RESTRICT,
  customer_name text,
  phone_number text,
  external_reference text,
  admin_label text,
  internal_note text,
  created_by uuid REFERENCES admins(id) ON DELETE SET NULL,
  revoked_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  CHECK ((license_kind = 'lifetime' AND grant_expires_at IS NULL) OR license_kind = 'one_year')
);

CREATE TABLE trial_entitlements (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  installation_id uuid NOT NULL UNIQUE REFERENCES installations(id) ON DELETE RESTRICT,
  starts_at timestamptz NOT NULL,
  expires_at timestamptz NOT NULL,
  revoked_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now(),
  CHECK (expires_at > starts_at)
);

CREATE TABLE license_sessions (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  installation_id uuid NOT NULL REFERENCES installations(id) ON DELETE CASCADE,
  activation_code_id uuid REFERENCES activation_codes(id) ON DELETE CASCADE,
  trial_entitlement_id uuid REFERENCES trial_entitlements(id) ON DELETE CASCADE,
  token_hash text NOT NULL UNIQUE,
  expires_at timestamptz NOT NULL,
  revoked_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now(),
  CHECK ((activation_code_id IS NOT NULL)::int + (trial_entitlement_id IS NOT NULL)::int = 1)
);

CREATE TABLE app_settings (
  singleton boolean PRIMARY KEY DEFAULT true CHECK (singleton = true),
  trial_enabled boolean NOT NULL DEFAULT true,
  updated_at timestamptz NOT NULL DEFAULT now()
);
INSERT INTO app_settings (singleton, trial_enabled) VALUES (true, true);

CREATE SEQUENCE audit_logs_id_seq;

CREATE TABLE audit_logs (
  id bigint PRIMARY KEY,
  admin_id uuid REFERENCES admins(id) ON DELETE SET NULL,
  action text NOT NULL,
  entity_type text NOT NULL,
  entity_id text,
  metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
  ip_address inet,
  previous_hash char(64) NOT NULL DEFAULT repeat('0', 64),
  entry_hash char(64) NOT NULL DEFAULT repeat('0', 64),
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE FUNCTION audit_log_hash(
  p_id bigint,
  p_admin_id uuid,
  p_action text,
  p_entity_type text,
  p_entity_id text,
  p_metadata jsonb,
  p_ip_address inet,
  p_created_at timestamptz,
  p_previous_hash text
) RETURNS text LANGUAGE sql IMMUTABLE AS $$
  SELECT encode(digest(concat_ws('|',
    p_id::text,
    coalesce(p_admin_id::text, ''),
    encode(convert_to(p_action, 'UTF8'), 'hex'),
    encode(convert_to(p_entity_type, 'UTF8'), 'hex'),
    encode(convert_to(coalesce(p_entity_id, ''), 'UTF8'), 'hex'),
    encode(convert_to(p_metadata::text, 'UTF8'), 'hex'),
    coalesce(host(p_ip_address), ''),
    floor(extract(epoch FROM p_created_at) * 1000000)::bigint::text,
    p_previous_hash
  ), 'sha256'), 'hex')
$$;

CREATE FUNCTION chain_audit_log() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
  PERFORM pg_advisory_xact_lock(812194, 1);
  NEW.id := nextval('audit_logs_id_seq');
  SELECT entry_hash INTO NEW.previous_hash FROM audit_logs ORDER BY id DESC LIMIT 1;
  NEW.previous_hash := coalesce(NEW.previous_hash, repeat('0', 64));
  NEW.entry_hash := audit_log_hash(
    NEW.id, NEW.admin_id, NEW.action, NEW.entity_type, NEW.entity_id,
    NEW.metadata, NEW.ip_address, NEW.created_at, NEW.previous_hash
  );
  RETURN NEW;
END
$$;

ALTER SEQUENCE audit_logs_id_seq OWNED BY audit_logs.id;

CREATE FUNCTION reject_audit_log_mutation() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
  RAISE EXCEPTION 'audit_logs is append-only' USING ERRCODE = '55000';
END
$$;

CREATE FUNCTION verify_audit_log_chain() RETURNS boolean LANGUAGE sql STABLE AS $$
  WITH ordered AS (
    SELECT al.*,
           lag(entry_hash, 1, repeat('0', 64)::char(64)) OVER (ORDER BY id) AS expected_previous_hash
      FROM audit_logs al
  )
  SELECT coalesce(bool_and(
    previous_hash = expected_previous_hash
    AND entry_hash = audit_log_hash(
      id, admin_id, action, entity_type, entity_id, metadata, ip_address, created_at, previous_hash
    )
  ), true) FROM ordered
$$;

CREATE TRIGGER audit_logs_chain_before_insert
BEFORE INSERT ON audit_logs FOR EACH ROW EXECUTE FUNCTION chain_audit_log();

CREATE TRIGGER audit_logs_append_only
BEFORE UPDATE OR DELETE ON audit_logs FOR EACH ROW EXECUTE FUNCTION reject_audit_log_mutation();

CREATE INDEX admin_sessions_expiry_idx ON admin_sessions(expires_at) WHERE revoked_at IS NULL;
CREATE UNIQUE INDEX admins_single_owner_idx ON admins ((role)) WHERE role = 'owner';
CREATE INDEX admin_login_challenges_expiry_idx ON admin_login_challenges(expires_at) WHERE consumed_at IS NULL;
CREATE INDEX activation_codes_status_idx ON activation_codes(status, grant_expires_at);
CREATE INDEX license_sessions_installation_idx ON license_sessions(installation_id);
CREATE INDEX license_sessions_expiry_idx ON license_sessions(expires_at) WHERE revoked_at IS NULL;
CREATE INDEX audit_logs_created_idx ON audit_logs(created_at DESC);
CREATE UNIQUE INDEX audit_logs_entry_hash_idx ON audit_logs(entry_hash);

COMMIT;
