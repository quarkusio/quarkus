package io.quarkus.kubernetes.deployment;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.PodSecurityContext;
import io.fabric8.kubernetes.api.model.PodSecurityContextBuilder;
import io.fabric8.kubernetes.api.model.SELinuxOptions;
import io.fabric8.kubernetes.api.model.SELinuxOptionsBuilder;
import io.fabric8.kubernetes.api.model.Sysctl;
import io.fabric8.kubernetes.api.model.WindowsSecurityContextOptions;
import io.fabric8.kubernetes.api.model.WindowsSecurityContextOptionsBuilder;
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

    /**
     * Controls whether a process can gain more privileges than its parent process.
     * This directly controls whether the {@code no_new_privs} flag gets set on the container process.
     * Always true when the container:
     * <ul>
     * <li>is run as privileged, or</li>
     * <li>has {@code CAP_SYS_ADMIN}</li>
     * </ul>
     */
    Optional<Boolean> allowPrivilegeEscalation();

    /**
     * Mounts the container's root filesystem as read-only
     */
    Optional<Boolean> readOnlyRootFilesystem();

    default boolean isAnyPropertySet() {
        return seLinuxOptions().isAnyPropertySet() || windowsOptions().isAnyPropertySet() || runAsUser().isPresent()
                || runAsGroup().isPresent() || runAsNonRoot().isPresent() || supplementalGroups().isPresent()
                || fsGroup().isPresent() || !sysctls().isEmpty() || fsGroupChangePolicy().isPresent()
                || allowPrivilegeEscalation().isPresent() || readOnlyRootFilesystem().isPresent();
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

    default PodSecurityContext buildSecurityContext() {
        PodSecurityContextBuilder securityContextBuilder = new PodSecurityContextBuilder();

        runAsUser().ifPresent(securityContextBuilder::withRunAsUser);
        runAsGroup().ifPresent(securityContextBuilder::withRunAsGroup);
        runAsNonRoot().ifPresent(securityContextBuilder::withRunAsNonRoot);
        supplementalGroups().ifPresent(securityContextBuilder::addAllToSupplementalGroups);
        fsGroup().ifPresent(securityContextBuilder::withFsGroup);
        sysctls().entrySet().stream()
                .map(e -> new Sysctl(e.getKey(), e.getValue()))
                .forEach(securityContextBuilder::addToSysctls);
        fsGroupChangePolicy().map(Enum::name).ifPresent(securityContextBuilder::withFsGroupChangePolicy);
        buildSeLinuxOptions().ifPresent(securityContextBuilder::withSeLinuxOptions);
        buildWindowsOptions().ifPresent(securityContextBuilder::withWindowsOptions);

        return securityContextBuilder.build();
    }

    default Optional<WindowsSecurityContextOptions> buildWindowsOptions() {
        final var windowsOptions = windowsOptions();
        if (windowsOptions.isAnyPropertySet()) {
            WindowsSecurityContextOptionsBuilder builder = new WindowsSecurityContextOptionsBuilder();
            windowsOptions.gmsaCredentialSpec().ifPresent(builder::withGmsaCredentialSpec);
            windowsOptions.gmsaCredentialSpecName().ifPresent(builder::withGmsaCredentialSpecName);
            windowsOptions.hostProcess().ifPresent(builder::withHostProcess);
            windowsOptions.runAsUserName().ifPresent(builder::withRunAsUserName);
            return Optional.of(builder.build());
        }

        return Optional.empty();
    }

    default Optional<SELinuxOptions> buildSeLinuxOptions() {
        final var seLinuxOptions = seLinuxOptions();
        if (seLinuxOptions.isAnyPropertySet()) {
            SELinuxOptionsBuilder builder = new SELinuxOptionsBuilder();
            seLinuxOptions.user().ifPresent(builder::withUser);
            seLinuxOptions.role().ifPresent(builder::withRole);
            seLinuxOptions.level().ifPresent(builder::withLevel);
            seLinuxOptions.type().ifPresent(builder::withType);
            return Optional.of(builder.build());
        }

        return Optional.empty();
    }
}
