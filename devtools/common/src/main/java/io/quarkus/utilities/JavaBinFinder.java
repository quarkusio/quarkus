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
                if (OS.determineOS() == OS.WINDOWS) {
                    java = "java.exe";
                } else {
                    java = "java";
                }
            }
        }
        return java;
    }

    /**
     * Enum to classify the os.name system property
     */
    static enum OS {
        WINDOWS,
        LINUX,
        MAC,
        OTHER;

        private String version;

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        static OS determineOS() {
            final String osName = System.getProperty("os.name").toLowerCase();
            final OS os;
            if (osName.contains("windows")) {
                os = OS.WINDOWS;
            } else if (osName.contains("linux")
                    || osName.contains("freebsd")
                    || osName.contains("unix")
                    || osName.contains("sunos")
                    || osName.contains("solaris")
                    || osName.contains("aix")) {
                os = OS.LINUX;
            } else if (osName.contains("mac os")) {
                os = OS.MAC;
            } else {
                os = OS.OTHER;
            }

            os.setVersion(System.getProperty("os.version"));
            return os;
        }
    }
}
