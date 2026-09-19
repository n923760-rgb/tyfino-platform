#!/usr/bin/env bash
set -Eeuo pipefail

if [[ $# -ne 1 ]]; then
  echo "Usage: DATABASE_URL=... $0 /path/to/backup.dump" >&2
  exit 64
fi

: "${DATABASE_URL:?DATABASE_URL must identify the database to back up}"

backup_file=$1
checksum_file="${backup_file}.sha256"

if [[ -e "$backup_file" || -e "$checksum_file" ]]; then
  echo "Refusing to overwrite an existing backup or checksum: $backup_file" >&2
  exit 73
fi

mkdir -p "$(dirname "$backup_file")"
umask 077

pg_dump \
  --dbname="$DATABASE_URL" \
  --format=custom \
  --compress=9 \
  --no-owner \
  --no-acl \
  --file="$backup_file"

pg_restore --list "$backup_file" >/dev/null
(
  cd "$(dirname "$backup_file")"
  sha256sum "$(basename "$backup_file")" >"$(basename "$checksum_file")"
)

echo "Backup created and structurally validated: $backup_file"
