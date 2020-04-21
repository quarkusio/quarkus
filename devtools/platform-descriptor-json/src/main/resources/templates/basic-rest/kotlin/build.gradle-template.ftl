plugins {
    id 'org.jetbrains.kotlin.jvm' version "${kotlin_version}"
    id "org.jetbrains.kotlin.plugin.allopen" version "${kotlin_version}"
    id 'io.quarkus'
}

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
    kotlinOptions.javaParameters = true
}

compileTestKotlin {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8
}
