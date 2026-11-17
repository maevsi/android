#!/usr/bin/env bash
# Runs play-notes.sh against a throwaway repository.
set -euo pipefail
export LC_ALL=C.UTF-8

script=$(realpath "$(dirname "$0")/play-notes.sh")
cd "$(mktemp -d)"
git init -q
git config user.email test@example.com
git config user.name test
unset RELEASE_NOTES_DE RELEASE_NOTES_EN

fragment() {
  mkdir -p "play-notes/$1"
  printf '%s\n' "$2" >"play-notes/$1/de-DE.txt"
  printf '%s\n' "$3" >"play-notes/$1/en-US.txt"
  git add play-notes
  git commit -qm "$1"
}
expect() {
  [ "$(<"app/src/main/play/release-notes/$1/default.txt")" = "$2" ] || {
    echo "unexpected $1 notes: $(<"app/src/main/play/release-notes/$1/default.txt")"
    exit 1
  }
}
fails() {
  if "$script" "$@" 2>/dev/null; then
    echo "expected failure: $*"
    exit 1
  fi
}

fragment old 'Alt' 'Old'
git tag 1.0.0
fragment splash 'Der Startbildschirm ist jetzt hell.' 'The start screen is light now.'
git tag 1.0.1
git commit -q --allow-empty -m deps
git tag 1.0.2
fragment umlaut "$(printf 'ä%.0s' {1..150})" 'Umlauts count as one character.'
git tag 1.1.0

# Fragments of every release since the one in production, oldest first.
"$script" write 1.0.0 1.1.0
expect de-DE "• Der Startbildschirm ist jetzt hell."$'\n'"• $(printf 'ä%.0s' {1..150})"
expect en-US $'• The start screen is light now.\n• Umlauts count as one character.'

# A release without fragments writes nothing, and overrides replace collected notes.
rm -r app
"$script" write 1.0.1 1.0.2
[ ! -e app ] || { echo 'expected no notes'; exit 1; }
RELEASE_NOTES_DE='Neu' RELEASE_NOTES_EN='New' "$script" write 1.0.1 1.0.2
expect de-DE 'Neu'
RELEASE_NOTES_DE='Neu' fails write 1.0.1 1.0.2
RELEASE_NOTES_DE=$(printf 'x%.0s' {1..501}) RELEASE_NOTES_EN='New' fails write 1.0.1 1.0.2
fails write 0.9.0 1.1.0

# Pull requests that release need a fragment, and every fragment needs both locales.
check_pr() { BASE=$1 HEAD=$2 TITLE=$3 AUTHOR=someone SKIP=${4:-false} "$script" pr; }
check_pr 1.0.1 1.0.2 'chore(ci): tidy up'
check_pr 1.0.1 1.0.2 'fix(deps): update' true
check_pr 1.0.2 1.1.0 'feat(ui): add umlauts'
BASE=1.0.1 HEAD=1.0.2 TITLE='fix(deps): update' AUTHOR='renovate[bot]' SKIP=false "$script" pr
BASE=1.0.1 HEAD=1.0.2 TITLE='fix(ui): change color' AUTHOR=someone SKIP=false fails pr
mkdir play-notes/broken
printf 'Kaputt\n' >play-notes/broken/de-DE.txt
fails check play-notes/broken
printf 'One\nTwo\n' >play-notes/broken/en-US.txt
fails check play-notes/broken
printf '%s\n' "$(printf 'x%.0s' {1..151})" >play-notes/broken/en-US.txt
fails check play-notes/broken

echo ok
