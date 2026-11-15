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
version="1.1.0"

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
    // Plain JUnit 5 for the pure classes (BundledFonts, UiFontPolicy); no IDE test framework needed.
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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

// The two UI themes, Tiling Blue Dark and Tiling Blue Island Dark, share one source: src/main/theme/tilingblue.theme.json.
// Each file in src/main/theme/variants adds its name, its parent theme and the keys of its own; merged here into
// theme/tilingblue-<variant>.theme.json in the plugin's resources (registered by themeProvider in plugin.xml).
val generateThemes = tasks.register("generateThemes") {
    val sources = layout.projectDirectory.dir("src/main/theme")
    val output = layout.buildDirectory.dir("generated/themes")
    inputs.dir(sources)
    outputs.dir(output)
    doLast {
        // the variant's value wins; nested objects are merged key by key
        fun merge(base: Map<*, *>, variant: Map<*, *>): Map<Any?, Any?> {
            val merged = LinkedHashMap<Any?, Any?>(base)
            for ((key, value) in variant) {
                val inBase = merged[key]
                merged[key] = if (value is Map<*, *> && inBase is Map<*, *>) merge(inBase, value) else value
            }
            return merged
        }
        val json = groovy.json.JsonSlurper()
        val shared = json.parse(sources.file("tilingblue.theme.json").asFile) as Map<*, *>
        val themes = output.get().dir("theme").asFile
        themes.deleteRecursively()
        themes.mkdirs()
        val variants = sources.dir("variants").asFile.listFiles { f -> f.name.endsWith(".theme.json") }.orEmpty()
        check(variants.isNotEmpty()) { "no theme variants in ${sources.dir("variants")}" }
        for (variant in variants.sortedBy { it.name }) {
            val merged = merge(shared, json.parse(variant) as Map<*, *>)
            // name and parent first, as in a hand-written theme
            val ordered = (listOf("name", "dark", "author", "parentTheme") + merged.keys).distinct()
                .filter { it in merged }.associateWith { merged[it] }
            val text = groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson(ordered))
            themes.resolve("tilingblue-${variant.name}").writeText(text + "\n")
        }
    }
}

sourceSets {
    main {
        resources.srcDir(generateThemes)
    }
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
    }
    jar {
        from("LICENSE") { into("META-INF") }
    }
    test {
        useJUnitPlatform()
    }
}
