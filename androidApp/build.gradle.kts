import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.detekt)
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        FileInputStream(keystorePropertiesFile).use { load(it) }
    }
}

android {
    namespace = "com.berryos.music"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.berryos.music"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.1.0"
        manifestPlaceholders["logtoRedirectScheme"] = "com.berryos.music"
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
                enableV4Signing = true
            }
        }
    }

    buildTypes {
        debug {
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        release {
            isMinifyEnabled = false
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":composeApp"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.koin.android)
    detektPlugins(libs.detekt.rules.ktlint.wrapper)
}

androidComponents {
    onVariants { variant ->
        val assetsDir = rootProject.file("composeApp/build/generated/compose/resourceGenerator/androidMain/assets")
        variant.sources.assets?.addStaticSourceDirectory(assetsDir.absolutePath)
    }
}

afterEvaluate {
    val composeResourceTasks = listOf(
        ":composeApp:copyAndroidMainComposeResourcesToAndroidAssets",
        ":composeApp:prepareComposeResourcesTaskForCommonMain",
        ":composeApp:convertXmlValueResourcesForCommonMain",
        ":composeApp:copyNonXmlValueResourcesForCommonMain",
    )
    tasks.matching { task -> task.name.contains("merge") && task.name.contains("Assets") }.configureEach {
        composeResourceTasks.forEach { taskPath -> dependsOn(taskPath) }
    }
    tasks.matching { task -> task.name.contains("lint", ignoreCase = true) }.configureEach {
        composeResourceTasks.forEach { taskPath -> dependsOn(taskPath) }
    }
}

detekt {
    parallel.set(true)
    buildUponDefaultConfig.set(true)
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
}
