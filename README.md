# Orbit Dash

One-thumb Android arcade game. Your ball circles a ring: tap anywhere to reverse direction,
collect gold gems, dodge the red sparks fired from the core. Speed rises with your score.

Plain Java, one Activity, no libraries. Works on Android 5.0 and newer.

## Build the APK with GitHub (no installs needed)

1. Sign in at https://github.com (create a free account if needed).
2. Click **+** (top right) > **New repository**. Name it `orbit-dash`, choose **Public**, click **Create repository**.
3. On the new page click the link **uploading an existing file**.
4. Unzip `OrbitDash.zip` on your computer. Open the `OrbitDash` folder, select **everything inside it**
   (including the `.github` folder) and drag it into the browser page. Click **Commit changes**.
5. Open the **Actions** tab. A run called "Build APK" starts by itself and takes about 3 to 5 minutes.
   A green tick means it worked.
6. Go back to the repository's front page and click **Releases** (right side). Download **OrbitDash.apk**.
   (Alternative: open the finished run in Actions and download `OrbitDash-apk` under Artifacts; that one is a zip containing the APK.)

If the Actions tab shows no run, the `.github` folder was not uploaded. Click **Add file > Create new file**,
type `.github/workflows/build.yml` as the name, paste the contents of that file from the zip, and commit.

## Install on the phone

1. On the phone, open `https://github.com/YOUR-USERNAME/orbit-dash/releases` in Chrome and tap **OrbitDash.apk**.
2. Open the downloaded file. Android says the browser is not allowed to install unknown apps:
   tap **Settings**, switch on **Allow from this source**, go back, tap **Install**.
   (Android 7 or older: Settings > Security > switch on **Unknown sources**.)
3. If Play Protect warns about an unknown developer, tap **More details > Install anyway**.

## Pinned versions

Gradle 8.9, Android Gradle Plugin 8.5.2, JDK 17, compileSdk/targetSdk 34, minSdk 21.
There is no Gradle wrapper; the workflow installs Gradle itself. To build locally instead:
install JDK 17, Gradle 8.9 and the Android SDK (platform 34), then run `gradle assembleDebug`.
