#!/usr/bin/env bash
# Source from a script whose working directory is the project root.
# Git Bash/Cygwin launch Windows Java, which uses semicolons in its classpath.
case "${OSTYPE:-}" in
  msys*|cygwin*|win32*) game_classpath='build/classes;lib/core-4.3.1.jar' ;;
  *) game_classpath='build/classes:lib/core-4.3.1.jar' ;;
esac
