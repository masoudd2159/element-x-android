#!/usr/bin/env bash
# Run this script inside a Git project: bash ./export-codex-changes.sh [BASE]
# Examples: bash ./export-codex-changes.sh    or    bash ./export-codex-changes.sh HEAD~1
set -euo pipefail

base="${1:-HEAD}"
root="$(git rev-parse --show-toplevel)" || {
  echo 'Run this script inside a Git repository.' >&2
  exit 1
}
cd "$root"
if ! git rev-parse --verify "${base}^{commit}" >/dev/null 2>&1; then
  echo "Invalid base revision: $base" >&2
  exit 1
fi

project_name="$(basename "$root")"
stamp="$(date +%Y%m%d-%H%M%S)"
dest="$(dirname "$root")/codex-changes-$project_name-$stamp"
zip_path="$dest.zip"
mkdir -p "$dest"

# Script itself is excluded if it was copied into the project for convenience.
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
script_path="$script_dir/$(basename "${BASH_SOURCE[0]}")"
copied=0

# NUL separators safely handle spaces and unusual filenames.
while IFS= read -r -d '' relative_path; do
  [[ -f "$relative_path" ]] || continue
  [[ "$root/$relative_path" == "$script_path" ]] && continue
  mkdir -p "$dest/$(dirname "$relative_path")"
  cp "./$relative_path" "$dest/$relative_path"
  copied=$((copied + 1))
done < <(
  git diff --name-only --diff-filter=ACMRT -z "$base"
  git ls-files --others --exclude-standard -z
)

# Preserves staged/unstaged changes and deletions in a binary-safe patch.
git diff --binary --no-ext-diff --output="$dest/changes.patch" "$base"
git -c core.quotePath=false status --short --untracked-files=all > "$dest/git-status.txt"
git -c core.quotePath=false diff --name-status "$base" > "$dest/changed-files.txt"
printf 'Base revision: %s\nProject: %s\nCreated: %s\n' "$base" "$root" "$(date '+%Y-%m-%dT%H:%M:%S')" > "$dest/README.txt"

# ditto is built into macOS. Fall back to zip for non-macOS testing.
if command -v ditto >/dev/null 2>&1; then
  ditto -c -k --keepParent "$dest" "$zip_path"
else
  (cd "$(dirname "$dest")" && zip -qr "$zip_path" "$(basename "$dest")")
fi

printf '\nExported %s current file entries.\nZIP: %s\n' "$copied" "$zip_path"
echo 'Check the ZIP for secrets (tokens, passwords, keys) before sharing.'
