package io.quarkus.dockerfiles.spi;

/**
 * Represents different Linux distributions that can be used as base images in Dockerfiles.
 * Each distribution has its associated package manager and installation commands.
 */
public enum Distribution {
    /**
     * Red Hat Universal Base Image (UBI) - uses microdnf
     */
    UBI("microdnf", "microdnf install -y", "microdnf clean all"),

    /**
     * Fedora - uses dnf/microdnf
     */
    FEDORA("microdnf", "microdnf install -y", "microdnf clean all"),

    /**
     * Red Hat Enterprise Linux / CentOS - uses yum
     */
    RHEL("yum", "yum install -y", "yum clean all"),

    /**
     * Ubuntu - uses apt-get
     */
    UBUNTU("apt-get", "apt-get update && apt-get install -y", "apt-get clean && rm -rf /var/lib/apt/lists/*"),

    /**
     * Debian - uses apt-get
     */
    DEBIAN("apt-get", "apt-get update && apt-get install -y", "apt-get clean && rm -rf /var/lib/apt/lists/*"),

    /**
     * Alpine Linux - uses apk
     */
    ALPINE("apk", "apk add --no-cache", ""),

    /**
     * Unknown/unsupported distribution - fallback
     */
    UNKNOWN("", "", "");

    private final String packageManager;
    private final String installCommand;
    private final String cleanupCommand;

    Distribution(String packageManager, String installCommand, String cleanupCommand) {
        this.packageManager = packageManager;
        this.installCommand = installCommand;
        this.cleanupCommand = cleanupCommand;
    }

    public String getPackageManager() {
        return packageManager;
    }

    public String getInstallCommand() {
        return installCommand;
    }

    public String getCleanupCommand() {
        return cleanupCommand;
    }

    /**
     * Generate the complete RUN command for installing packages.
     *
     * @param packages the packages to install
     * @return the complete RUN command, or empty string if no packages or unknown distribution
     */
    public String generateInstallCommand(String packages) {
        if (this == UNKNOWN || packages == null || packages.trim().isEmpty()) {
            return "";
        }

        String command = installCommand + " " + packages.trim();
        if (!cleanupCommand.isEmpty()) {
            command += " && " + cleanupCommand;
        }
        return "RUN " + command;
    }
}