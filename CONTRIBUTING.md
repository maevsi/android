# Contributing

## Build

Build and lint the release variant with `./gradlew lint assembleRelease`, which is what CI runs.
CI uses Temurin JDK 25.
The debug build needs no signing setup and is signed with the default Android debug keystore.

## Local signing

A signed release build needs a `keystore.properties` file in the repository root.
The file is gitignored, so never commit it, and never commit the keystores it points to.
It must hold all of these keys, or the Gradle configuration fails:

- `uploadStoreFile`, `uploadStorePassword`, `uploadKeyAlias`, `uploadKeyPassword`: the upload keystore
- `debugStoreFile`, `debugStorePassword`, `debugKeyAlias`, `debugKeyPassword`: the debug keystore, which then replaces the default one

The two store file paths are relative to the `app` directory, not to the repository root.
Without the file, release builds are unsigned.

## Release

A push to `main`, `beta`, or `alpha` runs [semantic-release](https://semantic-release.gitbook.io/), which derives the version from the commits and writes it to `versionName` in `app/build.gradle.kts`.
CI then signs the release bundle and uploads it to the internal track on Google Play with [Gradle Play Publisher](https://github.com/Triple-T/gradle-play-publisher).
The plugin asks Google Play for the highest `versionCode` in use and publishes with the next one, so the `versionCode` in `app/build.gradle.kts` only applies to local builds.
Pushes to `beta` and `alpha` create prereleases on GitHub only, since CI publishes to Google Play from `main` alone.

Publishing authenticates with [Workload Identity Federation](https://cloud.google.com/iam/docs/workload-identity-federation), so CI holds no Google service account key.
Every job that talks to Google Play gets a short-lived GitHub OIDC token, which Google exchanges for access to the service account.

### Release notes

Every pull request that triggers a release, so every `feat`, `fix`, or `perf`, adds release notes for Google Play.
They live in `play-notes/<slug>/de-DE.txt` and `play-notes/<slug>/en-US.txt`, one sentence of at most 150 characters per file, written for people who use the app.
The Play notes workflow fails a pull request that triggers a release without adding them, unless it carries the `no-play-notes` label or comes from Renovate.
It also checks every fragment the pull request touches.
The fragments stay in the repository, so they double as a history of what users were told.

### Promoting a release

The upload stops at the internal track, so testers there get each release first.
After the upload, CI calls the Promote workflow, which asks Google Play which version is in production and collects the fragments added since then, one bullet each.
It shows the collected notes in the run's summary, so read them there before you approve the deployment to the `production` environment.
After the approval, it promotes the latest release on the internal track to production with those notes.
A release that adds no fragments stays on the internal track, and the next release that adds some takes it along.
A newer release cancels a promotion that still waits for approval, since its notes include the older ones.

Google Play allows 500 characters per language, and the workflow fails when the collected notes exceed that.
Then, or to promote a release without fragments, run the Promote workflow from `main` and pass your own notes, which replace the collected ones:

```sh
gh workflow run promote.yml --repo maevsi/android --ref main \
  -f release-notes-de="• Der Startbildschirm passt jetzt zum hellen Design." \
  -f release-notes-en="• The start screen now matches the light design."
```

Run `.github/scripts/play-notes.test.sh` after changing `.github/scripts/play-notes.sh`.

## Secrets and variables

The upload job needs these repository secrets:

- `UPLOAD_KEYSTORE_BASE64`: the upload keystore, base64-encoded on a single line, holding the key under the alias `upload`
- `UPLOAD_KEYSTORE_PASSWORD`: the password of the keystore
- `UPLOAD_KEY_PASSWORD`: the password of the key
- `PERSONAL_ACCESS_TOKEN`: the token semantic-release uses to push the release commit and create releases, which must belong to an account that may bypass the branch ruleset on `main` and `beta`

It also needs these repository variables, which are identifiers and not secrets, and the Promote workflow needs only these two:

- `GCP_WORKLOAD_IDENTITY_PROVIDER`: the full resource name of the workload identity provider
- `GCP_PUBLISHER_SERVICE_ACCOUNT`: the email of the service account that publishes

### Getting the upload key

If you already have the upload keystore, use it.
Google Play only accepts uploads signed with the upload key it has on record, so a new key needs an upload key reset in the Play Console before the first upload.
To create one:

```sh
keytool -genkeypair -v -keystore upload.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
```

### Setting up the environments

Create the `internal` and `production` environments, each allowed to deploy from `main` only:

```sh
for environment in internal production; do
  gh api -X PUT "repos/maevsi/android/environments/$environment" \
    -F 'deployment_branch_policy[protected_branches]=false' \
    -F 'deployment_branch_policy[custom_branch_policies]=true'
  gh api -X POST "repos/maevsi/android/environments/$environment/deployment-branch-policies" \
    -f name=main -f type=branch
done
```

Then make every promotion wait for your approval:

```sh
gh api -X PUT repos/maevsi/android/environments/production \
  -F 'deployment_branch_policy[protected_branches]=false' \
  -F 'deployment_branch_policy[custom_branch_policies]=true' \
  -F 'reviewers[][type]=User' -F "reviewers[][id]=$(gh api user --jq .id)"
```

Without reviewers, CI promotes every release that adds release notes to production on its own.

### Setting up Workload Identity Federation

Run these once, with a Google Cloud project that is linked to the Play Console.
Replace `PROJECT_ID` with the project ID.

```sh
gcloud services enable androidpublisher.googleapis.com iamcredentials.googleapis.com --project=PROJECT_ID

PROJECT_NUMBER=$(gcloud projects describe PROJECT_ID --format='value(projectNumber)')

gcloud iam workload-identity-pools create github \
  --project=PROJECT_ID --location=global --display-name="GitHub Actions"

gcloud iam workload-identity-pools providers create-oidc github-actions \
  --project=PROJECT_ID --location=global --workload-identity-pool=github \
  --issuer-uri=https://token.actions.githubusercontent.com \
  --attribute-mapping='google.subject=assertion.sub,attribute.repository=assertion.repository' \
  --attribute-condition="assertion.sub in ['repo:maevsi/android:environment:internal', 'repo:maevsi/android:environment:production']"

gcloud iam service-accounts create play-publisher --project=PROJECT_ID

gcloud iam service-accounts add-iam-policy-binding \
  play-publisher@PROJECT_ID.iam.gserviceaccount.com \
  --role=roles/iam.workloadIdentityUser \
  --member="principalSet://iam.googleapis.com/projects/${PROJECT_NUMBER}/locations/global/workloadIdentityPools/github/attribute.repository/maevsi/android"
```

The attribute condition means only jobs of `maevsi/android` that run in the `internal` or `production` environment can use the provider.
Both environments share one service account, and Google Play cannot limit a service account to one track.
The condition therefore keeps Play credentials away from workflows outside these environments, but it does not stop the upload job from releasing to production.
Then, in the Play Console, invite `play-publisher@PROJECT_ID.iam.gserviceaccount.com` under Users and permissions and grant it release permissions for the app.
Google Cloud IAM roles do not grant Play access, so this invitation is required.

Set the variables with the GitHub CLI:

```sh
gh variable set GCP_WORKLOAD_IDENTITY_PROVIDER --repo maevsi/android \
  --body "projects/${PROJECT_NUMBER}/locations/global/workloadIdentityPools/github/providers/github-actions"
gh variable set GCP_PUBLISHER_SERVICE_ACCOUNT --repo maevsi/android \
  --body "play-publisher@PROJECT_ID.iam.gserviceaccount.com"
```

### Migrating from the repository-wide condition

Order matters here.
First merge the workflow changes and create the environments, and only then tighten the existing provider.
A tightened provider makes the next release fail after its tag and GitHub release already exist.

```sh
gcloud iam workload-identity-pools providers update-oidc github-actions \
  --project=PROJECT_ID --location=global --workload-identity-pool=github \
  --attribute-condition="assertion.sub in ['repo:maevsi/android:environment:internal', 'repo:maevsi/android:environment:production']"
```

### Setting the secrets

Set them with the GitHub CLI, which keeps the values out of your shell history:

```sh
base64 < upload.jks | tr -d '\n' | gh secret set UPLOAD_KEYSTORE_BASE64 --repo maevsi/android
gh secret set UPLOAD_KEYSTORE_PASSWORD --repo maevsi/android
gh secret set UPLOAD_KEY_PASSWORD --repo maevsi/android
gh secret set PERSONAL_ACCESS_TOKEN --repo maevsi/android
```

If you used the earlier JSON key setup, delete the `PLAY_SERVICE_ACCOUNT_JSON` secret and delete any JSON keys of the service account in Google Cloud.
