plugins {
    java
}

repositories {
    mavenLocal()
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
