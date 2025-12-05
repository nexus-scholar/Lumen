import org.gradle.jvm.tasks.Jar
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.gradle.api.tasks.compile.JavaCompile

plugins {
    kotlin("multiplatform") version "1.9.21"
    kotlin("plugin.serialization") version "1.9.21"
    id("org.jetbrains.compose") version "1.5.11"
    id("io.gitlab.arturbosch.detekt") version "1.23.4"
    id("org.jetbrains.kotlinx.kover") version "0.7.5"
}

group = "com.lumen"
version = "0.1.0-SNAPSHOT"

val kotlinxCoroutinesVersion = "1.7.3"
val kotlinxSerializationVersion = "1.6.2"
val kotlinxDatetimeVersion = "0.5.0"
val ktorVersion = "2.3.7"
val koinVersion = "3.5.3"
val kotestVersion = "5.8.0"
val junitVersion = "5.10.1"
val mockkVersion = "1.13.8"
val exposedVersion = "0.46.0"
val composeVersion = "1.5.11"

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    maven("https://maven.pkg.jetbrains.space/public/p/kotlin-wrappers/maven")
}

kotlin {
    jvmToolchain(21)

    jvm {
        withJava()
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
        @OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
        mainRun {
            mainClass.set("com.lumen.desktop.MainKt")
        }
    }

    js(IR) {
        browser {
            commonWebpackConfig {
                cssSupport {
                    enabled.set(true)
                }
            }
        }
        binaries.executable()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutinesVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:$kotlinxDatetimeVersion")
                implementation("io.github.oshai:kotlin-logging:5.1.1")
                implementation("io.insert-koin:koin-core:$koinVersion")
                implementation("io.ktor:ktor-client-core:$ktorVersion")
                implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
                implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
                implementation("io.ktor:ktor-client-logging:$ktorVersion")
                implementation("com.michael-bull.kotlin-result:kotlin-result:1.1.18")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$kotlinxCoroutinesVersion")
                implementation("io.kotest:kotest-assertions-core:$kotestVersion")
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation("org.xerial:sqlite-jdbc:3.44.1.0")
                implementation("com.squareup.sqldelight:sqlite-driver:1.5.5")
                implementation("org.postgresql:postgresql:42.7.1")
                implementation("com.zaxxer:HikariCP:5.1.0")
                implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
                implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
                implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
                implementation("org.jetbrains.exposed:exposed-kotlin-datetime:$exposedVersion")
                implementation("org.jgrapht:jgrapht-core:1.5.2")
                implementation("org.jgrapht:jgrapht-io:1.5.2")
                implementation("io.ktor:ktor-client-cio:$ktorVersion")
                implementation("com.github.ajalt.clikt:clikt:4.2.1")
                implementation("com.charleskorn.kaml:kaml:0.55.0")
                implementation("ch.qos.logback:logback-classic:1.4.14")
                implementation("org.apache.commons:commons-csv:1.10.0")
                implementation("com.aallam.openai:openai-client:3.6.2")
                implementation("io.insert-koin:koin-core-coroutines:$koinVersion")
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit5"))
                implementation("org.junit.jupiter:junit-jupiter:$junitVersion")
                implementation("io.mockk:mockk:$mockkVersion")
                implementation("io.kotest:kotest-runner-junit5:$kotestVersion")
                implementation("io.kotest:kotest-assertions-core:$kotestVersion")
                implementation("io.ktor:ktor-client-mock:$ktorVersion")
                implementation("io.insert-koin:koin-test-junit5:$koinVersion") {
                    exclude(group = "org.jetbrains.kotlin", module = "kotlin-test-junit")
                }
                implementation("com.h2database:h2:2.2.224")
            }
        }

        val jsMain by getting {
            dependencies {
                implementation(compose.html.core)
                implementation(compose.runtime)
                implementation("io.ktor:ktor-client-js:$ktorVersion")
                implementation("org.jetbrains.kotlin-wrappers:kotlin-react:18.2.0-pre.649")
                implementation("org.jetbrains.kotlin-wrappers:kotlin-react-dom:18.2.0-pre.649")
            }
        }

        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.lumen.desktop.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Lumen"
            packageVersion = "1.0.0"
            description = "AI-Powered Systematic Review Tool"
            copyright = "© 2025 Lumen. All rights reserved."
            vendor = "Lumen"

            windows {
                menuGroup = "Lumen"
                upgradeUuid = "d4c8e1a2-3b5f-4e6d-9c8b-7a1e2f3d4c5b"
            }

            linux {
                packageName = "lumen"
                debMaintainer = "lumen@example.com"
            }

            macOS {
                bundleID = "com.lumen.app"
            }
        }
    }
}


detekt {
    config.setFrom("$projectDir/config/detekt/detekt.yml")
    buildUponDefaultConfig = true
}

// Ensure Java and Kotlin use compatible target versions
tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = "21"
    targetCompatibility = "21"
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.register<Jar>("cliJar") {
    group = "build"
    description = "Creates a standalone JAR for CLI"
    manifest {
        attributes["Main-Class"] = "com.lumen.cli.MainKt"
    }
    archiveBaseName.set("lumen-cli")
    from(kotlin.jvm().compilations.getByName("main").output)
    dependsOn(configurations.named("jvmRuntimeClasspath"))
    from({ configurations.named("jvmRuntimeClasspath").get().map { if (it.isDirectory) it else zipTree(it) } })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
