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
    id 'java'
}

apply plugin: 'io.quarkus'

repositories {
     mavenLocal()
     mavenCentral()
}

dependencies {
    implementation enforcedPlatform("${bom_groupId}:${bom_artifactId}:${bom_version}")
    implementation 'io.quarkus:quarkus-resteasy'

    testImplementation 'io.quarkus:quarkus-junit5'
    testImplementation 'io.rest-assured:rest-assured'

    nativeTestImplementation 'io.quarkus:quarkus-junit5'
    nativeTestImplementation 'io.rest-assured:rest-assured'
}

group '${project_groupId}'
version '${project_version}'

compileJava {
    options.compilerArgs << '-parameters'
}



