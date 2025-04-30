allprojects {

    group = "org.acme"
    version = "1.0.0-SNAPSHOT"

    repositories {
        mavenLocal {
            content {
                includeGroupByRegex("io.quarkus.*")
                includeGroup("org.hibernate.orm")
            }
        }
        mavenCentral()
    }
}
