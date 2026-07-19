#!/usr/bin/env sh
set -eu
APP_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
exec java -jar "$APP_DIR/Pokemon DS Map Studio.jar" "$@"
