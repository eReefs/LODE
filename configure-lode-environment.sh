#!/bin/bash
set -e

if [ -z "${LODE_CONFIG}" ]; then
    >&2 echo "ERROR: the LODE_CONFIG variable is not set!"
    exit 1
fi

echo "Populating the LODE properties file at '${LODE_CONFIG}'"
cat <<EOF > "${LODE_CONFIG}"
defaultLang=${DEFAULT_LANG:-en}
externalURL=${LODE_EXTERNAL_URL:-}
maxTentative=${MAX_TENTATIVE:-3}
vendorCss=${VENDOR_CSS:-}
vendorName=${VENDOR_NAME:-}
vendorUrl=${VENDOR_URL:-}
webvowl=${WEBVOWL_EXTERNAL_URL:-}
EOF

cat "${LODE_CONFIG}"
