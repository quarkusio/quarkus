// this block is necessary to make enforcedPlatform work for Quarkus plugin available
// only locally (snapshot) that is also importing the Quarkus BOM
buildscript {
    repositories {
        mavenLocal()
    }
    dependencies {
        classpath "io.quarkus:quarkus-gradle-plugin:${quarkusVersion}"
    }
}

plugins {
    id 'org.jetbrains.kotlin.jvm' version "${kotlin_version}"
    id "org.jetbrains.kotlin.plugin.allopen" version "${kotlin_version}"
}

apply plugin: 'io.quarkus'

repositories {
     mavenLocal()
     mavenCentral()
}

dependencies {
    implementation enforcedPlatform("io.quarkus:quarkus-bom:${quarkusVersion}")
    implementation 'io.quarkus:quarkus-resteasy'
    implementation 'org.jetbrains.kotlin:kotlin-stdlib-jdk8'

    testImplementation 'io.quarkus:quarkus-junit5'
    testImplementation 'io.rest-assured:rest-assured'

    nativeTestImplementation 'io.quarkus:quarkus-junit5'
    nativeTestImplementation 'io.rest-assured:rest-assured'
}

group '${project_groupId}'
version '${project_version}'

quarkus {
    setOutputDirectory("$projectDir/build/classes/kotlin/main")
}

quarkusDev {
    setSourceDir("$projectDir/src/main/kotlin")
}

allOpen {
    annotation("javax.ws.rs.Path")
    annotation("javax.enterprise.context.ApplicationScoped")
    annotation("io.quarkus.test.junit.QuarkusTest")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

compileKotlin {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8
}

compileTestKotlin {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8
}
