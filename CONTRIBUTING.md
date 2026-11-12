# Contributing

## Build

Build and lint the release variant with `./gradlew lint assembleRelease`, which is what CI runs.
CI uses Temurin JDK 25.
The debug build needs no signing setup.

## Local signing

A signed release build needs a `keystore.properties` file in the repository root.
The file is gitignored, so never commit it, and never commit the keystores it points to.
It holds these keys:

- `uploadStoreFile`, `uploadStorePassword`, `uploadKeyAlias`, `uploadKeyPassword`: the upload keystore
- `debugStoreFile`, `debugStorePassword`, `debugKeyAlias`, `debugKeyPassword`: the debug keystore

Without the file, release builds are unsigned.

## Release

A push to `main` runs [semantic-release](https://semantic-release.gitbook.io/), which derives the version from the commits and writes it to `versionName` in `app/build.gradle.kts`.
CI then signs the release bundle and publishes it to the internal testing track on Google Play with [Gradle Play Publisher](https://github.com/Triple-T/gradle-play-publisher).
The plugin asks Google Play for the highest `versionCode` in use and publishes with the next one, so the `versionCode` in `app/build.gradle.kts` only applies to local builds.

## Secrets

The release job needs these repository secrets:

- `UPLOAD_KEYSTORE_BASE64`: the upload keystore, encoded with `base64 -w0`, holding the key under the alias `upload`
- `UPLOAD_KEYSTORE_PASSWORD`: the password of the keystore
- `UPLOAD_KEY_PASSWORD`: the password of the key
- `PLAY_SERVICE_ACCOUNT_JSON`: the JSON key of a Google Cloud service account that may release the app in the Play Console
- `PERSONAL_ACCESS_TOKEN`: optional, passed to the semantic-release job, which uses it to push commits and create releases

### Getting the upload key

If you already have the upload keystore, use it.
Google Play only accepts uploads signed with the upload key it has on record, so a new key needs an upload key reset in the Play Console before the first upload.
To create one:

```sh
keytool -genkeypair -v -keystore upload.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
```

### Getting the service account key

1. In the Google Cloud Console, use the project linked to the Play Console (or create one) and enable the Google Play Android Developer API.
2. Create a service account under IAM & Admin, then add a JSON key to it.
3. In the Play Console, invite the service account's email under Users and permissions and grant it release permissions for the app.

Delete the downloaded JSON file once the secret is set.

### Setting the secrets

Set them with the GitHub CLI, which keeps the values out of your shell history:

```sh
base64 -w0 upload.jks | gh secret set UPLOAD_KEYSTORE_BASE64 --repo maevsi/android
gh secret set UPLOAD_KEYSTORE_PASSWORD --repo maevsi/android
gh secret set UPLOAD_KEY_PASSWORD --repo maevsi/android
gh secret set PLAY_SERVICE_ACCOUNT_JSON --repo maevsi/android < service-account.json
gh secret set PERSONAL_ACCESS_TOKEN --repo maevsi/android
```
