# AGENTS.md

Build, signing, release, and secret setup are documented in [CONTRIBUTING.md](CONTRIBUTING.md).

Consult CONTRIBUTING.md before:

- building or running the release variant, or changing how it is signed
- changing the release pipeline: `.github/workflows/ci.yml`, `.releaserc.json`, or the `play {}` block in `app/build.gradle.kts`
- adding, renaming, or rotating a repository secret

Never commit `keystore.properties`, keystores (`*.jks`, `*.keystore`), or service account JSON files.
