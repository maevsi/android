#!/usr/bin/env bash
# Checks the Google Play release notes that pull requests add under play-notes/, and assembles them
# into the files Gradle Play Publisher reads. See CONTRIBUTING.md.
set -euo pipefail
# ${#var} counts bytes in the C locale, which would count each umlaut twice.
export LC_ALL=C.UTF-8

locales=(de-DE en-US)
fragment_limit=150
notes_limit=500
notes_dir=app/src/main/play/release-notes

fail() {
  echo "::error::$*" >&2
  exit 1
}

# check DIR...: each fragment holds exactly one line per locale and nothing else.
check() {
  local dir file locale text
  for dir in "$@"; do
    for file in "$dir"/*; do
      case "${file##*/}" in
        de-DE.txt | en-US.txt) ;;
        *) fail "$file is neither de-DE.txt nor en-US.txt." ;;
      esac
    done
    for locale in "${locales[@]}"; do
      file="$dir/$locale.txt"
      [ -f "$file" ] || fail "$dir lacks $locale.txt."
      text=$(<"$file")
      [ -n "$text" ] || fail "$file is empty."
      [[ $text != *$'\n'* ]] || fail "$file holds more than one line."
      [ "${#text}" -le "$fragment_limit" ] || fail "$file holds ${#text} characters, more than $fragment_limit."
    done
  done
}

# pr: checks the fragments a pull request touches, and requires one for releasing changes.
# Reads BASE, HEAD, TITLE, AUTHOR, and SKIP from the environment.
pr() {
  local list touched=() added
  # Process substitution would swallow a failing git diff, so the list goes through a variable.
  list=$(git diff --diff-filter=d --name-only "$BASE...$HEAD" -- play-notes/ | sed 's|/[^/]*$||' | sort -u)
  [ -z "$list" ] || mapfile -t touched <<<"$list"
  [ "${#touched[@]}" -eq 0 ] || check "${touched[@]}"
  if [[ $TITLE =~ ^(feat|fix|perf)(\(.*\))?!?: ]] && [ "$SKIP" != true ] && [ "$AUTHOR" != 'renovate[bot]' ]; then
    added=$(git diff --diff-filter=A --name-only "$BASE...$HEAD" -- play-notes/)
    [ -n "$added" ] || fail "This pull request triggers a release, so it needs Google Play release notes in play-notes/<slug>/de-DE.txt and en-US.txt, or the no-play-notes label if users notice nothing. See CONTRIBUTING.md."
  fi
}

# collect FROM TO: prints the fragment directories added after tag FROM up to tag TO, oldest first.
collect() {
  local ref
  for ref in "$1" "$2"; do
    git rev-parse -q --verify "refs/tags/$ref" >/dev/null || fail "There is no tag $ref."
  done
  git log --reverse --diff-filter=A --name-only --format= "refs/tags/$1..refs/tags/$2" -- play-notes/ |
    sed 's|/[^/]*$||' | awk '!seen[$0]++' | while read -r dir; do
      # A fragment deleted again before TO no longer describes the release.
      if [ -d "$dir" ]; then echo "$dir"; fi
    done
}

# write FROM TO: writes the release notes for every locale from the fragments between the tags,
# unless RELEASE_NOTES_DE or RELEASE_NOTES_EN replaces them. Writes nothing when there are none.
write() {
  local list dirs=() dir locale notes written=0
  list=$(collect "$1" "$2")
  [ -z "$list" ] || mapfile -t dirs <<<"$list"
  [ "${#dirs[@]}" -eq 0 ] || check "${dirs[@]}"
  for locale in "${locales[@]}"; do
    case "$locale" in
      de-DE) notes=${RELEASE_NOTES_DE:-} ;;
      en-US) notes=${RELEASE_NOTES_EN:-} ;;
    esac
    if [ -z "$notes" ] && [ "${#dirs[@]}" -gt 0 ]; then
      notes=$(for dir in "${dirs[@]}"; do printf '• %s\n' "$(<"$dir/$locale.txt")"; done)
    fi
    [ -n "$notes" ] || continue
    [ "${#notes}" -le "$notes_limit" ] || fail "The $locale release notes hold ${#notes} characters, more than $notes_limit. Pass shorter ones to the Promote workflow."
    mkdir -p "$notes_dir/$locale"
    printf '%s' "$notes" >"$notes_dir/$locale/default.txt"
    written=$((written + 1))
  done
  [ "$written" -eq 0 ] || [ "$written" -eq "${#locales[@]}" ] || fail "Release notes exist for some locales only. Pass both to the Promote workflow."
}

# version TRACK: prints the version of the live release on a Google Play track.
# Reads ACCESS_TOKEN and PACKAGE_NAME from the environment.
version() {
  local api="https://androidpublisher.googleapis.com/androidpublisher/v3/applications/$PACKAGE_NAME/edits"
  local auth=(-H "Authorization: Bearer $ACCESS_TOKEN")
  local edit name version
  edit=$(curl -fsS -X POST "${auth[@]}" "$api" | jq -r .id)
  name=$(curl -fsS "${auth[@]}" "$api/$edit/tracks/$1" |
    jq -r '.releases // [] | (map(select(.status == "inProgress")) + map(select(.status == "completed")))[0].name // empty')
  curl -fsS -X DELETE "${auth[@]}" "$api/$edit" >/dev/null || true
  # Google Play names a release after its versionName unless told otherwise, sometimes with the versionCode in front.
  version=$(grep -oE '[0-9]+\.[0-9]+\.[0-9]+' <<<"$name" | tail -n1 || true)
  [ -n "$version" ] || fail "The live release on Google Play's $1 track has no version in its name '$name'."
  echo "$version"
}

case "${1:-}" in
  check | collect | pr | version | write)
    command=$1
    shift
    "$command" "$@"
    ;;
  *) fail "Usage: $0 check DIR... | collect FROM TO | pr | version TRACK | write FROM TO" ;;
esac
