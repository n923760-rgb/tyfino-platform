BEGIN;

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE admins (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  email text NOT NULL UNIQUE,
  password_hash text NOT NULL,
  role text NOT NULL DEFAULT 'admin' CHECK (role IN ('owner', 'admin', 'support')),
  is_active boolean NOT NULL DEFAULT true,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
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

CREATE TABLE audit_logs (
  id bigserial PRIMARY KEY,
  admin_id uuid REFERENCES admins(id) ON DELETE SET NULL,
  action text NOT NULL,
  entity_type text NOT NULL,
  entity_id text,
  metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
  ip_address inet,
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX admin_sessions_expiry_idx ON admin_sessions(expires_at) WHERE revoked_at IS NULL;
CREATE INDEX activation_codes_status_idx ON activation_codes(status, grant_expires_at);
CREATE INDEX license_sessions_installation_idx ON license_sessions(installation_id);
CREATE INDEX license_sessions_expiry_idx ON license_sessions(expires_at) WHERE revoked_at IS NULL;
CREATE INDEX audit_logs_created_idx ON audit_logs(created_at DESC);

COMMIT;
