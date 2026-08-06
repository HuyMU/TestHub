#!/usr/bin/env bash
# TestFlow Lite - Scheduled MySQL Backup Script
# Usage: ./scripts/backup-db.sh [backup_dir]

set -euo pipefail

BACKUP_DIR="${1:-./backups}"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
DB_CONTAINER="${DB_CONTAINER:-testhub-mysql}"
DB_NAME="${DB_NAME:-testhub_db}"
DB_USER="${DB_USER:-testhub_user}"
DB_PASSWORD="${DB_PASSWORD:-testhub_pass}"

mkdir -p "${BACKUP_DIR}"

BACKUP_FILE="${BACKUP_DIR}/${DB_NAME}_backup_${TIMESTAMP}.sql.gz"

echo "[$(date)] Starting MySQL backup for ${DB_NAME}..."

if command -v docker >/dev/null 2>&1 && docker ps | grep -q "${DB_CONTAINER}"; then
    docker exec "${DB_CONTAINER}" mysqldump -u"${DB_USER}" -p"${DB_PASSWORD}" "${DB_NAME}" | gzip > "${BACKUP_FILE}"
else
    mysqldump -h"${DB_HOST:-localhost}" -P"${DB_PORT:-3306}" -u"${DB_USER}" -p"${DB_PASSWORD}" "${DB_NAME}" | gzip > "${BACKUP_FILE}"
fi

echo "[$(date)] Backup completed successfully: ${BACKUP_FILE}"

# Retention Policy: keep backups from last 30 days
find "${BACKUP_DIR}" -type f -name "*.sql.gz" -mtime +30 -delete
echo "[$(date)] Cleaned up backups older than 30 days."
