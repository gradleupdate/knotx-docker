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
    base
    id("com.bmuschko.docker-remote-api") version "6.1.3"
    id("org.nosphere.apache.rat") version "0.6.0"
}

repositories {
    jcenter()
    mavenLocal()
    mavenCentral()
    maven { url = uri("https://plugins.gradle.org/m2/") }
}

tasks {
    named<org.nosphere.apache.rat.RatTask>("rat") {
        excludes.addAll(listOf(
                "*.md", // docs
                "gradle/wrapper/**", "gradle*", "**/build/**", // Gradle
                "*.iml", "*.ipr", "*.iws", "*.idea/**", // IDEs
                ".github/*"
        ))
    }

    named("check") {
        dependsOn("rat")
    }

    withType<com.bmuschko.gradle.docker.tasks.image.DockerBuildImage>() {
        dependsOn("rat")
    }

    named("build") {
        dependsOn("downloadAndUnzipDistribution", "prepareDocker")
    }

    named("clean") {
        dependsOn("removeBaseImage", "removeBaseAlpineImage")
    }

}



val dockerBaseImageRef = "$buildDir/.docker/buildBaseImage-imageId.txt"
val dockerBaseAlpineImageRef = "$buildDir/.docker/buildBaseAlpineImage-imageId.txt"

val dockerBaseImageId = "${project.property("docker.domain")}/knotx:${project.version}"
val dockerBaseAlpineImageId = "${project.property("docker.domain")}/knotx-alpine:${project.version}"

val dockerfileBaseImagePath = "$projectDir/src/main/docker/base/Dockerfile"
val dockerfileBaseAlpineImagePath = "$projectDir/src/main/docker/base-alpine/Dockerfile"

fun com.bmuschko.gradle.docker.tasks.image.DockerRemoveImage.remove(name: String): Unit {
    val spec = object : Spec<Task> {
        override fun isSatisfiedBy(task: Task): Boolean {
            return file(name).exists()
        }
    }
    onlyIf(spec)

    targetImageId(if (file(name).exists()) file(name).readText() else "")
    onError {
        if (!this.message!!.contains("No such image"))
            throw this
    }
}

tasks.register<Copy>("copyDockerfileBase") {
    group = "docker"
    from(dockerfileBaseImagePath)
    into("$buildDir/out/base")
}

tasks.register<Copy>("copyDockerfileBaseAlpine") {
    group = "docker"
    from(dockerfileBaseAlpineImagePath)
    into("$buildDir/out/baseAlpine")
}

tasks.register<com.bmuschko.gradle.docker.tasks.image.DockerRemoveImage>("removeBaseImage") {
    group = "docker"
    remove(dockerBaseImageRef)
}

tasks.register<com.bmuschko.gradle.docker.tasks.image.DockerRemoveImage>("removeBaseAlpineImage") {
    group = "docker"
    remove(dockerBaseAlpineImageRef)
}

tasks.register<com.bmuschko.gradle.docker.tasks.image.DockerBuildImage> ("buildBaseImage") {
    group = "docker"
    inputDir.set(file("$buildDir/out"))
    dockerFile.set(file("$buildDir/out/base/Dockerfile"))
    images.add(dockerBaseImageId)
    dependsOn("removeBaseImage", "copyDockerfileBase")
}

tasks.register<com.bmuschko.gradle.docker.tasks.image.DockerBuildImage> ("buildBaseAlpineImage") {
    group = "docker"
    inputDir.set(file("$buildDir/out"))
    dockerFile.set(file("$buildDir/out/baseAlpine/Dockerfile"))
    images.add(dockerBaseAlpineImageId)
    dependsOn("removeBaseAlpineImage", "copyDockerfileBaseAlpine")
}

tasks.register<com.bmuschko.gradle.docker.tasks.image.DockerSaveImage>("saveBaseImage") {
    destFile.set(file("$projectDir/build/docker/docker-build-base.tar"))
    image.set(dockerBaseImageId)
    useCompression.set(true)
    mustRunAfter("buildBaseImage")
}

tasks.register<com.bmuschko.gradle.docker.tasks.image.DockerSaveImage>("saveBaseAlpineImage") {
    destFile.set(file("$projectDir/build/docker/docker-build-base-alpine.tar"))
    image.set(dockerBaseAlpineImageId)
    useCompression.set(true)
    mustRunAfter("buildBaseAlpineImage")
}

tasks.register("prepareDocker") {
    dependsOn("buildBaseImage", "buildBaseAlpineImage", "saveDockerImage")
    mustRunAfter("downloadAndUnzipDistribution")
}


tasks.register("saveDockerImage") {
    dependsOn("saveBaseImage", "saveBaseAlpineImage")
}

apply(from = "gradle/distribution.gradle.kts")