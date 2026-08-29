#!/bin/sh
set -eu
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd -P)
if [ -n "${JAVA_HOME:-}" ]; then
  JAVA_CMD="$JAVA_HOME/bin/java"
else
  JAVA_CMD="java"
fi
if ! command -v "$JAVA_CMD" >/dev/null 2>&1 && [ ! -x "$JAVA_CMD" ]; then
  echo "ERROR: Java was not found. Set JAVA_HOME to JDK 17 or newer." >&2
  exit 1
fi
exec "$JAVA_CMD" -cp "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
