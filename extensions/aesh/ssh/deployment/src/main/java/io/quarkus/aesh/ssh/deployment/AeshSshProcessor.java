package io.quarkus.aesh.ssh.deployment;

import jakarta.enterprise.inject.Vetoed;

import org.jboss.jandex.AnnotationTransformation;
import org.jboss.jandex.DotName;

import io.quarkus.aesh.deployment.AeshRemoteTransportBuildItem;
import io.quarkus.aesh.ssh.runtime.SshServerLifecycle;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;

class AeshSshProcessor {

    private static final DotName HEALTH_CHECK_CLASS = DotName
            .createSimple("io.quarkus.aesh.ssh.runtime.health.AeshSshHealthCheck");

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.AESH_SSH);
    }

    @BuildStep
    AdditionalBeanBuildItem registerBeans() {
        return AdditionalBeanBuildItem.unremovableOf(SshServerLifecycle.class);
    }

    @BuildStep
    AeshRemoteTransportBuildItem remoteTransport() {
        return new AeshRemoteTransportBuildItem("ssh");
    }

    @BuildStep
    HealthBuildItem addHealthCheck(AeshSshBuildTimeConfig buildTimeConfig, Capabilities capabilities) {
        if (!capabilities.isPresent(Capability.SMALLRYE_HEALTH)) {
            return null;
        }
        return new HealthBuildItem(
                "io.quarkus.aesh.ssh.runtime.health.AeshSshHealthCheck",
                buildTimeConfig.healthEnabled());
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void nativeImageConfiguration(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClasses,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            BuildProducer<NativeImageProxyDefinitionBuildItem> proxyDefinitions) {
        // MontgomeryCurve creates KeyPairGenerator instances (containing SecureRandom)
        // in its enum constants' static initializer. GraalVM does not allow Random
        // instances in the image heap, so defer to runtime.
        runtimeInitializedClasses.produce(
                new RuntimeInitializedClassBuildItem(
                        "org.apache.sshd.common.kex.MontgomeryCurve"));

        // SSHD loads security provider registrars via Class.forName() at runtime.
        // Register them and the providers they instantiate for reflection.
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(
                "org.apache.sshd.common.util.security.bouncycastle.BouncyCastleSecurityProviderRegistrar",
                "org.apache.sshd.common.util.security.eddsa.EdDSASecurityProviderRegistrar",
                "org.bouncycastle.jce.provider.BouncyCastleProvider",
                "net.i2p.crypto.eddsa.EdDSASecurityProvider")
                .build());

        // SshServerLifecycle.ensureSecurityProviders() uses reflection to clear
        // SecurityUtils's cached state (SECURITY_ENTITY_FACTORIES, REGISTERED_PROVIDERS,
        // REGISTRATION_STATE_HOLDER) so SSHD re-evaluates at runtime and uses JDK
        // default providers instead of BouncyCastle.
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(
                "org.apache.sshd.common.util.security.SecurityUtils")
                .fields().build());

        // SSHD's SecurityEntityFactory uses getDeclaredMethod() on JDK security
        // classes to create provider-specific factory methods. Register these
        // JDK classes for reflection so their methods are available at runtime.
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(
                "javax.crypto.KeyAgreement",
                "javax.crypto.Cipher",
                "javax.crypto.Mac",
                "java.security.KeyFactory",
                "java.security.KeyPairGenerator",
                "java.security.MessageDigest",
                "java.security.Signature",
                "java.security.cert.CertificateFactory")
                .methods().build());

        // SSHD creates dynamic proxies for event listener interfaces via
        // EventListenerUtils.proxyWrapper(). Register these for proxy generation.
        proxyDefinitions.produce(new NativeImageProxyDefinitionBuildItem(
                "org.apache.sshd.common.session.SessionListener"));
        proxyDefinitions.produce(new NativeImageProxyDefinitionBuildItem(
                "org.apache.sshd.common.channel.ChannelListener"));
        proxyDefinitions.produce(new NativeImageProxyDefinitionBuildItem(
                "org.apache.sshd.common.forward.PortForwardingEventListener"));
    }

    @BuildStep
    void disableHealthCheckIfNotNeeded(AeshSshBuildTimeConfig buildTimeConfig,
            Capabilities capabilities,
            BuildProducer<AnnotationsTransformerBuildItem> transformers) {
        // Veto the health check class if the SmallRye Health extension is not
        // present or if health checks are disabled by config. This prevents Arc
        // from trying to load the class (which implements HealthCheck and would
        // fail with NoClassDefFoundError when the health extension is absent).
        if (!capabilities.isPresent(Capability.SMALLRYE_HEALTH) || !buildTimeConfig.healthEnabled()) {
            transformers.produce(new AnnotationsTransformerBuildItem(
                    AnnotationTransformation.forClasses()
                            .whenClass(c -> c.name().equals(HEALTH_CHECK_CLASS))
                            .transform(ctx -> ctx.add(Vetoed.class))));
        }
    }
}
