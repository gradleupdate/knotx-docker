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
repositories {
    mavenLocal()
    mavenCentral()
    gradlePluginPortal()
}
dependencies {
    implementation("com.bmuschko:gradle-docker-plugin:6.1.3")
    implementation("org.nosphere.apache.rat:org.nosphere.apache.rat.gradle.plugin:0.6.0")
}
