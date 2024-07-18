#!/bin/bash
set -e

if [ -z "${LODE_CONTEXT_PATH}" ]; then
    >&2 echo "ERROR: the LODE_CONTEXT_PATH variable is not set!"
    exit 1
fi

USE_HTTPS="${USE_HTTPS:-false}"
LODE_SCHEME="${CONNECTOR_PROXY_SCHEME:-http}"
if [[ "${USE_HTTPS}" == "true" ]] || [[ "${CONNECTOR_PROXY_SECURE:-false}" == "true" ]]; then
    LODE_SCHEME="https"
fi
if [ -z "${LODE_EXTERNAL_URL:-}" ]; then
    LODE_HOST="${CONNECTOR_PROXY_NAME:-localhost}"
    LODE_PORT="${CONNECTOR_PROXY_PORT:-8080}"
    LODE_EXTERNAL_URL="${LODE_SCHEME}://${LODE_HOST}:${LODE_PORT}"
    if [[ "${LODE_CONTEXT}" != "ROOT" ]]; then
        LODE_EXTERNAL_URL="${LODE_EXTERNAL_URL}/${LODE_CONTEXT}"
    fi
fi
if [ -z "${WEBVOWL_EXTERNAL_URL}" ]; then
    WEBVOWL_EXTERNAL_URL="http://visualdataweb.de/webvowl/#iri="
fi

LODE_PROPERTIES_FILE="${LODE_CONTEXT_PATH}/config.properties"
echo "Populating the LODE properties file at '${LODE_PROPERTIES_FILE}'"
cat <<EOF > "${LODE_PROPERTIES_FILE}"
externalURL=${LODE_EXTERNAL_URL}
webvowl=${WEBVOWL_EXTERNAL_URL}
useHTTPs=${USE_HTTPS:-false}
EOF

cat "${LODE_PROPERTIES_FILE}"
