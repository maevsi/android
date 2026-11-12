[![ci status][ci-image]][ci-url]

[ci-image]: https://img.shields.io/github/actions/workflow/status/maevsi/android/ci.yml
[ci-url]: https://github.com/maevsi/android/actions/workflows/ci.yml

# Vibetype's Android app

This app is a [trusted web activity](https://developer.chrome.com/docs/android/trusted-web-activity/).

## Release

A push to `main` runs [semantic-release](https://semantic-release.gitbook.io/), which derives the version from the commits and writes it to `versionName` in `app/build.gradle.kts`.
CI then signs the release bundle and publishes it to the internal testing track on Google Play with [Gradle Play Publisher](https://github.com/Triple-T/gradle-play-publisher).
The plugin asks Google Play for the highest `versionCode` in use and publishes with the next one, so the `versionCode` in `app/build.gradle.kts` only applies to local builds.

This needs the following repository secrets:

- `UPLOAD_KEYSTORE_BASE64`: the upload keystore, encoded with `base64 -w0`, holding the key under the alias `upload`
- `UPLOAD_KEYSTORE_PASSWORD`: the password of the keystore
- `UPLOAD_KEY_PASSWORD`: the password of the key
- `PLAY_SERVICE_ACCOUNT_JSON`: the JSON key of a Google Cloud service account that may release the app in the Play Console

## License

This project is licensed under the [Apache License 2.0](LICENSE).
It is based on the Trusted Web Activity project template of [Bubblewrap](https://github.com/GoogleChromeLabs/bubblewrap), which is licensed under the same terms.
