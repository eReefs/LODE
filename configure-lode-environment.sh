#!/bin/bash
set -e

if [ -z "${LODE_CONTEXT_PATH}" ]; then
    >&2 echo "ERROR: the LODE_CONTEXT_PATH variable is not set!"
    exit 1
fi

LODE_PROPERTIES_FILE="${LODE_CONTEXT_PATH}/config.properties"
echo "Populating the LODE properties file at '${LODE_PROPERTIES_FILE}'"
cat <<EOF > "${LODE_PROPERTIES_FILE}"
defaultLang=${DEFAULT_LANG:-en}
externalURL=${LODE_EXTERNAL_URL:-}
maxTentative=${MAX_TENTATIVE:-3}
vendorCss=${VENDOR_CSS:-}
vendorName=${VENDOR_NAME:-}
vendorUrl=${VENDOR_URL:-}
webvowl=${WEBVOWL_EXTERNAL_URL:-}
EOF

cat "${LODE_PROPERTIES_FILE}"
