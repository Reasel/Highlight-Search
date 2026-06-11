# Deployment — Bank Highlight Search

The hub repo (`runelite/plugin-hub`) holds one manifest file per plugin:

```properties
# plugins/bank-highlight-search
repository=https://github.com/<you>/Highlight-Search.git
commit=<full 40-char SHA>
```

A deploy is exactly one PR changing `commit=` in that file. Hub CI checks out
this repo at that SHA, builds it with their hardened template (replacing our
`build.gradle`), and on maintainer merge signs + publishes the jar. Users get
it within hours. This repo itself needs no PRs — push to master directly.

```
code change → push to master → (test!) → hub PR bumping commit= → maintainer merge → live
```

## First submission

1. `gh repo create Highlight-Search --public --source=. --push`
2. `deploy/deploy.sh` — detects that `plugins/bank-highlight-search` doesn't
   exist upstream and opens an **Add** PR (forks the hub, writes the manifest,
   pushes a branch, opens the PR via `gh`).
3. A maintainer reviews the plugin code for malice, Jagex third-party rule
   violations, and overlap with the Rejected/Rolled-Back list. Expect
   days-to-weeks. Respond to change requests by pushing fixes here and
   updating the PR's `commit=` to the new SHA.

## Routine update

```bash
git push           # plugin repo, master
deploy/deploy.sh   # opens the bump PR
```

The script refuses a dirty tree or unpushed HEAD, builds locally first
(`--skip-build` to skip), then opens a PR containing only the `commit=`
change. Update PRs review much faster than submissions.

## Automatic deployment

`.github/workflows/hub-bump.yml` runs `deploy/deploy.sh --yes` on a version
tag or the Run-workflow button. One-time setup: create a classic PAT
(`public_repo` scope) and add it as repo secret `HUB_PAT`.

```bash
git tag v1.1.0 && git push origin v1.1.0
```

Deploys are gated behind the tag on purpose: every hub PR pings human
maintainers, so a bump should always mean "I ran it in the client".

## After client updates

RuneLite revs weekly; `build.gradle` tracks `latest.release`. When an update
breaks the plugin, users see it first — fix, test, tag, bump. The hub pins
its own RuneLite version (`plugin-hub/runelite.version`), so a hub PR can
fail CI even when local builds pass — rebuild against latest and re-push.

## Troubleshooting

| Symptom | Cause / fix |
|---|---|
| Hub CI fails, local build green | Hub builds with their template + pinned RuneLite version. Check the `manifest_diff` artifact; fix, push, update `commit=` on the same PR branch. |
| `deploy.sh: HEAD not pushed` | `git push` first — the manifest SHA must be reachable on the public repo. |
| PR rejected: "difficult to verify" | Keep the diff small; never add reflection, shading, or network calls without a `warning=` manifest line. |
| Fork out of date | Irrelevant — the script clones upstream fresh and pushes a new branch each run. |
| Pull the plugin from the hub | PR deleting `plugins/bank-highlight-search`. |

## Files

| File | Role |
|---|---|
| `deploy/deploy.sh` | Both modes: initial **Add** PR / routine **bump** PR. `--yes` for CI, `--skip-build` to skip the local build. |
| `.github/workflows/hub-bump.yml` | Tag/button automation around the script. Needs `HUB_PAT`. |
| `runelite-plugin.properties` | Metadata the hub reads at build time. |
