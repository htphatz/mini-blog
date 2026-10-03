#!/bin/bash
# PreToolUse: block edits to real .env files (.env.example stays editable).
input=$(cat)
file=$(printf '%s' "$input" | python3 -c 'import sys,json; print(json.load(sys.stdin).get("tool_input",{}).get("file_path",""))' 2>/dev/null | tr '\\' '/')
name=${file##*/}
case "$name" in
  .env.example) exit 0 ;;
  .env|.env.*) echo "Blocked: do not edit $name (secrets). Edit .env.example instead." >&2; exit 2 ;;
esac
exit 0
