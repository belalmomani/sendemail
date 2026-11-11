# Email Generator (Android Studio project)

## Features
- Generates clearly-labelled sample addresses under `example.com` (reserved for examples; not real inboxes).
- Stores subscriber email addresses locally on the device.
- Imports CSV rows only when the second column is `yes` or `true`.
- Prepares an email draft in the user's installed email app for up to 20 opted-in recipients per batch. The user reviews and sends it manually.
- Does not send automatically and does not verify that an address exists.

## CSV format
`email,consent`
`person@example.org,yes`
`other@example.org,no`

Only rows with `yes` or `true` in column 2 are imported.

## Build APK
1. Install Android Studio (current stable) on a computer.
2. Open this folder as a project.
3. Allow Gradle sync to finish. Android Studio may install the required Android SDK 35 and Gradle components.
4. Choose **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
5. Find the APK via **Build > Locate** (normally `app/build/outputs/apk/debug/app-debug.apk`).

This workspace contains the Android Studio source project, not a compiled APK. The APK must be built with Android Studio/Gradle.
