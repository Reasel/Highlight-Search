#!/usr/bin/env bash
# Opens (or updates) the Plugin Hub PR for this plugin.
#
# Auto-detects mode:
#   - plugins/<name> missing upstream  -> initial submission PR
#   - plugins/<name> exists upstream   -> bump PR updating commit= to local HEAD
#
# Requirements: gh (authenticated, or GH_TOKEN set), git, java 11 or nix.
# Usage: deploy/deploy.sh [--yes] [--skip-build]
#   --yes         don't prompt for confirmation (CI)
#   --skip-build  skip the local gradle build check

set -euo pipefail

PLUGIN_NAME="bank-highlight-search"
PLUGIN_TITLE="Bank Highlight Search"
HUB_REPO="runelite/plugin-hub"

YES=0
SKIP_BUILD=0
for arg in "$@"; do
	case "$arg" in
		--yes) YES=1 ;;
		--skip-build) SKIP_BUILD=1 ;;
		*) echo "unknown arg: $arg" >&2; exit 2 ;;
	esac
done

repo_root=$(git rev-parse --show-toplevel)
cd "$repo_root"

# ---- preflight ------------------------------------------------------------
if [[ -n $(git status --porcelain) ]]; then
	echo "ERROR: working tree dirty — commit or stash first" >&2
	exit 1
fi

sha=$(git rev-parse HEAD)
short=${sha:0:8}

origin_url=$(git remote get-url origin 2>/dev/null) || {
	echo "ERROR: no 'origin' remote. Create the GitHub repo and push first:" >&2
	echo "  gh repo create $PLUGIN_NAME --public --source=. --push" >&2
	exit 1
}
# normalize to the https .git form the hub expects
https_url=$(echo "$origin_url" | sed -E 's#^git@github\.com:#https://github.com/#; s#\.git$##').git

git fetch origin --quiet
if [[ $(git rev-parse origin/master 2>/dev/null || git rev-parse origin/main) != "$sha" ]]; then
	echo "ERROR: HEAD ($short) is not pushed to origin — run: git push" >&2
	exit 1
fi

if [[ $SKIP_BUILD -eq 0 ]]; then
	echo "==> verifying build at $short"
	if command -v java >/dev/null; then
		./gradlew build --quiet
	else
		nix-shell -p jdk11 --run './gradlew build --quiet'
	fi
fi

# ---- hub fork + branch -----------------------------------------------------
gh_user=$(gh api user -q .login)
# let git push authenticate through gh (keyring locally, GH_TOKEN in CI)
gh auth setup-git >/dev/null 2>&1 || true
echo "==> ensuring fork $gh_user/plugin-hub exists"
gh repo fork "$HUB_REPO" --clone=false >/dev/null 2>&1 || true

work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
echo "==> cloning upstream hub"
git clone --quiet --depth 1 "https://github.com/$HUB_REPO.git" "$work"
cd "$work"
# OAuth tokens without 'workflow' scope can't push upstream's workflow files
# to a fresh fork over https; prefer ssh locally, keep https for CI (GH_TOKEN)
if [[ -z ${GH_TOKEN:-} && $(gh config get git_protocol 2>/dev/null) == ssh ]]; then
	fork_url="git@github.com:$gh_user/plugin-hub.git"
else
	fork_url="https://github.com/$gh_user/plugin-hub.git"
fi
git remote add fork "$fork_url"

hub_file="plugins/$PLUGIN_NAME"
if [[ -f $hub_file ]]; then
	mode="bump"
	version=$(sed -n 's/^version=//p' "$repo_root/runelite-plugin.properties")
	branch="update-$PLUGIN_NAME-$short"
	title="Update $PLUGIN_TITLE to ${version:-$short}"
	body="Updates $PLUGIN_TITLE to https://github.com/${https_url#https://github.com/}"
	body="${body%.git}/commit/$sha"
else
	mode="submit"
	branch="add-$PLUGIN_NAME"
	title="Add $PLUGIN_TITLE"
	body=$(cat <<-EOF
	Adds $PLUGIN_TITLE: search the bank with a hotkey and highlight matching
	items (blinking/feathered outline) instead of filtering them out; Enter
	switches to the ALL tab and scrolls to where the most matches are visible.

	Repository: ${https_url%.git}
	EOF
	)
fi

echo "==> mode: $mode"
git checkout --quiet -b "$branch"
{
	echo "repository=$https_url"
	echo "commit=$sha"
} > "$hub_file"
git add "$hub_file"
git commit --quiet -m "$title"

echo
echo "---- $hub_file ----"
cat "$hub_file"
echo "-------------------"
if [[ $YES -eq 0 ]]; then
	read -rp "Push branch '$branch' to $gh_user/plugin-hub and open PR against $HUB_REPO? [y/N] " ok
	[[ $ok == y || $ok == Y ]] || { echo "aborted"; exit 1; }
fi

git push --quiet -u fork "$branch"
pr_url=$(gh pr create --repo "$HUB_REPO" --head "$gh_user:$branch" --title "$title" --body "$body")
echo "==> PR opened: $pr_url"
