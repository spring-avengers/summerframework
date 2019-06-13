#!/usr/bin/env bash
find . -name "*.versionsBackup" -exec rm -rf {} \;
find . -maxdepth 2 -type d \( ! -name . \) -name "*-dependencies" -exec bash -c "cd '{}' && mvn versions:set -DnewVersion=$1" \;
find . -maxdepth 1 -type d \( ! -name . \) -exec bash -c "cd '{}' && mvn versions:set -DnewVersion=$1" \;
find . -maxdepth 1 -type d \( ! -name . \) -exec bash -c "cd '{}' && mvn versions:commit" \;