package io.quarkus.oidc.deployment;

import static io.quarkus.arc.processor.BuiltinScope.APPLICATION;
import static io.quarkus.arc.processor.DotNames.DEFAULT;
import static io.quarkus.oidc.runtime.OidcUtils.DEFAULT_TENANT_ID;
import static io.quarkus.vertx.http.deployment.EagerSecurityInterceptorCandidateBuildItem.hasProperEndpointModifiers;
import static org.jboss.jandex.AnnotationTarget.Kind.CLASS;
import static org.jboss.jandex.AnnotationTarget.Kind.METHOD;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import jakarta.inject.Singleton;

import org.eclipse.microprofile.jwt.Claim;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanDiscoveryFinishedBuildItem;
import io.quarkus.arc.deployment.QualifierRegistrarBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.processor.InjectionPointInfo;
import io.quarkus.arc.processor.QualifierRegistrar;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.oidc.Tenant;
import io.quarkus.oidc.TenantFeature;
import io.quarkus.oidc.TenantIdentityProvider;
import io.quarkus.oidc.TokenIntrospectionCache;
import io.quarkus.oidc.UserInfoCache;
import io.quarkus.oidc.runtime.BackChannelLogoutHandler;
import io.quarkus.oidc.runtime.DefaultTenantConfigResolver;
import io.quarkus.oidc.runtime.DefaultTokenIntrospectionUserInfoCache;
import io.quarkus.oidc.runtime.DefaultTokenStateManager;
import io.quarkus.oidc.runtime.OidcAuthenticationMechanism;
import io.quarkus.oidc.runtime.OidcConfig;
import io.quarkus.oidc.runtime.OidcConfigurationMetadataProducer;
import io.quarkus.oidc.runtime.OidcIdentityProvider;
import io.quarkus.oidc.runtime.OidcJsonWebTokenProducer;
import io.quarkus.oidc.runtime.OidcRecorder;
import io.quarkus.oidc.runtime.OidcSessionImpl;
import io.quarkus.oidc.runtime.OidcTokenCredentialProducer;
import io.quarkus.oidc.runtime.TenantConfigBean;
import io.quarkus.oidc.runtime.providers.AzureAccessTokenCustomizer;
import io.quarkus.runtime.TlsConfig;
import io.quarkus.vertx.core.deployment.CoreVertxBuildItem;
import io.quarkus.vertx.http.deployment.EagerSecurityInterceptorCandidateBuildItem;
import io.quarkus.vertx.http.deployment.SecurityInformationBuildItem;
import io.quarkus.vertx.http.runtime.HttpBuildTimeConfig;
import io.smallrye.jwt.auth.cdi.ClaimValueProducer;
import io.smallrye.jwt.auth.cdi.CommonJwtProducer;
import io.smallrye.jwt.auth.cdi.JsonValueProducer;
import io.smallrye.jwt.auth.cdi.RawClaimTypeProducer;
import io.vertx.ext.web.RoutingContext;

@BuildSteps(onlyIf = OidcBuildStep.IsEnabled.class)
public class OidcBuildStep {
    private static final DotName TENANT_NAME = DotName.createSimple(Tenant.class);
    private static final DotName TENANT_FEATURE_NAME = DotName.createSimple(TenantFeature.class);
    private static final DotName TENANT_IDENTITY_PROVIDER_NAME = DotName.createSimple(TenantIdentityProvider.class);
    private static final Logger LOG = Logger.getLogger(OidcBuildStep.class);

    @BuildStep
    public void provideSecurityInformation(BuildProducer<SecurityInformationBuildItem> securityInformationProducer) {
        // TODO: By default quarkus.oidc.application-type = service
        // Also look at other options (web-app, hybrid)
        securityInformationProducer
                .produce(SecurityInformationBuildItem.OPENIDCONNECT("quarkus.oidc.auth-server-url"));
    }

    @BuildStep
    AdditionalBeanBuildItem jwtClaimIntegration(Capabilities capabilities) {
        if (!capabilities.isPresent(Capability.JWT)) {
            AdditionalBeanBuildItem.Builder removable = AdditionalBeanBuildItem.builder();
            removable.addBeanClass(CommonJwtProducer.class);
            removable.addBeanClass(RawClaimTypeProducer.class);
            removable.addBeanClass(JsonValueProducer.class);
            removable.addBeanClass(ClaimValueProducer.class);
            removable.addBeanClass(Claim.class);
            return removable.build();
        }
        return null;
    }

    @BuildStep
    public void additionalBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder().setUnremovable();

        builder.addBeanClass(OidcAuthenticationMechanism.class)
                .addBeanClass(OidcJsonWebTokenProducer.class)
                .addBeanClass(OidcTokenCredentialProducer.class)
                .addBeanClass(OidcConfigurationMetadataProducer.class)
                .addBeanClass(OidcIdentityProvider.class)
                .addBeanClass(DefaultTenantConfigResolver.class)
                .addBeanClass(DefaultTokenStateManager.class)
                .addBeanClass(OidcSessionImpl.class)
                .addBeanClass(BackChannelLogoutHandler.class)
                .addBeanClass(AzureAccessTokenCustomizer.class);
        additionalBeans.produce(builder.build());
    }

    @BuildStep(onlyIf = IsCacheEnabled.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    public SyntheticBeanBuildItem addDefaultCacheBean(OidcConfig config,
            OidcRecorder recorder,
            CoreVertxBuildItem vertxBuildItem) {
        return SyntheticBeanBuildItem.configure(DefaultTokenIntrospectionUserInfoCache.class).unremovable()
                .types(DefaultTokenIntrospectionUserInfoCache.class, TokenIntrospectionCache.class, UserInfoCache.class)
                .supplier(recorder.setupTokenCache(config, vertxBuildItem.getVertx()))
                .scope(Singleton.class)
                .setRuntimeInit()
                .done();
    }

    @BuildStep
    ExtensionSslNativeSupportBuildItem enableSslInNative() {
        return new ExtensionSslNativeSupportBuildItem(Feature.OIDC);
    }

    @BuildStep
    QualifierRegistrarBuildItem addQualifiers() {
        // this seems to be necessary; I think it's because sometimes we only access beans
        // annotated with @TenantFeature programmatically and no injection point is annotated with it
        return new QualifierRegistrarBuildItem(new QualifierRegistrar() {
            @Override
            public Map<DotName, Set<String>> getAdditionalQualifiers() {
                return Map.of(TENANT_FEATURE_NAME, Set.of());
            }
        });
    }

    /**
     * Produce {@link OidcIdentityProvider} with already selected tenant for each {@link OidcIdentityProvider}
     * injection point annotated with {@link TenantFeature} annotation.
     * For example, we produce {@link OidcIdentityProvider} with pre-selected tenant 'my-tenant' for injection point:
     *
     * <code>
     *  &#064;Inject
     *  &#064;TenantFeature("my-tenant")
     *  OidcIdentityProvider identityProvider;
     * </code>
     */
    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    void produceTenantIdentityProviders(BuildProducer<SyntheticBeanBuildItem> syntheticBeanProducer,
            OidcRecorder recorder, BeanDiscoveryFinishedBuildItem beans, CombinedIndexBuildItem combinedIndex) {
        // create TenantIdentityProviders for tenants selected with @TenantFeature like: @TenantFeature("my-tenant")
        if (!combinedIndex.getIndex().getAnnotations(TENANT_FEATURE_NAME).isEmpty()) {
            // create TenantIdentityProviders for tenants selected with @TenantFeature like: @TenantFeature("my-tenant")
            beans
                    .getInjectionPoints()
                    .stream()
                    .filter(ip -> ip.getRequiredQualifier(TENANT_FEATURE_NAME) != null)
                    .filter(OidcBuildStep::isTenantIdentityProviderType)
                    .map(ip -> ip.getRequiredQualifier(TENANT_FEATURE_NAME).value().asString())
                    .distinct()
                    .forEach(tenantName -> syntheticBeanProducer.produce(
                            SyntheticBeanBuildItem
                                    .configure(TenantIdentityProvider.class)
                                    .addQualifier().annotation(TENANT_FEATURE_NAME).addValue("value", tenantName).done()
                                    .scope(APPLICATION.getInfo())
                                    .supplier(recorder.createTenantIdentityProvider(tenantName))
                                    .unremovable()
                                    .done()));
        }
        // create TenantIdentityProvider for default tenant when tenant is not explicitly selected via @TenantFeature
        boolean createTenantIdentityProviderForDefaultTenant = beans
                .getInjectionPoints()
                .stream()
                .filter(InjectionPointInfo::hasDefaultedQualifier)
                .anyMatch(OidcBuildStep::isTenantIdentityProviderType);
        if (createTenantIdentityProviderForDefaultTenant) {
            syntheticBeanProducer.produce(
                    SyntheticBeanBuildItem
                            .configure(TenantIdentityProvider.class)
                            .addQualifier(DEFAULT)
                            .scope(APPLICATION.getInfo())
                            .supplier(recorder.createTenantIdentityProvider(DEFAULT_TENANT_ID))
                            .unremovable()
                            .done());
        }
    }

    private static boolean isTenantIdentityProviderType(InjectionPointInfo ip) {
        return TENANT_IDENTITY_PROVIDER_NAME.equals(ip.getRequiredType().name());
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    public SyntheticBeanBuildItem setup(
            OidcConfig config,
            OidcRecorder recorder,
            CoreVertxBuildItem vertxBuildItem,
            TlsConfig tlsConfig) {
        return SyntheticBeanBuildItem.configure(TenantConfigBean.class).unremovable().types(TenantConfigBean.class)
                .supplier(recorder.setup(config, vertxBuildItem.getVertx(), tlsConfig))
                .destroyer(TenantConfigBean.Destroyer.class)
                .scope(Singleton.class) // this should have been @ApplicationScoped but fails for some reason
                .setRuntimeInit()
                .done();
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    public void produceTenantResolverInterceptors(CombinedIndexBuildItem indexBuildItem,
            Capabilities capabilities, OidcRecorder recorder,
            BuildProducer<EagerSecurityInterceptorCandidateBuildItem> producer,
            HttpBuildTimeConfig buildTimeConfig) {
        if (!buildTimeConfig.auth.proactive
                && (capabilities.isPresent(Capability.RESTEASY_REACTIVE) || capabilities.isPresent(Capability.RESTEASY))) {
            // provide method interceptor that will be run before security checks

            // collect endpoint candidates
            IndexView index = indexBuildItem.getIndex();
            Map<MethodInfo, String> candidateToTenant = new HashMap<>();

            for (AnnotationInstance annotation : index.getAnnotations(TENANT_NAME)) {

                // validate tenant id
                AnnotationTarget target = annotation.target();
                if (annotation.value() == null || annotation.value().asString().isEmpty()) {
                    LOG.warnf("Annotation instance @Tenant placed on %s did not provide valid tenant", toTargetName(target));
                    continue;
                }

                // collect annotation instance methods
                String tenant = annotation.value().asString();
                if (target.kind() == METHOD) {
                    MethodInfo method = target.asMethod();
                    if (hasProperEndpointModifiers(method)) {
                        candidateToTenant.put(method, tenant);
                    } else {
                        LOG.warnf("Method %s is not valid endpoint, but is annotated with the '@Tenant' annotation",
                                toTargetName(target));
                    }
                } else if (target.kind() == CLASS) {
                    // collect endpoint candidates; we only collect candidates, extensions like
                    // RESTEasy Reactive and others are still in control of endpoint selection and interceptors
                    // are going to be applied only on the actual endpoints
                    for (MethodInfo method : target.asClass().methods()) {
                        if (hasProperEndpointModifiers(method)) {
                            candidateToTenant.put(method, tenant);
                        }
                    }
                }
            }

            // create 'interceptor' for each tenant that puts tenant id into routing context
            if (!candidateToTenant.isEmpty()) {

                Map<String, Consumer<RoutingContext>> tenantToInterceptor = candidateToTenant
                        .values()
                        .stream()
                        .distinct()
                        .map(tenant -> Map.entry(tenant, recorder.createTenantResolverInterceptor(tenant)))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                candidateToTenant.forEach((method, tenant) -> {

                    // transform method info to description
                    String[] paramTypes = method.parameterTypes().stream().map(t -> t.name().toString()).toArray(String[]::new);
                    String className = method.declaringClass().name().toString();
                    String methodName = method.name();
                    var description = recorder.methodInfoToDescription(className, methodName, paramTypes);

                    producer.produce(new EagerSecurityInterceptorCandidateBuildItem(method, description,
                            tenantToInterceptor.get(tenant)));
                });
            }
        }
    }

    private static String toTargetName(AnnotationTarget target) {
        if (target.kind() == CLASS) {
            return target.asClass().name().toString();
        } else {
            return target.asMethod().declaringClass().name().toString() + "#" + target.asMethod().name();
        }
    }

    public static class IsEnabled implements BooleanSupplier {
        OidcBuildTimeConfig config;

        public boolean getAsBoolean() {
            return config.enabled;
        }
    }

    public static class IsCacheEnabled implements BooleanSupplier {
        OidcBuildTimeConfig config;

        public boolean getAsBoolean() {
            return config.enabled && config.defaultTokenCacheEnabled;
        }
    }
}
