plugins {
    java
    id("io.quarkus")
}

repositories {
    if (System.getProperties().containsKey("maven.repo.local")) {
        maven {
            url = uri(System.getProperties().get("maven.repo.local") as String)
        }
    } else {
        mavenLocal()
    }
    mavenCentral()
}

val quarkusVersion: String by project

dependencies {
    implementation("io.quarkus:quarkus-jsonp")
    implementation("io.quarkus:quarkus-jsonb")
    constraints {
        implementation("io.quarkus:quarkus-jsonb:0.10.0") {
            because("to test constraints")
        }
    }
    implementation(enforcedPlatform("io.quarkus:quarkus-bom:${quarkusVersion}"))
    implementation("io.quarkus:quarkus-resteasy")

    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.rest-assured:rest-assured")
}

tasks {
    test {
        dependsOn("cleanTest")
        useJUnitPlatform()

        // @NativeImageTest and JVM mode tests can't be mixed in the same run
        setForkEvery(1)
    }
}
