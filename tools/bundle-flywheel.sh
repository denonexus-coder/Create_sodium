#!/usr/bin/env bash
#
# Replace the Flywheel that ships inside a built Create jar with a jar built from a
# different Flywheel fork.
#
# Create consumes Flywheel through Jar-in-Jar: whatever Flywheel jar was on the compile
# classpath ends up as one entry under META-INF/jars/ inside Create's own jar, and Fabric
# discovers it there at runtime. Overwriting that single entry is therefore all it takes
# to make Create run a Flywheel that is not published on any Maven repository, without
# touching Create's build and without recompiling Create against it.
#
# usage: tools/bundle-flywheel.sh <create.jar> <flywheel.jar> [output.jar]
#
set -euo pipefail

if [ $# -lt 2 ]; then
	echo "usage: $0 <create.jar> <flywheel.jar> [output.jar]" >&2
	exit 2
fi

create_jar=$1
flywheel_jar=$2
out_jar=${3:-"${create_jar%.jar}+restyled-flywheel.jar"}

[ -f "$create_jar" ] || { echo "no such file: $create_jar" >&2; exit 1; }
[ -f "$flywheel_jar" ] || { echo "no such file: $flywheel_jar" >&2; exit 1; }

# Refuse to bundle something that is not actually Flywheel.
if ! unzip -p "$flywheel_jar" fabric.mod.json | grep -q '"id" *: *"flywheel"'; then
	echo "$flywheel_jar is not a Flywheel mod jar" >&2
	exit 1
fi

entry=$(unzip -Z1 "$create_jar" | grep -E '^META-INF/jars/flywheel[^/]*\.jar$' || true)
if [ -z "$entry" ]; then
	echo "$create_jar does not bundle a Flywheel jar" >&2
	exit 1
fi
if [ "$(printf '%s\n' "$entry" | wc -l)" -ne 1 ]; then
	echo "$create_jar bundles more than one Flywheel jar:" >&2
	printf '  %s\n' $entry >&2
	exit 1
fi

out_jar=$(realpath -m "$out_jar")

work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT

unzip -q "$create_jar" -d "$work"
cp "$flywheel_jar" "$work/$entry"

rm -f "$out_jar"
( cd "$work" && zip -q -r -X "$out_jar" . )

echo "replaced $entry"
echo "  with $(basename "$flywheel_jar")"
echo "  in   $create_jar"
echo "  out  $out_jar"
