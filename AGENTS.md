# AGENTS.md

Build, signing, release, and secret setup are documented in [CONTRIBUTING.md](CONTRIBUTING.md).

Consult CONTRIBUTING.md before:

- building or running the release variant, or changing how it is signed
- changing the release pipeline: the workflows in `.github/workflows`, `.github/scripts/play-notes.sh`, `.releaserc.json`, or the `play {}` block in `app/build.gradle.kts`
- adding, renaming, or rotating a repository secret or variable

Never commit `keystore.properties`, keystores (`*.jks`, `*.keystore`), or service account JSON files.

## Release notes

A pull request whose title starts with `feat`, `fix`, or `perf` triggers a release, so it must add Google Play release notes, which CI checks.
Add `play-notes/<slug>/de-DE.txt` and `play-notes/<slug>/en-US.txt`, where `<slug>` is the last part of the branch name.
Each file holds one sentence of at most 150 characters.
Write for people who use the app: say what they notice or can do now, without technical terms, issue numbers, or the words "fix" and "bug".
Address readers in German with "du", like the web app does.
If users notice nothing, such as for a build or CI fix, add the `no-play-notes` label instead.
