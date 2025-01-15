#!/bin/bash

# Get current timestamp
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_NAME="trendflick_backup_${TIMESTAMP}"

# Create backup directory if it doesn't exist
mkdir -p backups

# Create a zip archive of the project, excluding unnecessary files
zip -r "backups/${BACKUP_NAME}.zip" . \
    -x "*.git/*" \
    -x "*.gradle/*" \
    -x "*/build/*" \
    -x "*.idea/*" \
    -x "*.iml" \
    -x "local.properties" \
    -x "*.hprof" \
    -x "secrets.properties" \
    -x "app/release/*" \
    -x "app/debug/*" \
    -x "backups/*"

echo "Backup created: backups/${BACKUP_NAME}.zip" 