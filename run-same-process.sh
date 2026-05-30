#!/usr/bin/env bash
# This script runs both players inside a single Java process (one PID, two threads).
set -euo pipefail
cd "$(dirname "$0")"

echo ">>> Compiling..."
mvn -q compile

echo ">>> Running same-process demo..."
java -cp target/classes com.prit.messaging.app.SameProcessApp