import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
}

kotlin {
    android {
        namespace = "com.berryos.music.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }

        withHostTest {}
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.animation)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.components.resources)
            implementation(compose.ui)
            implementation(libs.compose.ui.backhandler)
            implementation(libs.navigation3.ui)
            implementation(libs.lifecycle.viewmodel.navigation3)
            implementation(libs.lifecycle.viewmodel.compose)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)
            implementation("art.rippledesign:compose-rui")
            implementation("art.rippledesign:compose-rui-navigation3")
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.ktor.client.mock)
        }

        androidMain.dependencies {
            implementation("com.berryos:kotlin-audio-metadata:0.1.0-SNAPSHOT")
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.media3.exoplayer)
            implementation(libs.androidx.media3.session)
            implementation(libs.androidx.palette)
            implementation(libs.androidx.datastore.preferences.core)
            implementation(libs.koin.android)
            implementation(libs.logto.android)
            implementation(libs.ktor.client.okhttp)
        }
    }
}

detekt {
    source.setFrom(
        files(
            "src/commonMain/kotlin",
            "src/androidMain/kotlin",
            "src/commonTest/kotlin",
            "src/androidHostTest/kotlin",
        )
    )
    parallel.set(true)
    buildUponDefaultConfig.set(true)
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
}

dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
    detektPlugins(libs.detekt.rules.ktlint.wrapper)
}

room {
    schemaDirectory("$projectDir/schemas")
}

compose.resources {
    packageOfResClass = "com.berryos.music.resources"
}

afterEvaluate {
    tasks.named("copyAndroidMainComposeResourcesToAndroidAssets").configure {
        val outputDir = project.layout.buildDirectory.dir("generated/compose/resourceGenerator/androidMain/assets")
        val property = this::class.java.getMethod("getOutputDirectory").invoke(this) as org.gradle.api.file.DirectoryProperty
        property.set(outputDir)
    }
}
