/*
 * :core — Library wrapper around the upstream Quick Search :app sources.
 *
 * This module is consumed by downstream apps (e.g. instant-search) that want
 * to reuse Quick Search's business logic / data layer / ViewModels behind their
 * own UI. The source set points at ../app/src/main, so all upstream files compile
 * here verbatim — keeping this patch series minimal and rebase-friendly.
 *
 * The upstream :app module remains fully buildable as before.
 */
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    id("kotlin-parcelize")
}

android {
    namespace = "com.tk.quicksearch"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
        // Library modules do not generate app-level BuildConfig fields by default,
        // but upstream :app source (e.g. CrashLogManager) reads them. Mirror the
        // upstream :app values here so those call sites compile.
        buildConfigField("String", "VERSION_NAME", "\"3.9\"")
        buildConfigField("int", "VERSION_CODE", "69")
        buildConfigField("String", "APPLICATION_ID", "\"com.tk.quicksearch\"")
    }

    sourceSets["main"].apply {
        // Upstream split into `standard` / `fdroid` product flavors (commit a6347d9f).
        // We consume the `standard` flavor (Play-store behaviour: web suggestions on,
        // Google Sans fonts, review/update helpers). Pull both main + standard sources
        // so flavor-only symbols (DistributionDefaults, DistributionTypography,
        // ReviewHelper, UpdateHelper) resolve.
        java.srcDirs("../app/src/main/java", "../app/src/standard/java")
        res.srcDirs("../app/src/main/res", "../app/src/standard/res")
        manifest.srcFile("src/main/AndroidManifest.xml")
        assets.srcDirs("../app/src/main/assets")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // Mirror upstream app/build.gradle.kts so all sources compile.
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    api(libs.androidx.core.ktx)
    api(libs.androidx.lifecycle.runtime.ktx)
    api(libs.androidx.lifecycle.runtime.compose)
    api(libs.androidx.lifecycle.viewmodel.compose)
    api(libs.androidx.activity.compose)
    api(libs.androidx.appcompat)
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.ui)
    api(libs.androidx.ui.graphics)
    api(libs.androidx.ui.tooling.preview)
    api(libs.androidx.material3)
    api(libs.androidx.material.icons.extended)
    api(libs.androidx.glance.appwidget)
    api(libs.androidx.profileinstaller)
    api(libs.google.material)
    api(libs.androidx.security.crypto)
    api(libs.okhttp)
    api(libs.play.review.ktx)
    api(libs.play.app.update.ktx)
    api(libs.libphonenumber)
    api(libs.reorderable)
    api(libs.androidx.browser)
    api(libs.androidx.room.runtime)
    api(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    // The upstream :app source references OssLicensesMenuActivity from this artifact.
    api(libs.play.services.oss.licenses)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
