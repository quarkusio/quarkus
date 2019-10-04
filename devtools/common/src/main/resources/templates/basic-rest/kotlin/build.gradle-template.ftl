plugins {
    id 'org.jetbrains.kotlin.jvm' version "${kotlin_version}"
    id 'io.quarkus'
}

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
}

group '${project_groupId}'
version '${project_version}'

quarkus {
    setOutputDirectory("$projectDir/build/classes/kotlin/main")
}

quarkusDev {
    setSourceDir("$projectDir/src/main/kotlin")
}

