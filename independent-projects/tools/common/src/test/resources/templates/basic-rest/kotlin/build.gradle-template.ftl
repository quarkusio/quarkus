// this block is necessary to make enforcedPlatform work for Quarkus plugin available
// only locally (snapshot) that is also importing the Quarkus BOM
buildscript {
    repositories {
        mavenLocal()
    }
    dependencies {
        classpath "io.quarkus:quarkus-gradle-plugin:${quarkusPluginVersion}"
    }
}

plugins {
    id 'org.jetbrains.kotlin.jvm' version "${kotlin_version}"
}

apply plugin: 'io.quarkus'

repositories {
     mavenLocal()
     mavenCentral()
}

dependencies {
    implementation enforcedPlatform("${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}")
    implementation 'io.quarkus:quarkus-resteasy'
    implementation 'org.jetbrains.kotlin:kotlin-stdlib-jdk8'

    testImplementation 'io.quarkus:quarkus-junit5'
    testImplementation 'io.rest-assured:kotlin-extensions'

    nativeTestImplementation 'io.quarkus:quarkus-junit5'
    nativeTestImplementation 'io.rest-assured:kotlin-extensions'
}

group '${project_groupId}'
version '${project_version}'

quarkus {
    setOutputDirectory("$projectDir/build/classes/kotlin/main")
}

quarkusDev {
    setSourceDir("$projectDir/src/main/kotlin")
}

