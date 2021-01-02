#!/usr/bin/env bash

set -o nounset

while getopts p: opt
do
  case $opt in
    p) PREFIX=$OPTARG;;
  esac
done

main() {
  local server_command server_pid bytes suffix
  printf '| Binary | Memory [bytes] |\n|:---|---:|\n'

  for suffix in no-upx upx-1 upx-2 upx-3 upx-4 upx-5 upx-6 upx-7 upx-8 upx-9 upx-best upx-brute upx-ultra-brute; do
    server_command="$PREFIX-$suffix"
    ./$server_command &> /dev/null &
    server_pid=$!
    sleep 2
    bytes="$(ps_mem -t -p $server_pid)"
    printf "| %-25s | %10s |\n" $server_command $bytes
    kill ${server_pid}
  done
}

main
exit 0
