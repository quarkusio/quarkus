package io.quarkus.kubernetes.deployment;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class SecurityContextConfig {

    /**
     * SELinuxOptions to be applied to the container.
     */
    SeLinuxOptions seLinuxOptions;

    /**
     * The Windows specific settings applied to all containers.
     */
    WindowsOptions windowsOptions;

    /**
     * The UID to run the entrypoint of the container process.
     */
    @ConfigItem
    Optional<Long> runAsUser;

    /**
     * The GID to run the entrypoint of the container process.
     */
    @ConfigItem
    Optional<Long> runAsGroup;

    /**
     * Indicates that the container must run as a non-root user.
     */
    @ConfigItem
    Optional<Boolean> runAsNonRoot;

    /**
     * A list of groups applied to the first process run in each container, in addition to the container's primary GID.
     * If unspecified, no groups will be added to any container.
     */
    @ConfigItem
    Optional<List<Long>> supplementalGroups;

    /**
     * A special supplemental group that applies to all containers in a pod.
     */
    @ConfigItem
    Optional<Long> fsGroup;

    /**
     * Sysctls hold a list of namespaced sysctls used for the pod.
     */
    @ConfigItem
    @ConfigDocMapKey("sysctl-name")
    Optional<Map<String, String>> sysctls;

    /**
     * It holds policies that will be used for applying fsGroup to a volume when volume is mounted.
     * Values: OnRootMismatch, Always
     */
    @ConfigItem
    Optional<PodFSGroupChangePolicy> fsGroupChangePolicy;

    protected boolean isAnyPropertySet() {
        return seLinuxOptions.isAnyPropertySet() || windowsOptions.isAnyPropertySet() || runAsUser.isPresent()
                || runAsGroup.isPresent() || runAsNonRoot.isPresent() || supplementalGroups.isPresent()
                || fsGroup.isPresent() || sysctls.isPresent() || fsGroupChangePolicy.isPresent();
    }

    @ConfigGroup
    public static class SeLinuxOptions {

        /**
         * The SELinux level label that applies to the container.
         */
        @ConfigItem
        Optional<String> level;

        /**
         * The SELinux role label that applies to the container.
         */
        @ConfigItem
        Optional<String> role;

        /**
         * The SELinux type label that applies to the container.
         */
        @ConfigItem
        Optional<String> type;

        /**
         * The SELinux user label that applies to the container.
         */
        @ConfigItem
        Optional<String> user;

        protected boolean isAnyPropertySet() {
            return level.isPresent() || role.isPresent() || type.isPresent() || user.isPresent();
        }
    }

    @ConfigGroup
    public static class WindowsOptions {

        /**
         * The name of the GMSA credential spec to use.
         */
        @ConfigItem
        Optional<String> gmsaCredentialSpecName;

        /**
         * GMSACredentialSpec is where the GMSA admission webhook (https://github.com/kubernetes-sigs/windows-gmsa) inlines the
         * contents of the GMSA credential spec named by the GMSACredentialSpecName field.
         */
        @ConfigItem
        Optional<String> gmsaCredentialSpec;

        /**
         * The UserName in Windows to run the entrypoint of the container process.
         */
        @ConfigItem
        Optional<String> runAsUserName;

        /**
         * HostProcess determines if a container should be run as a 'Host Process' container.
         */
        @ConfigItem
        Optional<Boolean> hostProcess;

        protected boolean isAnyPropertySet() {
            return gmsaCredentialSpecName.isPresent() || gmsaCredentialSpec.isPresent() || runAsUserName.isPresent()
                    || hostProcess.isPresent();
        }
    }

    public enum PodFSGroupChangePolicy {
        /**
         * It indicates that volume's ownership and permissions will be changed only when permission and ownership of root
         * directory does not match with expected permissions on the volume.
         */
        OnRootMismatch,
        /**
         * It indicates that volume's ownership and permissions should always be changed whenever volume is mounted inside a
         * Pod. This the default behavior.
         */
        Always;
    }
}
