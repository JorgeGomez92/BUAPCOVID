#!/usr/bin/env bash

set -euo pipefail

DATA=$(cat <<EOF
{
  "event_type":"generate-release-note",
  "client_payload": {
    "revision_range": "$1"
  }
}
EOF
)

echo 'creating dispatch event'
echo "$DATA"

curl \
  --verbose \
  --url 'https://api.github.com/repos/nhsx/sonar-colocate-android/dispatches' \
  --header "Content-type: application/json" \
  --header "Authorization: token $GITHUB_USER_TOKEN" \
  --data "$DATA"

echo
echo

echo 'The report is viewable here: https://github.com/nhsx/sonar-colocate-android/actions?query=workflow%3Agenerate-release-note'
