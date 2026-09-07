BEGIN;

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TYPE customer_status AS ENUM ('active', 'suspended', 'archived');
CREATE TYPE provider_protocol AS ENUM ('xtream', 'm3u', 'stalker');
CREATE TYPE activation_status AS ENUM ('unused', 'active', 'revoked', 'expired');
CREATE TYPE device_status AS ENUM ('active', 'blocked');

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

CREATE TABLE customers (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  display_name text NOT NULL,
  phone text,
  whatsapp text,
  notes text,
  status customer_status NOT NULL DEFAULT 'active',
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE provider_hosts (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  label text NOT NULL,
  protocol provider_protocol NOT NULL,
  base_url text NOT NULL,
  is_active boolean NOT NULL DEFAULT true,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (protocol, base_url)
);

CREATE TABLE provider_accounts (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  customer_id uuid NOT NULL REFERENCES customers(id) ON DELETE RESTRICT,
  host_id uuid NOT NULL REFERENCES provider_hosts(id) ON DELETE RESTRICT,
  external_reference text,
  encrypted_username text,
  encrypted_password text,
  encrypted_playlist_url text,
  encrypted_portal_data text,
  expires_at timestamptz,
  max_connections integer CHECK (max_connections IS NULL OR max_connections > 0),
  is_active boolean NOT NULL DEFAULT true,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE activation_codes (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  customer_id uuid NOT NULL REFERENCES customers(id) ON DELETE RESTRICT,
  provider_account_id uuid REFERENCES provider_accounts(id) ON DELETE RESTRICT,
  code_hash text NOT NULL UNIQUE,
  code_suffix char(4) NOT NULL,
  status activation_status NOT NULL DEFAULT 'unused',
  device_limit integer NOT NULL DEFAULT 1 CHECK (device_limit BETWEEN 1 AND 10),
  starts_at timestamptz,
  expires_at timestamptz,
  activated_at timestamptz,
  revoked_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  CHECK (expires_at IS NULL OR starts_at IS NULL OR expires_at > starts_at)
);

CREATE TABLE devices (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  activation_code_id uuid NOT NULL REFERENCES activation_codes(id) ON DELETE CASCADE,
  device_fingerprint_hash text NOT NULL,
  platform text NOT NULL,
  model text,
  app_version text,
  status device_status NOT NULL DEFAULT 'active',
  first_seen_at timestamptz NOT NULL DEFAULT now(),
  last_seen_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (activation_code_id, device_fingerprint_hash)
);

CREATE TABLE player_sessions (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  device_id uuid NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
  token_hash text NOT NULL UNIQUE,
  expires_at timestamptz NOT NULL,
  revoked_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now(),
  last_seen_at timestamptz NOT NULL DEFAULT now()
);

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

CREATE INDEX provider_accounts_customer_idx ON provider_accounts(customer_id);
CREATE INDEX admin_sessions_admin_idx ON admin_sessions(admin_id);
CREATE INDEX admin_sessions_expiry_idx ON admin_sessions(expires_at) WHERE revoked_at IS NULL;
CREATE INDEX activation_codes_customer_idx ON activation_codes(customer_id);
CREATE INDEX activation_codes_status_expiry_idx ON activation_codes(status, expires_at);
CREATE INDEX devices_activation_idx ON devices(activation_code_id);
CREATE INDEX player_sessions_device_idx ON player_sessions(device_id);
CREATE INDEX player_sessions_expiry_idx ON player_sessions(expires_at) WHERE revoked_at IS NULL;
CREATE INDEX audit_logs_created_idx ON audit_logs(created_at DESC);

COMMIT;
