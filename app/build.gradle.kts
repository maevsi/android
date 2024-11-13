import com.android.build.gradle.internal.tasks.factory.dependsOn
import groovy.xml.MarkupBuilder
import java.io.StringWriter
import java.io.FileInputStream
import java.util.Properties
import groovy.util.IndentPrinter

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val keystorePropertiesFile: File = rootProject.file("keystore.properties")
val keystoreProperties = Properties()

if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

interface Shortcut {
    val name: String?
    val shortName: String?
    val url: String?
    val icon: String?
}

object TwaManifest {
    const val applicationId = "si.maev.twa"
    const val protocol = "https://"
    const val hostName = "maev.si" // The domain being opened in the TWA.
    const val launchPath =
        "/?source=pwa" // The start path for the TWA. Must be relative to the domain.
    const val name = "maevsi" // The application name.
    const val launcherName = "maevsi" // The name shown on the Android Launcher.
    const val themeColor = "#27272a" // The color used for the status bar.
    const val themeColorDark = themeColor // The color used for the dark status bar.
    const val navigationColor = themeColor // The color used for the navigation bar.
    const val navigationColorDark = themeColorDark // The color used for the dark navbar.
    const val navigationDividerColor = themeColor // The navbar divider color.
    const val navigationDividerColorDark = themeColorDark // The dark navbar divider color.
    const val backgroundColor = themeColorDark // The color used for the splash screen background.
    const val enableNotifications = true // Set to true to enable notification delegation.

    // Every shortcut must include the following fields:
    // - name: String that will show up in the shortcut.
    // - short_name: Shorter string used if |name| is too long.
    // - url: Absolute path of the URL to launch the app with (e.g "/create").
    // - icon: Name of the resource in the drawable folder to use as an icon.
    val shortcuts = arrayOf<Shortcut>()

    // The duration of fade out animation in milliseconds to be played when removing splash screen.
    const val splashScreenFadeOutDuration = 300
    const val generatorApp = "PWABuilder" // Application that generated the Android Project

    // The fallback strategy for when Trusted Web Activity is not available. Possible values are
    // "customtabs" and "webview".
    const val fallbackType = "customtabs"
    const val enableSiteSettingsShortcut = "true"
    // const val orientation = "any" // Disabled to respect system's rotation setting
}

android {
    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("upload") {
                keyAlias = keystoreProperties.getProperty("uploadKeyAlias")
                keyPassword = keystoreProperties.getProperty("uploadKeyPassword")
                storeFile = file(keystoreProperties.getProperty("uploadStoreFile"))
                storePassword = keystoreProperties.getProperty("uploadStorePassword")
            }
            getByName("debug") {
                keyAlias = keystoreProperties.getProperty("debugKeyAlias")
                keyPassword = keystoreProperties.getProperty("debugKeyPassword")
                storeFile = file(keystoreProperties.getProperty("debugStoreFile"))
                storePassword = keystoreProperties.getProperty("debugStorePassword")
            }
        }
    }

    namespace = "si.maev.twa"
    compileSdk = 34

    defaultConfig {
        applicationId = "si.maev.twa"
        minSdk = 21
        targetSdk = 34
        versionCode = 3
        versionName = "0.0.3"

        // The name for the application
        resValue("string", "appName", TwaManifest.name)

        // The name for the application on the Android Launcher
        resValue("string", "launcherName", TwaManifest.launcherName)

        // The URL that will be used when launching the TWA from the Android Launcher
        val launchUrl = TwaManifest.protocol + TwaManifest.hostName + TwaManifest.launchPath
        resValue("string", "launchUrl", launchUrl)

        // The URL the Web Manifest for the Progressive Web App that the TWA points to. This
        // is used by Chrome OS and Meta Quest to open the Web version of the PWA instead of
        // the TWA, as it will probably give a better user experience for non-mobile devices.
        resValue(
            "string",
            "webManifestUrl",
            TwaManifest.protocol + TwaManifest.hostName + "/manifest.webmanifest"
        )

        // This is used by Meta Quest.
        resValue("string", "fullScopeUrl", TwaManifest.protocol + TwaManifest.hostName + "/")

        // The hostname is used when building the intent-filter, so the TWA is able to
        // handle Intents to open host url of the application.
        resValue("string", "hostName", TwaManifest.hostName)

        // This attribute sets the status bar color for the TWA. It can be either set here or in
        // `res/values/colors.xml`. Setting in both places is an error and the app will not
        // compile. If not set, the status bar color defaults to #FFFFFF - white.
        resValue("color", "colorPrimary", TwaManifest.themeColor)

        // This attribute sets the dark status bar color for the TWA. It can be either set here or in
        // `res/values/colors.xml`. Setting in both places is an error and the app will not
        // compile. If not set, the status bar color defaults to #000000 - white.
        resValue("color", "colorPrimaryDark", TwaManifest.themeColorDark)

        // This attribute sets the navigation bar color for the TWA. It can be either set here or
        // in `res/values/colors.xml`. Setting in both places is an error and the app will not
        // compile. If not set, the navigation bar color defaults to #FFFFFF - white.
        resValue("color", "navigationColor", TwaManifest.navigationColor)

        // This attribute sets the dark navigation bar color for the TWA. It can be either set here
        // or in `res/values/colors.xml`. Setting in both places is an error and the app will not
        // compile. If not set, the navigation bar color defaults to #000000 - black.
        resValue("color", "navigationColorDark", TwaManifest.navigationColorDark)

        // This attribute sets the navbar divider color for the TWA. It can be either
        // set here or in `res/values/colors.xml`. Setting in both places is an error and the app
        // will not compile. If not set, the divider color defaults to #00000000 - transparent.
        resValue("color", "navigationDividerColor", TwaManifest.navigationDividerColor)

        // This attribute sets the dark navbar divider color for the TWA. It can be either
        // set here or in `res/values/colors.xml`. Setting in both places is an error and the
        //app will not compile. If not set, the divider color defaults to #000000 - black.
        resValue("color", "navigationDividerColorDark", TwaManifest.navigationDividerColorDark)

        // Sets the color for the background used for the splash screen when launching the
        // Trusted Web Activity.
        resValue("color", "backgroundColor", TwaManifest.backgroundColor)

        // Defines a provider authority for the Splash Screen
        resValue("string", "providerAuthority", TwaManifest.applicationId + ".fileprovider")

        // The enableNotification resource is used to enable or disable the
        // TrustedWebActivityService, by changing the android:enabled and android:exported
        // attributes
        resValue("bool", "enableNotification", TwaManifest.enableNotifications.toString())

        TwaManifest.shortcuts.forEachIndexed { index, shortcut ->
            resValue("string", "shortcut_name_$index", "${shortcut.name}")
            resValue("string", "shortcut_short_name_$index", "${shortcut.shortName}")
        }

        // The splashScreenFadeOutDuration resource is used to set the duration of fade out animation in milliseconds
        // to be played when removing splash screen. The default is 0 (no animation).
        resValue(
            "integer",
            "splashScreenFadeOutDuration",
            TwaManifest.splashScreenFadeOutDuration.toString()
        )
        resValue("string", "generatorApp", TwaManifest.generatorApp)
        resValue("string", "fallbackType", TwaManifest.fallbackType)
        resValue("bool", "enableSiteSettingsShortcut", TwaManifest.enableSiteSettingsShortcut)
        // resValue("string", "orientation", TwaManifest.orientation) // Disabled to respect system's rotation setting

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    val signingConfigDebug = if (keystorePropertiesFile.exists()) {
        signingConfigs.getByName("debug")
    } else {
        null
    }
    val signingConfigUpload = if (keystorePropertiesFile.exists()) {
        signingConfigs.getByName("upload")
    } else {
        null
    }

    buildTypes {
        release {
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
            signingConfig = signingConfigUpload
            isMinifyEnabled = true
        }
        debug {
            signingConfig = signingConfigDebug
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

composeCompiler {
    enableStrongSkippingMode = true
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation(platform("androidx.compose:compose-bom:2024.11.00"))
    implementation("androidx.compose.ui:ui:1.7.5")
    implementation("androidx.compose.ui:ui-graphics:1.7.5")
    implementation("androidx.compose.ui:ui-tooling-preview:1.7.5")
    implementation("androidx.compose.material3:material3:1.3.1")
//    implementation 'com.google.androidbrowserhelper:locationdelegation:1.1.1'
//    implementation 'com.google.androidbrowserhelper:billing:1.0.0-alpha10'
    implementation("com.google.androidbrowserhelper:androidbrowserhelper:2.5.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.11.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.7.5")
    debugImplementation("androidx.compose.ui:ui-tooling:1.7.5")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.7.5")
}

tasks.register("generateShortcutsFile") {
    assert(TwaManifest.shortcuts.size < 5) { "You can have at most 4 shortcuts." }
    TwaManifest.shortcuts.forEachIndexed { i, s ->
        assert(s.name != null) { "Missing `name` in shortcut #$i" }
        assert(s.shortName != null) { "Missing `short_name` in shortcut #$i" }
        assert(s.url != null) { "Missing `icon` in shortcut #$i" }
        assert(s.icon != null) { "Missing `url` in shortcut #$i" }
    }

    val shortcutsFile = File("$projectDir/src/main/res/xml/shortcuts.xml")
    val xmlWriter = StringWriter()
    val xmlMarkup = MarkupBuilder(IndentPrinter(xmlWriter, "    ", true))

    xmlMarkup.withGroovyBuilder {
        "shortcuts"("xmlns:android=http://schemas.android.com/apk/res/android") {
            TwaManifest.shortcuts.forEachIndexed { i, s ->
                "shortcut"(
                    "android:shortcutId=shortcut$i",
                    "android:enabled=true",
                    "android:icon=@drawable/${s.icon}",
                    "android:shortcutShortLabel=@string/shortcut_short_name_$i",
                    "android:shortcutLongLabel=@string/shortcut_name_$i"
                ) {
                    "intent"(
                        "android:action=android.intent.action.MAIN",
                        "android:targetPackage=${TwaManifest.applicationId}",
                        "android:targetClass=${TwaManifest.applicationId}.LauncherActivity",
                        "android:data=${s.url}"
                    )
                    "categories"("android:name=android.intent.category.LAUNCHER")
                }
            }
        }
    }

    shortcutsFile.writeText(xmlWriter.toString() + '\n')
}

tasks.withType<PublishToMavenRepository> {
    dependsOn("generateShortcutsFile")
}

tasks.named("generateShortcutsFile").dependsOn(":preBuild")
