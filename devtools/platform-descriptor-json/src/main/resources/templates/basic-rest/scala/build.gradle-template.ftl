plugins {
    id 'scala'
    id 'io.quarkus'
}

repositories {
     mavenLocal()
     mavenCentral()
}

dependencies {
    implementation enforcedPlatform("${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}")
    implementation 'org.scala-lang:scala-library:${scala_version}'
    implementation 'io.quarkus:quarkus-resteasy'

    testImplementation 'io.quarkus:quarkus-junit5'
    testImplementation 'io.rest-assured:rest-assured'
}

group '${project_groupId}'
version '${project_version}'

