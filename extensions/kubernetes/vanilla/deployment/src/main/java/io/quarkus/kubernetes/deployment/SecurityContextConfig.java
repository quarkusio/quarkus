package io.quarkus.kubernetes.deployment;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocMapKey;

public interface SecurityContextConfig {
    /**
     * SELinuxOptions to be applied to the container.
     */
    SeLinuxOptions seLinuxOptions();

    /**
     * The Windows specific settings applied to all containers.
     */
    WindowsOptions windowsOptions();

    /**
     * The UID to run the entrypoint of the container process.
     */
    Optional<Long> runAsUser();

    /**
     * The GID to run the entrypoint of the container process.
     */
    Optional<Long> runAsGroup();

    /**
     * Indicates that the container must run as a non-root user.
     */
    Optional<Boolean> runAsNonRoot();

    /**
     * A list of groups applied to the first process run in each container, in addition to the container's primary GID.
     * If unspecified, no groups will be added to any container.
     */
    Optional<List<Long>> supplementalGroups();

    /**
     * A special supplemental group that applies to all containers in a pod.
     */
    Optional<Long> fsGroup();

    /**
     * Sysctls hold a list of namespaced sysctls used for the pod.
     */
    @ConfigDocMapKey("sysctl-name")
    Map<String, String> sysctls();

    /**
     * It holds policies that will be used for applying fsGroup to a volume when volume is mounted.
     * Values: OnRootMismatch, Always
     */
    Optional<PodFSGroupChangePolicy> fsGroupChangePolicy();

    default boolean isAnyPropertySet() {
        return seLinuxOptions().isAnyPropertySet() || windowsOptions().isAnyPropertySet() || runAsUser().isPresent()
                || runAsGroup().isPresent() || runAsNonRoot().isPresent() || supplementalGroups().isPresent()
                || fsGroup().isPresent() || !sysctls().isEmpty() || fsGroupChangePolicy().isPresent();
    }

    interface SeLinuxOptions {
        /**
         * The SELinux level label that applies to the container.
         */
        Optional<String> level();

        /**
         * The SELinux role label that applies to the container.
         */
        Optional<String> role();

        /**
         * The SELinux type label that applies to the container.
         */
        Optional<String> type();

        /**
         * The SELinux user label that applies to the container.
         */
        Optional<String> user();

        default boolean isAnyPropertySet() {
            return level().isPresent() || role().isPresent() || type().isPresent() || user().isPresent();
        }
    }

    interface WindowsOptions {
        /**
         * The name of the GMSA credential spec to use.
         */
        Optional<String> gmsaCredentialSpecName();

        /**
         * GMSACredentialSpec is where the GMSA admission webhook
         * (<a href="https://github.com/kubernetes-sigs/windows-gmsa">windows-gsma</a>) inlines the contents of the
         * GMSA credential spec named by the GMSACredentialSpecName field.
         */
        Optional<String> gmsaCredentialSpec();

        /**
         * The UserName in Windows to run the entrypoint of the container process.
         */
        Optional<String> runAsUserName();

        /**
         * HostProcess determines if a container should be run as a 'Host Process' container.
         */
        Optional<Boolean> hostProcess();

        default boolean isAnyPropertySet() {
            return gmsaCredentialSpecName().isPresent() || gmsaCredentialSpec().isPresent() || runAsUserName().isPresent()
                    || hostProcess().isPresent();
        }
    }

    enum PodFSGroupChangePolicy {
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
