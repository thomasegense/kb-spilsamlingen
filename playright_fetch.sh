#!/usr/bin/env bash
#
# playright_fetch.sh — resolve and download Playright.dk cover art for the
# round-23 backlog (64 catalog IDs whose Playright-ID lead was blocked by a
# live 503 outage in the cloud session). Run this directly on your own
# machine, where the outage may have cleared and network conditions differ.
#
# Recipe (established across rounds 20-23 of the image-sourcing project):
#   1. GET https://www.playright.dk/info/titel/<slug>/<platform>
#   2. Pull the real cover URL out of the HTML: it lives on
#      media.playright.dk/cover/.../<slug>@<size>min.jpg — pages with no
#      real cover serve cover_dummy.png instead, which we explicitly skip.
#   3. Download that image and save it as <catalogID>_1.<ext>, where <ext>
#      is taken from the file's actual bytes (not the URL), since some CDNs
#      serve a different format than their URL implies.
#
# Usage:
#   ./playright_fetch.sh [output_dir] [max_retry_passes]
#
#   output_dir        Where to save images. Default: ./images
#                      Point this at your
#                      kb-spilsamlingen/src/main/webapp/images/ folder to
#                      drop files straight into place.
#   max_retry_passes  How many times to retry entries that hit the 503
#                      outage page before giving up on them. Default: 4.
#
# Already-present files (same catalog ID, any extension) are skipped, so
# it's safe to re-run this after a partial run or after the images folder
# has moved on.

set -uo pipefail

OUTDIR="${1:-./images}"
MAX_PASSES="${2:-4}"
mkdir -p "$OUTDIR"

UA="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36"

# catalog_id|slug/platform[, slug/platform...]
ENTRIES="
10|dava-tronic-video-sport/png
49|diamond-hunt/lcd
55|deputy-den/lcd
73|diamond-hunt/lcd
292|kaptajn-kaper-i-kattegat/pc
718|last-inca-the-part-ii-and-the-quest-of-fate/ami
781|airlines/pc
821|jimmys-fantastic-journey/ami
867|hugo-paa-nye-eventyr/pc
915|hugo-classic-2/pc
930|pixeline-2-hulen-i-traeet/pc
948|survival-1996/pc
993|hugo-classic-3/pc
1011|pixeline-3-paa-bedstemors-loft/pc
1016|rasmus-klump-og-hans-venner-leger-og-laerer/pc
1067|hugo-1998/ps1
1075|jul-paa-slottet/pc
1190|lego-friends-1999/pc
1263|disneys-dinosaurerne-sjov-med-spil/pc
1277|grand-theft-auto-london-1969/pc
1291|hugo-the-quest-for-the-sunstones/pc
1292|hugo-the-quest-for-the-sunstones/ps1
1303|magnus-+-myggen-hold-da-helt-ferie/pc
1324|pro-rally-2001
1327|pyrus-i-alletiders-eventyr/pc
1354|alvin-+-ugiugi-mosters-mosteri/pc
1372|emperors-new-groove-the/pc
1386|hugo-black-diamond-fever/pc
1387|hugo-black-diamond-fever/ps1
1395|jens-marius-+-jagten-paa-de-forsvundne-boksnoegler/pc
1400|lego-creator-harry-potter/pc
1414|nissebanden/pc
1416|off-piste/pc
1423|paa-opdagelse-med-peter-og-marie-bondegaarden/pc
1434|spil-med-kaj-og-andrea/pc
1445|x-gold/pc
1453|bamses-allerskoereste-spille-rom/pc
1467|brainstorm-the-game-show/pc
1496|lilo-+-stitch-trouble-in-paradise/pc
1497|peter-pan-return-to-never-land/pc
1515|hugo-the-evil-mirror/gbc
1516|hugo-the-evil-mirror/gba
1517|hugo-the-evil-mirror/ps1
1568|spil-med-i-broedrene-mortensens-jul/pc
1619|fim-speedway-grand-prix/pc
1637|hugo-bukkazoom/pc
1639|hugo-bukkazoom/ps2
1643|hugo-the-evil-mirror/pc
1700|vietnam-ho-chi-minh-trail/pc
1702|wrc-3/ps2
1703|112-rescue-copter/pc
1746|hugo-cannon-cruise/ps2
1777|pyrus-alletiders-jul/pc
1862|hugo-cannon-cruise/pc
1929|war-world-tactical-combat/pc
1935|agent-hugo-2-robo-rumble/ps2
1936|agent-hugo-2-robo-rumble/pc
1937|agent-hugo-2-robo-rumble/gba
2051|seed/pc
2071|koala-brothers-the-outback-adventures/gba
2072|land-before-time-the-into-the-mysterious-beyond/gba
5196|not-a-hero/ps4
5703|freedom-planet/ps4
5814|lego-star-wars-microfighters/ipd, lego-star-wars-microfighters/ip
"

declare -a PENDING_IDS
declare -A SLUGS_BY_ID
declare -a DUMMY_IDS
declare -a OK_IDS

already_have() {
  local id="$1"
  compgen -G "$OUTDIR/${id}_1.*" > /dev/null 2>&1
}

# Try every slug variant for one catalog id. Echoes "OK", "DUMMY", or "RETRY".
try_id() {
  local id="$1" slugs="$2"
  local IFS=','
  local -a slug_list
  read -ra slug_list <<< "$slugs"
  local saw_outage=0

  for raw in "${slug_list[@]}"; do
    local slug
    slug="$(echo "$raw" | xargs)"
    [ -z "$slug" ] && continue

    local page_url="https://www.playright.dk/info/titel/${slug}"
    local html
    html="$(curl -sS -L -A "$UA" --max-time 20 "$page_url" 2>/dev/null)"

    if [ -z "$html" ]; then
      saw_outage=1
      continue
    fi
    if echo "$html" | grep -qi "Midlertidig nedetid\|Temporary down"; then
      saw_outage=1
      continue
    fi

    local img_url
    img_url="$(echo "$html" | grep -oE 'https://media\.playright\.dk/cover/[^"'"'"' ]+\.(jpg|png|jpeg)' | grep -v cover_dummy | head -1)"

    if [ -z "$img_url" ]; then
      # Page loaded fine but no real cover for this slug variant — try next
      # variant if any, otherwise this id is a confirmed dummy/no-cover.
      continue
    fi

    local tmpfile
    tmpfile="$(mktemp)"
    curl -sS -L -A "$UA" --max-time 30 -o "$tmpfile" "$img_url" 2>/dev/null

    if [ ! -s "$tmpfile" ]; then
      rm -f "$tmpfile"
      continue
    fi

    local mime ext
    mime="$(file --brief --mime-type "$tmpfile" 2>/dev/null)"
    ext="${mime#image/}"
    case "$ext" in
      jpeg) ext="jpg" ;;
      jpg|png|gif|webp) ;;
      *) ext="jpg" ;;
    esac

    mv "$tmpfile" "$OUTDIR/${id}_1.${ext}"
    echo "OK"
    return 0
  done

  if [ "$saw_outage" -eq 1 ]; then
    echo "RETRY"
  else
    echo "DUMMY"
  fi
  return 0
}

echo "=== Pass 1 ==="
while IFS='|' read -r id slugs; do
  [ -z "${id:-}" ] && continue
  if already_have "$id"; then
    echo "[$id] already present, skipping"
    continue
  fi
  result="$(try_id "$id" "$slugs")"
  case "$result" in
    OK)    echo "[$id] -> downloaded";              OK_IDS+=("$id") ;;
    DUMMY) echo "[$id] -> no real cover (dummy)";    DUMMY_IDS+=("$id") ;;
    RETRY) echo "[$id] -> 503 outage, will retry";   PENDING_IDS+=("$id"); SLUGS_BY_ID["$id"]="$slugs" ;;
  esac
done <<< "$ENTRIES"

pass=1
while [ "${#PENDING_IDS[@]}" -gt 0 ] && [ "$pass" -lt "$MAX_PASSES" ]; do
  pass=$((pass+1))
  echo ""
  echo "=== Pass $pass (${#PENDING_IDS[@]} pending) ==="
  sleep 3
  declare -a still_pending
  for id in "${PENDING_IDS[@]}"; do
    result="$(try_id "$id" "${SLUGS_BY_ID[$id]}")"
    case "$result" in
      OK)    echo "[$id] -> downloaded";           OK_IDS+=("$id") ;;
      DUMMY) echo "[$id] -> no real cover (dummy)"; DUMMY_IDS+=("$id") ;;
      RETRY) echo "[$id] -> still 503, retrying later"; still_pending+=("$id") ;;
    esac
  done
  PENDING_IDS=("${still_pending[@]}")
done

echo ""
echo "================= SUMMARY ================="
echo "Downloaded: ${#OK_IDS[@]}  -> ${OK_IDS[*]:-none}"
echo "Confirmed no cover (dummy): ${#DUMMY_IDS[@]}  -> ${DUMMY_IDS[*]:-none}"
echo "Still blocked after $((pass)) passes: ${#PENDING_IDS[@]}  -> ${PENDING_IDS[*]:-none}"
echo "Files saved under: $OUTDIR"
