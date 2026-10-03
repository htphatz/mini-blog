#!/bin/bash
# PostToolUse: after a .java edit, compile only the affected service so errors surface immediately.
input=$(cat)
file=$(printf '%s' "$input" | python3 -c 'import sys,json; print(json.load(sys.stdin).get("tool_input",{}).get("file_path",""))' 2>/dev/null | tr '\\' '/')
case "$file" in *.java) ;; *) exit 0 ;; esac

root=$(printf '%s' "${CLAUDE_PROJECT_DIR:-$(pwd)}" | tr '\\' '/')
rel=${file#"$root"/}
service=${rel%%/*}
[ -f "$root/$service/mvnw" ] || exit 0

out=$(cd "$root/$service" && ./mvnw -q -o compile 2>&1)
if [ $? -ne 0 ]; then
  echo "Compile failed in $service:" >&2
  echo "$out" | grep -E "ERROR|error:" | head -20 >&2
  exit 2
fi
exit 0
