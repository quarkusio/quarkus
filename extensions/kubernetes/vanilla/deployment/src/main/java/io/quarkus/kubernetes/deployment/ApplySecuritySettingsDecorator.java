package io.quarkus.kubernetes.deployment;

import java.util.Optional;

import io.dekorate.kubernetes.decorator.Decorator;
import io.dekorate.kubernetes.decorator.NamedResourceDecorator;
import io.dekorate.kubernetes.decorator.ResourceProvidingDecorator;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.PodSecurityContextBuilder;
import io.fabric8.kubernetes.api.model.PodSpecFluent;
import io.fabric8.kubernetes.api.model.SELinuxOptions;
import io.fabric8.kubernetes.api.model.SELinuxOptionsBuilder;
import io.fabric8.kubernetes.api.model.SysctlBuilder;
import io.fabric8.kubernetes.api.model.WindowsSecurityContextOptions;
import io.fabric8.kubernetes.api.model.WindowsSecurityContextOptionsBuilder;

public class ApplySecuritySettingsDecorator extends NamedResourceDecorator<PodSpecFluent> {

    private final SecurityContextConfig securityContext;

    public ApplySecuritySettingsDecorator(String resourceName, SecurityContextConfig securityContext) {
        super(resourceName);
        this.securityContext = securityContext;
    }

    @Override
    public void andThenVisit(PodSpecFluent podSpec, ObjectMeta resourceMeta) {
        PodSecurityContextBuilder securityContextBuilder = new PodSecurityContextBuilder();

        securityContext.runAsUser().ifPresent(securityContextBuilder::withRunAsUser);
        securityContext.runAsGroup().ifPresent(securityContextBuilder::withRunAsGroup);
        securityContext.runAsNonRoot().ifPresent(securityContextBuilder::withRunAsNonRoot);
        securityContext.supplementalGroups().ifPresent(securityContextBuilder::addAllToSupplementalGroups);
        securityContext.fsGroup().ifPresent(securityContextBuilder::withFsGroup);
        securityContext.sysctls().entrySet().stream()
                .map(entry -> new SysctlBuilder().withName(entry.getKey()).withValue(entry.getValue()).build())
                .forEach(securityContextBuilder::addToSysctls);
        securityContext.fsGroupChangePolicy().map(e -> e.name()).ifPresent(securityContextBuilder::withFsGroupChangePolicy);
        buildSeLinuxOptions().ifPresent(securityContextBuilder::withSeLinuxOptions);
        buildWindowsOptions().ifPresent(securityContextBuilder::withWindowsOptions);

        podSpec.withSecurityContext(securityContextBuilder.build());
    }

    @Override
    public Class<? extends Decorator>[] after() {
        return new Class[] { ResourceProvidingDecorator.class };
    }

    private Optional<WindowsSecurityContextOptions> buildWindowsOptions() {
        WindowsSecurityContextOptions item = null;
        if (securityContext.windowsOptions().isAnyPropertySet()) {
            WindowsSecurityContextOptionsBuilder builder = new WindowsSecurityContextOptionsBuilder();
            securityContext.windowsOptions().gmsaCredentialSpec().ifPresent(builder::withGmsaCredentialSpec);
            securityContext.windowsOptions().gmsaCredentialSpecName().ifPresent(builder::withGmsaCredentialSpecName);
            securityContext.windowsOptions().hostProcess().ifPresent(builder::withHostProcess);
            securityContext.windowsOptions().runAsUserName().ifPresent(builder::withRunAsUserName);
            item = builder.build();
        }

        return Optional.ofNullable(item);
    }

    private Optional<SELinuxOptions> buildSeLinuxOptions() {
        SELinuxOptions item = null;
        if (securityContext.seLinuxOptions().isAnyPropertySet()) {
            SELinuxOptionsBuilder builder = new SELinuxOptionsBuilder();
            securityContext.seLinuxOptions().user().ifPresent(builder::withUser);
            securityContext.seLinuxOptions().role().ifPresent(builder::withRole);
            securityContext.seLinuxOptions().level().ifPresent(builder::withLevel);
            securityContext.seLinuxOptions().type().ifPresent(builder::withType);
            item = builder.build();
        }

        return Optional.ofNullable(item);
    }

}
