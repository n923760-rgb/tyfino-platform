#!/usr/bin/env bash
set -Eeuo pipefail

if [[ $# -ne 1 ]]; then
  echo "Usage: RESTORE_DATABASE=tyfino_restore_<id> PGHOST=... PGUSER=... $0 /path/to/backup.dump" >&2
  exit 64
fi

: "${RESTORE_DATABASE:?RESTORE_DATABASE must name the disposable verification database}"

backup_file=$1
checksum_file="${backup_file}.sha256"

if [[ ! "$RESTORE_DATABASE" =~ ^tyfino_restore_[a-zA-Z0-9_]+$ ]]; then
  echo "RESTORE_DATABASE must match tyfino_restore_<letters-or-digits>" >&2
  exit 64
fi

if [[ ! -r "$backup_file" || ! -r "$checksum_file" ]]; then
  echo "Backup and matching .sha256 file must both be readable" >&2
  exit 66
fi

(
  cd "$(dirname "$backup_file")"
  sha256sum --check --status "$(basename "$checksum_file")"
)
pg_restore --list "$backup_file" >/dev/null

if psql --dbname=postgres --tuples-only --no-align --command="SELECT 1 FROM pg_database WHERE datname = '$RESTORE_DATABASE'" | grep -qx 1; then
  echo "Refusing to use an existing restore database: $RESTORE_DATABASE" >&2
  exit 73
fi

cleanup() {
  dropdb --if-exists "$RESTORE_DATABASE" >/dev/null
}
trap cleanup EXIT

createdb "$RESTORE_DATABASE"
pg_restore --exit-on-error --no-owner --no-acl --dbname="$RESTORE_DATABASE" "$backup_file"

psql --dbname="$RESTORE_DATABASE" --set=ON_ERROR_STOP=1 <<'SQL'
DO $$
BEGIN
  IF to_regclass('public.admins') IS NULL
     OR to_regclass('public.activation_codes') IS NULL
     OR to_regclass('public.audit_logs') IS NULL THEN
    RAISE EXCEPTION 'required TYFINO tables are missing after restore';
  END IF;

  IF NOT verify_audit_log_chain() THEN
    RAISE EXCEPTION 'audit log hash chain failed after restore';
  END IF;

  INSERT INTO audit_logs (action, entity_type, metadata)
  VALUES ('restore.verification_probe', 'database', '{"disposable":true}'::jsonb);

  IF NOT verify_audit_log_chain() THEN
    RAISE EXCEPTION 'audit log hash chain failed after verification insert';
  END IF;
END
$$;
SQL

if psql --dbname="$RESTORE_DATABASE" --set=ON_ERROR_STOP=1 \
  --command="UPDATE audit_logs SET action = 'restore_verification_tamper' WHERE action = 'restore.verification_probe'"; then
  echo "Restored audit log accepted a forbidden mutation" >&2
  exit 1
fi

echo "Backup restored and verified in disposable database: $RESTORE_DATABASE"
