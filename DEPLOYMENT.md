# Deployment Playbook — Bank Highlight Search

How this plugin reaches users, from first submission to routine updates.

## How the Plugin Hub works (mental model)

The hub repo (`runelite/plugin-hub`) holds one tiny manifest file per plugin:

```properties
# plugins/bank-highlight-search
repository=https://github.com/<you>/Highlight-Search.git
commit=<full 40-char SHA>
```

That's the entire deployment surface. The hub's CI checks out *your* repo at
*that* SHA, builds it with their hardened template (`build=standard` replaces
our `build.gradle`), and on merge signs + publishes the jar. Users get the
update automatically the next time the hub manifest is rebuilt — typically
within hours of merge.

So a "deploy" is exactly one PR: change `commit=` in that one file. There is
no second PR. Your plugin repo itself never needs PRs — you own it, push to
master directly.

```
code change → push to master → (test!) → hub PR bumping commit= → maintainer merge → live
```

## One-time setup

1. **Create the public GitHub repo and push** (from this directory):

   ```bash
   gh repo create Highlight-Search --public --source=. --push
   ```

2. **Optional but recommended:** add `icon.png` (max 48×72px) at repo root —
   shown in the hub listing. Commit it before submitting.

3. **First submission** (the approval step you mentioned):

   ```bash
   deploy/deploy.sh
   ```

   The script auto-detects that `plugins/bank-highlight-search` doesn't exist
   upstream and opens an **Add** PR: forks `runelite/plugin-hub`, writes the
   manifest file with `repository=` + current HEAD SHA, pushes a branch,
   opens the PR via `gh`.

4. **Review:** a human RuneLite maintainer reviews the actual plugin code for
   malice, Jagex third-party-client rule violations, and overlap with their
   Rejected/Rolled-Back features list. This plugin is presentation-only with
   core-plugin precedent for every game-state write, so it's low-risk — but
   expect days-to-weeks and possibly change requests. Respond by pushing
   fixes to the plugin repo and updating the PR's `commit=` to the new SHA.

## Routine update deployment (after approval)

```bash
# 1. land your changes
git push                  # plugin repo, master, no PR needed

# 2. ship
deploy/deploy.sh          # opens the bump PR
```

The script refuses to run with a dirty tree or unpushed HEAD, builds locally
first (`--skip-build` to skip), then opens a PR titled
`Update Bank Highlight Search to <sha>` containing only the `commit=` change.
Hub CI ("RuneLite Plugin Hub Checks") must pass; a maintainer merges; users
have it within hours. Update PRs are reviewed much faster than submissions —
maintainers diff your plugin repo between the old and new SHA.

## Automatic deployment

`.github/workflows/hub-bump.yml` automates the bump PR. **Setup once:**

1. Create a classic PAT (`public_repo` scope) on your GitHub account:
   <https://github.com/settings/tokens>
2. In the plugin repo: Settings → Secrets and variables → Actions →
   New repository secret → name `HUB_PAT`, paste the token.

**Then to deploy, either:**

- **Tag a release** (recommended ritual — makes deploys deliberate):

  ```bash
  git tag v1.1.0 && git push origin v1.1.0
  ```

- **Or** click *Run workflow* on the "Plugin Hub bump PR" action in the
  GitHub UI (workflow_dispatch).

Either trigger builds the plugin on CI, then runs `deploy/deploy.sh --yes`
to open the bump PR from your fork.

**Why not auto-deploy every push to master?** Each hub PR notifies human
maintainers. Untested auto-bumps burn goodwill and review bandwidth — gate
deploys behind the tag so a bump always means "I ran it in the client".
Spam is also the fastest way to get slower reviews.

You can also trigger from this machine without GitHub Actions at all —
`deploy/deploy.sh` is self-contained (needs `gh` authed + clean pushed tree).

## After the client updates (maintenance you didn't ask for but will hit)

RuneLite revs weekly. `build.gradle` tracks `runelite-client:latest.release`,
so local builds always compile against current. When a game/client update
breaks the plugin (renamed component, changed script), users see it before
you do — fix, test, tag, bump. The hub pins its own RuneLite version
(`plugin-hub/runelite.version`); occasionally a hub PR fails CI because their
pin moved before/after yours — rebuild against latest and re-push.

## Troubleshooting

| Symptom | Cause / fix |
|---|---|
| Hub CI fails, local build green | Hub builds with their template + pinned RuneLite version. Check the `manifest_diff` artifact on the PR; usually an API change in latest.release — fix code, push, update `commit=` on the same PR branch. |
| `deploy.sh: HEAD not pushed` | `git push` first. The manifest SHA must be reachable on the public repo. |
| PR rejected: "difficult to verify" | Keep the diff small and reviewable; never add reflection, shading, or network calls without a `warning=` line in the manifest. |
| Fork out of date / push rejected | The script clones upstream fresh and pushes a new branch to the fork each run — stale forks don't matter. Delete merged branches occasionally. |
| Need to pull the plugin from the hub | PR deleting `plugins/bank-highlight-search`. |

## File map

| File | Role |
|---|---|
| `deploy/deploy.sh` | One script, both modes: initial **Add** PR / routine **bump** PR. `--yes` for CI, `--skip-build` to skip the local gradle check. |
| `.github/workflows/hub-bump.yml` | Tag- or button-triggered automation around the script. Needs `HUB_PAT` secret. |
| `runelite-plugin.properties` | Plugin metadata the hub reads at build time (display name, author, tags, main class). |
