/*
 * Copyright (C) 2019 Knot.x Project
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

plugins {
    id("com.bmuschko.docker-remote-api")
    id("org.nosphere.apache.rat")
}

repositories {
    jcenter()
    mavenLocal()
    maven { url = uri("https://plugins.gradle.org/m2/") }
    maven { url = uri("https://oss.sonatype.org/content/groups/staging/") }
    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") }
}

tasks {
    register("printProps") {
        doLast{
            logger.lifecycle("knotx.version: ${project.property("knotx.version")}")
            logger.lifecycle("knotx.conf: ${project.property("knotx.conf")}")
        }
    }

    named<org.nosphere.apache.rat.RatTask>("rat") {
        excludes.addAll(listOf(
                "*.md", // docs
                "gradle/wrapper/**", "gradle*", "**/build/**", // Gradle
                "*.iml", "*.ipr", "*.iws", "*.idea/**", // IDEs
                ".github/*"
        ))
    }

    withType<com.bmuschko.gradle.docker.tasks.image.DockerBuildImage>() {
        dependsOn("rat")
    }

    register("build") {
        group = "build"
        dependsOn("downloadAndUnzipDistribution", "prepareDocker")
    }

    register("clean") {
        group = "build"
        dependsOn("removeBaseImage", "removeBaseAlpineImage")
        doFirst {
            delete(buildDir)
        }
    }

}

apply(from = "gradle/docker.gradle.kts")
apply(from = "gradle/distribution.gradle.kts")