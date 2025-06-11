package io.quarkus.oidc.client.deployment;

import static io.quarkus.oidc.client.deployment.OidcClientFilterDeploymentHelper.sanitize;

import java.lang.reflect.Modifier;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Singleton;

import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;

import io.quarkus.arc.BeanDestroyer;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeansRuntimeInitBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.ApplicationArchive;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigBuilderBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.oidc.client.NamedOidcClient;
import io.quarkus.oidc.client.OidcClient;
import io.quarkus.oidc.client.OidcClients;
import io.quarkus.oidc.client.Tokens;
import io.quarkus.oidc.client.runtime.AbstractTokensProducer;
import io.quarkus.oidc.client.runtime.OidcClientBuildTimeConfig;
import io.quarkus.oidc.client.runtime.OidcClientDefaultIdConfigBuilder;
import io.quarkus.oidc.client.runtime.OidcClientRecorder;
import io.quarkus.oidc.client.runtime.OidcClientsImpl;
import io.quarkus.oidc.client.runtime.TokenProviderProducer;
import io.quarkus.oidc.client.runtime.TokensHelper;
import io.quarkus.oidc.client.runtime.TokensProducer;

@BuildSteps(onlyIf = OidcClientBuildStep.IsEnabled.class)
public class OidcClientBuildStep {

    @BuildStep
    ExtensionSslNativeSupportBuildItem enableSslInNative() {
        return new ExtensionSslNativeSupportBuildItem(Feature.OIDC_CLIENT);
    }

    @BuildStep
    void registerProvider(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder().setUnremovable();
        builder.addBeanClass(TokensProducer.class);
        builder.addBeanClass(TokenProviderProducer.class);
        additionalBeans.produce(builder.build());
    }

    @BuildStep
    void runtimeInitializeTokenHelper(BuildProducer<RuntimeInitializedClassBuildItem> runtime) {
        runtime.produce(new RuntimeInitializedClassBuildItem(TokensHelper.class.getName()));
    }

    @BuildStep
    void extractInjectedOidcClientNames(
            ApplicationArchivesBuildItem beanArchiveIndex,
            BuildProducer<OidcClientNamesBuildItem> oidcClientNames) {

        oidcClientNames.produce(new OidcClientNamesBuildItem(oidcClientNamesOf(beanArchiveIndex)));
    }

    private Set<String> oidcClientNamesOf(ApplicationArchivesBuildItem beanArchiveIndex) {
        return beanArchiveIndex.getAllApplicationArchives().stream()
                .map(ApplicationArchive::getIndex)
                .flatMap(archive -> archive.getAnnotations(DotName.createSimple(NamedOidcClient.class.getName())).stream())
                .map(annotation -> annotation.value().asString())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    @Consume(SyntheticBeansRuntimeInitBuildItem.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    void initOidcClients(OidcClientRecorder recorder) {
        recorder.initOidcClients();
    }

    @BuildStep
    AdditionalBeanBuildItem createOidcClientsBean() {
        return AdditionalBeanBuildItem.unremovableOf(OidcClientsImpl.class);
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    void produceNamedOidcClientBeans(OidcClientRecorder recorder, OidcClientNamesBuildItem oidcClientNames,
            BuildProducer<SyntheticBeanBuildItem> syntheticBean) {
        oidcClientNames.oidcClientNames().stream()
                .map(clientName -> syntheticNamedOidcClientBeanFor(clientName, recorder))
                .forEach(syntheticBean::produce);
    }

    private static SyntheticBeanBuildItem syntheticNamedOidcClientBeanFor(String clientName, OidcClientRecorder recorder) {
        return SyntheticBeanBuildItem.configure(OidcClient.class).unremovable()
                .types(OidcClient.class)
                .addInjectionPoint(ClassType.create(OidcClients.class))
                .createWith(recorder.createOidcClientBean(clientName))
                .scope(Singleton.class)
                .addQualifier().annotation(NamedOidcClient.class).addValue("value", clientName).done()
                .setRuntimeInit()
                .destroyer(BeanDestroyer.CloseableDestroyer.class)
                .done();
    }

    @BuildStep
    public void createNonDefaultTokensProducers(
            BuildProducer<GeneratedBeanBuildItem> generatedBean,
            OidcClientNamesBuildItem oidcClientNames) {

        ClassOutput classOutput = new GeneratedBeanGizmoAdaptor(generatedBean);

        String targetPackage = DotNames
                .internalPackageNameWithTrailingSlash(DotName.createSimple(TokensProducer.class.getName()));

        for (String oidcClientName : oidcClientNames.oidcClientNames()) {
            createNamedTokensProducerFor(classOutput, targetPackage, oidcClientName);
        }
    }

    @BuildStep
    RunTimeConfigBuilderBuildItem useOidcClientDefaultIdConfigBuilder() {
        return new RunTimeConfigBuilderBuildItem(OidcClientDefaultIdConfigBuilder.class);
    }

    /**
     * Creates a Tokens producer class like follows:
     *
     * <pre>
     * &#64;Singleton
     * public class TokensProducer_oidcClientName extends AbstractTokensProducer {
     *     &#64;Produces
     *     &#64;NamedOidcClient("oidcClientName")
     *     &#64;RequestScoped
     *     public Tokens produceTokens() {
     *         return awaitTokens();
     *     }
     *
     *     &#64;Override
     *     protected Optional<String> clientId() {
     *         return Optional.of("oidcClientName");
     *     }
     * }
     * </pre>
     */
    private String createNamedTokensProducerFor(ClassOutput classOutput, String targetPackage, String oidcClientName) {
        String generatedName = targetPackage + "TokensProducer_" + sanitize(oidcClientName);

        try (ClassCreator tokensProducer = ClassCreator.builder().classOutput(classOutput).className(generatedName)
                .superClass(AbstractTokensProducer.class)
                .build()) {
            tokensProducer.addAnnotation(DotNames.SINGLETON.toString());

            try (MethodCreator produceMethod = tokensProducer.getMethodCreator("produceTokens", Tokens.class)) {
                produceMethod.setModifiers(Modifier.PUBLIC);

                produceMethod.addAnnotation(DotNames.PRODUCES.toString());
                produceMethod.addAnnotation(NamedOidcClient.class.getName()).addValue("value", oidcClientName);
                produceMethod.addAnnotation(RequestScoped.class.getName());

                ResultHandle tokensResult = produceMethod.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(AbstractTokensProducer.class, "awaitTokens", Tokens.class),
                        produceMethod.getThis());

                produceMethod.returnValue(tokensResult);
            }

            try (MethodCreator clientIdMethod = tokensProducer.getMethodCreator("clientId", Optional.class)) {
                clientIdMethod.setModifiers(Modifier.PROTECTED);

                clientIdMethod.returnValue(clientIdMethod.invokeStaticMethod(
                        MethodDescriptor.ofMethod(Optional.class, "of", Optional.class, Object.class),
                        clientIdMethod.load(oidcClientName)));
            }
        }

        return generatedName.replace('/', '.');
    }

    public static class IsEnabled implements BooleanSupplier {
        OidcClientBuildTimeConfig config;

        public boolean getAsBoolean() {
            return config.enabled();
        }
    }
}
