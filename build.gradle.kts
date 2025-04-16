import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(23))
    }
}

kotlin {
    jvmToolchain(23)
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.foundation)
    implementation(compose.material3)
    implementation(compose.runtime)
    implementation(compose.ui)
    implementation(compose.components.resources)
    compileOnly(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.ktor.client.java)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.ktor3)

    // Elasticsearch dependencies
    implementation("org.elasticsearch.client:elasticsearch-rest-high-level-client:7.10.2")
    implementation("org.elasticsearch:elasticsearch:7.10.2")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.12.3")
}

compose.desktop {
    application {
        mainClass = "se.kth.booksearcher.MainKt"

        nativeDistributions {
            // Set target formats based on current OS
            when (System.getProperty("os.name").toLowerCase()) {
                in listOf("mac os x", "darwin") -> targetFormats(TargetFormat.Dmg)
                in listOf("linux", "unix") -> targetFormats(TargetFormat.AppImage, TargetFormat.Deb)
                in listOf("windows") -> targetFormats(TargetFormat.Exe, TargetFormat.Msi)
                else -> targetFormats(TargetFormat.Dmg, TargetFormat.AppImage, TargetFormat.Exe)
            }

            packageName = "BookSearcher"
            packageVersion = "1.0.0"
        }
    }
}