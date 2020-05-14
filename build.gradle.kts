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
import com.bmuschko.gradle.docker.tasks.image.*

plugins {
    base
    id("io.knotx.release-base")
    id("com.bmuschko.docker-remote-api")
    id("org.nosphere.apache.rat")
}

repositories {
    mavenLocal()
    jcenter()
    gradlePluginPortal()
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

    withType<DockerBuildImage>() {
        dependsOn("rat")
    }

    named("build") {
        dependsOn("buildBaseImage", "buildBaseAlpineImage")
        mustRunAfter("setVersion")
    }

    named("clean") {
        dependsOn("removeBaseImage", "removeBaseAlpineImage")
    }

    named("updateChangelog") {
        dependsOn("build", "setVersion")
    }

    register("prepare") {
        group = "release"
        dependsOn("updateChangelog")
    }

    register("publishArtifacts") {
        group = "release"
        logger.lifecycle("Publishing docker images")
        dependsOn("pushImages")
    }

}

val dockerBaseImageRef = "$buildDir/.docker/buildBaseImage-imageId.txt"
val dockerBaseAlpineImageRef = "$buildDir/.docker/buildBaseAlpineImage-imageId.txt"

val dockerRepository = project.property("docker.domain").toString()

val dockerBaseImageId = "knotx"
val dockerBaseAlpineImageId = "knotx-alpine"

val dockerfileBaseImagePath = "$projectDir/src/main/docker/base/Dockerfile"
val dockerfileBaseAlpineImagePath = "$projectDir/src/main/docker/base-alpine/Dockerfile"

fun DockerRemoveImage.remove(name: String): Unit {
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
    mustRunAfter("cleanDistribution")
}

tasks.register<Copy>("copyDockerfileBaseAlpine") {
    group = "docker"
    from(dockerfileBaseAlpineImagePath)
    into("$buildDir/out/baseAlpine")
    mustRunAfter("cleanDistribution")
}

tasks.register<DockerRemoveImage>("removeBaseImage") {
    group = "docker"
    remove(dockerBaseImageRef)
}

tasks.register<DockerRemoveImage>("removeBaseAlpineImage") {
    group = "docker"
    remove(dockerBaseAlpineImageRef)
}

tasks.register<DockerBuildImage> ("buildBaseImage") {
    group = "docker"
    inputDir.set(file("$buildDir/out"))
    dockerFile.set(file("$buildDir/out/base/Dockerfile"))
    images.add("$dockerRepository/$dockerBaseImageId:${project.version}")
    dependsOn("removeBaseImage", "copyDockerfileBase", "downloadAndUnzipDistribution")
}

tasks.register<DockerBuildImage> ("buildBaseAlpineImage") {
    group = "docker"
    inputDir.set(file("$buildDir/out"))
    dockerFile.set(file("$buildDir/out/baseAlpine/Dockerfile"))
    images.add("$dockerRepository/$dockerBaseAlpineImageId:${project.version}")
    dependsOn("removeBaseAlpineImage", "copyDockerfileBaseAlpine", "downloadAndUnzipDistribution")
}

tasks.register<DockerTagImage>("tagBaseAlpineImage") {
    group = "docker"
    imageId.set("$dockerRepository/$dockerBaseAlpineImageId:${project.version}")
    repository.set("$dockerRepository/$dockerBaseAlpineImageId")
    tag.set(project.version.toString())
}

tasks.register<DockerTagImage>("tagBaseImage") {
    group = "docker"
    imageId.set("$dockerRepository/$dockerBaseImageId:${project.version}")
    repository.set("$dockerRepository/$dockerBaseImageId")
    tag.set(project.version.toString())
}

tasks.register<DockerPushImage>("pushImages") {
    group = "docker"
    images.add("$dockerRepository/$dockerBaseAlpineImageId:${project.version}")
    images.add("$dockerRepository/$dockerBaseImageId:${project.version}")
    dependsOn("tagBaseAlpineImage", "tagBaseImage")
    registryCredentials {
        username.set(if (project.hasProperty("dockerHubUsername")) project.property("dockerHubUsername")?.toString() else "UNKNOWN")
        password.set(if (project.hasProperty("dockerHubPassword")) project.property("dockerHubPassword")?.toString() else "UNKNOWN")
    }
}

apply(from = "gradle/distribution.gradle.kts")