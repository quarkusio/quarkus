package org.shamrock.security.test;

/**
 * Sample main to boot the BuildAnnotationProcessor for testing/debugging the processor.
 * Not an actual testcase
 */
public class DebugAnnProcessor {
    public static void main(String[] args) throws Exception {
        String[] compArgs = {"-proc:only",
        "-processor", "org.jboss.shamrock.annotations.BuildAnnotationProcessor",
        "/Users/starksm/Dev/JBoss/Protean/starksm64-protean-shamrock/security/runtime/src/main/java/org/jboss/shamrock/security/MPRealmConfig.java",
        "/Users/starksm/Dev/JBoss/Protean/starksm64-protean-shamrock/security/deployment/src/main/java/org/jboss/shamrock/security/SecurityDeploymentProcessor.java"};
        // Have tools.jar in classpath and then run
        //com.sun.tools.javac.Main.main(compArgs);
    }
}
