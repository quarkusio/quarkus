package io.quarkus.security.test;

/**
 * Sample main to boot the BuildAnnotationProcessor for testing/debugging the processor.
 * Not an actual testcase
 */
public class DebugAnnProcessor {
    public static void main(String[] args) throws Exception {
        String[] compArgs = { "-proc:only",
                "-processor", "io.quarkus.annotations.BuildAnnotationProcessor",
                "/Users/starksm/Dev/JBoss/Quarkus/starksm64-quarkus/security/runtime/src/main/java/io.quarkus/security/MPRealmConfig.java",
                "/Users/starksm/Dev/JBoss/Quarkus/starksm64-quarkus/security/deployment/src/main/java/io.quarkus/security/SecurityDeploymentProcessor.java" };
        // Have tools.jar in classpath and then run
        //com.sun.tools.javac.Main.main(compArgs);
    }
}
