/* Copyright 2021 KJP12
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. */

import java.net.URI

plugins {
  java
  application
  `java-library`
  `maven-publish`
  id("com.diffplug.spotless")
}

val project_version: String by project

val isPublish = System.getenv("GITHUB_EVENT_NAME") == "release"
val isRelease = System.getenv("BUILD_RELEASE").toBoolean()
val isActions = System.getenv("GITHUB_ACTIONS").toBoolean()
val baseVersion: String = project_version

group = "net.kjp12"

version =
  when {
    isRelease -> baseVersion
    isActions ->
      "$baseVersion-build.${System.getenv("GITHUB_RUN_NUMBER")}-commit.${System.getenv("GITHUB_SHA").substring(0, 7)}-branch.${System.getenv("GITHUB_REF")?.substring(11)?.replace('/', '.') ?: "unknown"}"
    else -> "$baseVersion-build.local"
  }

java {
  sourceCompatibility = JavaVersion.VERSION_16
  targetCompatibility = JavaVersion.VERSION_16
  modularity.inferModulePath.set(true)
  withSourcesJar()
}

application { mainClass.set("net.kjp12.updater.Main") }

repositories {
  mavenCentral()
  maven { url = URI.create("https://oss.sonatype.org/content/repositories/snapshots") }
}

dependencies {}

spotless {
  java {
    importOrderFile(projectDir.resolve(".internal/spotless.importorder"))
    eclipse().configFile(projectDir.resolve(".internal/spotless.xml"))

    licenseHeaderFile(projectDir.resolve(".internal/license-header.java"))
  }
  kotlinGradle {
    target("*.gradle.kts")
    ktfmt().googleStyle()
    licenseHeaderFile(
      projectDir.resolve(".internal/license-header.java"),
      "(import|plugins|rootProject)"
    )
  }
}

tasks {
  withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.isDeprecation = true
    options.isWarnings = true
  }
  jar {
    // A cursed hack indeed, but Gradle refuses to set it what *should* be the regular way.
    manifest {
      attributes["Implementation-Version"] = archiveVersion.orNull
      attributes["Main-Class"] = "net.kjp12.updater.Main"
    }
  }
  withType<Jar> { from("LICENSE") }
}
