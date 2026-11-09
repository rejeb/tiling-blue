/*
 * Copyright 2026 rbenrejeb
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Builds the Tiling Blue plugin with the IntelliJ Platform Gradle Plugin.
//   ./gradlew buildPlugin    -> build/distributions/tiling-blue-<version>.zip (upload this to JetBrains Marketplace)
//   ./gradlew verifyPlugin   -> IntelliJ Plugin Verifier against the target IDE
//   ./gradlew runIde         -> sandbox IDE with the plugin installed
plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.19.0"
}

group = "io.github.rejeb"

// The version lives in plugin.xml only, so tools/build.sh and Gradle always agree.
version = Regex("<version>(.+?)</version>")
    .find(file("src/main/resources/META-INF/plugin.xml").readText())!!
    .groupValues[1]

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

java {
    // Java 25 = the JetBrains Runtime every supported IDE (2026.2+) requires; GtkCss needs the Foreign Function API.
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

dependencies {
    intellijPlatform {
        // Compile against the oldest supported release (since-build 262).
        intellijIdea("2026.2")
        pluginVerifier()
    }
}

intellijPlatform {
    // A theme has no settings pages to index, and nothing to instrument (no forms; @NotNull checks not wanted).
    buildSearchableOptions = false
    instrumentCode = false

    pluginConfiguration {
        ideaVersion {
            sinceBuild = "262"
            untilBuild = provider { null }
        }
    }

    pluginVerification {
        ides {
            current()
        }
    }
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
    }
    jar {
        from("LICENSE") { into("META-INF") }
    }
}
