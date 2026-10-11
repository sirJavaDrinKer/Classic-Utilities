#!/bin/bash
shopt -s nullglob

rm -rf collect
mkdir -p collect/common collect/sources collect/dev

for j in ./*/build/libs/*.jar; do
	cp -v "$j" collect/
done

mv collect/*sources*.jar collect/sources/ 2>/dev/null
mv collect/*-core-*.jar  collect/common/  2>/dev/null
mv collect/*-dev.jar     collect/dev/     2>/dev/null

echo
echo "=============================================================="
mods=(collect/*.jar)
if [ ${#mods[@]} -eq 0 ]; then
	echo "ERROR: no mod jar was built. Check the gradle output for errors in Forge-1.4.7."
	echo "(collect/common/ only has the Core library; that is NOT a mod.)"
	exit 1
fi

found=0
for j in "${mods[@]}"; do
	# A real Forge mod jar always has an mcmod.info and a @Mod entrypoint class.
	if unzip -l "$j" | grep -q 'mcmod.info' && unzip -l "$j" | grep -q 'ForgeEntrypoint'; then
		echo "INSTALL THIS ONE  ->  $j"
		found=1
	else
		echo "not a mod (missing mcmod.info / entrypoint): $j"
	fi
done

echo
echo "Do NOT install anything from collect/common (Core library, already bundled into the mod jar)"
echo "or collect/dev (deobfuscated dev build, only works inside the dev environment)."
[ $found -eq 1 ] || { echo "ERROR: no installable mod jar found."; exit 1; }
