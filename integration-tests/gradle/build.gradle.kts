// This file doesn't configure integration tests and kept just to update gradle wrapper.
// To run tests use `./mvnw -f integration-tests/gradle test` from project root directory.

tasks.wrapper {
    // not sure if it's still required: IntelliJ works fine with `-bin` distribution 
    // after indexing Gradle API jars
    distributionType = Wrapper.DistributionType.ALL
}
