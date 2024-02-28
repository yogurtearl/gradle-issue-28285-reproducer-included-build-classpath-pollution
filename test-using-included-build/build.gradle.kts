/*
 * Copyright 2024 American Express Travel Related Services Company, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 */

import org.gradle.kotlin.dsl.support.uppercaseFirstChar

plugins {
    `kotlin-dsl`
    `maven-publish`
}

group = "test.plugin"
version = "0.0.1"

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.0")

    testImplementation(gradleTestKit())
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

val localMavenRepoForTestingPlugin = "${buildDir}/plugin-under-test-maven-repo"
val mavenRepoName = "LocalMavenRepoForTestingPlugin"
publishing {
    repositories {
        maven {
            name = mavenRepoName
            url = uri("file://$localMavenRepoForTestingPlugin")
        }
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    // make sure we publish the plugin before the tests run
    dependsOn("publishAllPublicationsTo${mavenRepoName.uppercaseFirstChar()}Repository")

    // this will be read by the test, so it load the plugin from the local maven repo
    this.systemProperties["pluginUnderTestMavenRepo"] = localMavenRepoForTestingPlugin
    this.systemProperties["pluginUnderTestVersion"] = version
}
