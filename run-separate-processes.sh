#!/usr/bin/env bash
# This script runs each player in its own Java process (two different PIDs) over TCP.
set -euo pipefail
cd "$(dirname "$0")"

HOST="127.0.0.1"
PORT="5050"

echo ">>> Compiling..."
mvn -q compile

echo ">>> Starting RESPONDER process (listening on ${HOST}:${PORT})..."
java -cp target/classes com.prit.messaging.app.SeparateProcessApp responder "${HOST}" "${PORT}" &
RESPONDER_PID=$!

echo ">>> Starting INITIATOR process (connecting to ${HOST}:${PORT})..."
java -cp target/classes com.prit.messaging.app.SeparateProcessApp initiator "${HOST}" "${PORT}" &
INITIATOR_PID=$!

echo ">>> Launched: responder OS pid=${RESPONDER_PID}, initiator OS pid=${INITIATOR_PID}"

wait "${INITIATOR_PID}"
wait "${RESPONDER_PID}"
echo ">>> Both processes finished."