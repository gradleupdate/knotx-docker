/*
 * Copyright (C) 2020 Knot.x Project
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

import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.bmuschko.gradle.docker.tasks.image.DockerRemoveImage
import com.bmuschko.gradle.docker.tasks.image.DockerSaveImage

val dockerBaseImageRef = "$buildDir/.docker/buildBaseImage-imageId.txt"
val dockerBaseAlpineImageRef = "$buildDir/.docker/buildBaseAlpineImage-imageId.txt"

val dockerBaseImageId = "${project.property("docker.domain")}/knotx:${project.version}"
val dockerBaseAlpineImageId = "${project.property("docker.domain")}/knotx-alpine:${project.version}"

val dockerfileBaseImagePath = "$projectDir/src/main/docker/base/Dockerfile"
val dockerfileBaseAlpineImagePath = "$projectDir/src/main/docker/base-alpine/Dockerfile"

fun DockerRemoveImage.remove(name: String): Unit {
    val spec = object : Spec<Task> {
        override fun isSatisfiedBy(task: Task): Boolean {
            return File(name).exists()
        }
    }
    onlyIf(spec)

    targetImageId(if (File(name).exists()) File(name).readText() else "")
    onError {
        if (!this.message!!.contains("No such image"))
            throw this
    }
}

tasks.register<Copy>("copyDockerfileBase") {
    group = "docker"
    from(dockerfileBaseImagePath)
    into("$buildDir/out/")
    mustRunAfter("downloadBaseDistribution")
}

tasks.register<Copy>("copyDockerfileBaseAlpine") {
    group = "docker"
    from(dockerfileBaseAlpineImagePath)
    into("$buildDir/out/")
    mustRunAfter("downloadBaseDistribution")
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
    images.add(dockerBaseImageId)
    dependsOn("removeBaseImage", "copyDockerfileBase")
}

tasks.register<DockerBuildImage> ("buildBaseAlpineImage") {
    group = "docker"
    inputDir.set(file("$buildDir/out"))
    images.add(dockerBaseAlpineImageId)
    dependsOn("removeBaseAlpineImage", "copyDockerfileBaseAlpine")
}

tasks.register<DockerSaveImage>("saveBaseImage") {
    destFile.set(file("$projectDir/build/docker/docker-build-base.tar"))
    image.set(dockerBaseImageId)
    useCompression.set(true)
    mustRunAfter("buildBaseImage")
}

tasks.register<DockerSaveImage>("saveBaseAlpineImage") {
    destFile.set(file("$projectDir/build/docker/docker-build-base-alpine.tar"))
    image.set(dockerBaseAlpineImageId)
    useCompression.set(true)
    mustRunAfter("buildBaseAlpineImage")
}

tasks.register("prepareDocker") {
    dependsOn("cleanDistribution", "overwriteCustomFiles", "buildBaseImage", "buildBaseAlpineImage", "saveDockerImage")
}


tasks.register("saveDockerImage") {
    dependsOn("saveBaseImage", "saveBaseAlpineImage")
}

