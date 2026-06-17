#!/bin/bash
# Run once after 'docker-compose up' to install code execution runtimes into Piston.
# Packages persist in the piston_data Docker volume and only need to be installed once
# per environment (re-running this script is safe — it skips already-installed runtimes).

PISTON_URL="${PISTON_URL:-http://localhost:2000}"

echo "Checking Piston at $PISTON_URL..."

runtimes=$(curl -sf "$PISTON_URL/api/v2/runtimes") || {
    echo "ERROR: Could not reach Piston. Make sure 'docker-compose up' is running first." >&2
    exit 1
}

install_if_missing() {
    local lang=$1 ver=$2
    if echo "$runtimes" | grep -q "\"language\":\"$lang\""; then
        echo "  $lang-$ver already installed, skipping."
    else
        echo "  Installing $lang-$ver (this may take a few minutes)..."
        curl -sf -X POST "$PISTON_URL/api/v2/packages" \
            -H "Content-Type: application/json" \
            -d "{\"language\":\"$lang\",\"version\":\"$ver\"}" > /dev/null
        echo "  Done."
    fi
}

install_if_missing java   15.0.2
install_if_missing python 3.10.0
install_if_missing mono   6.12.0

echo "All runtimes ready."
