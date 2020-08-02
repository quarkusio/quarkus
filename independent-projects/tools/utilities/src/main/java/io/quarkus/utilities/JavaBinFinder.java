package io.quarkus.utilities;

import java.io.File;

public class JavaBinFinder {
    /**
     * Search for the java command in the order:
     * 1. maven-toolchains plugin configuration
     * 2. java.home location
     * 3. java[.exe] on the system path
     *
     * @return the java command to use
     */
    public static String findBin() {
        // use the same JVM as the one used to run Maven (the "java.home" one)
        String java = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        File javaCheck = new File(java);
        if (!javaCheck.canExecute()) {

            java = null;
            // Try executable extensions if windows
            if (OS.determineOS() == OS.WINDOWS && System.getenv().containsKey("PATHEXT")) {
                String extpath = System.getenv("PATHEXT");
                String[] exts = extpath.split(";");
                for (String ext : exts) {
                    File winExe = new File(javaCheck.getAbsolutePath() + ext);
                    if (winExe.canExecute()) {
                        java = winExe.getAbsolutePath();
                        break;
                    }
                }
            }
            // Fallback to java on the path
            if (java == null) {
                java = simpleBinaryName();
            }
        }
        return java;
    }

    public static String simpleBinaryName() {
        if (OS.determineOS() == OS.WINDOWS) {
            return "java.exe";
        }
        return "java";
    }

}
