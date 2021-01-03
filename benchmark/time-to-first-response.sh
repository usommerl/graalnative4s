#!/usr/bin/env bash

set -o nounset

while getopts s:c: opt
do
  case $opt in
    s) SERVER_COMMAND=$OPTARG;;
    c) CLIENT_COMMAND=$OPTARG;;
  esac
done

main() {
  local server_pid
  local tries=500

  ./$SERVER_COMMAND &
  server_pid=$!

  $CLIENT_COMMAND
  while [ "$?" -ne 0 ]; do
    ((tries--))
    if [ "$tries" -eq 0 ]; then
      echo "Server not started within timeout"
      exit 1
    fi
    $CLIENT_COMMAND
  done
  kill ${server_pid}
}

main
exit 0
