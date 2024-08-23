package io.quarkus.oidc.deployment;

import static io.quarkus.arc.processor.BuiltinScope.APPLICATION;
import static io.quarkus.arc.processor.DotNames.DEFAULT;
import static io.quarkus.arc.processor.DotNames.NAMED;
import static io.quarkus.oidc.common.runtime.OidcConstants.BEARER_SCHEME;
import static io.quarkus.oidc.common.runtime.OidcConstants.CODE_FLOW_CODE;
import static io.quarkus.oidc.runtime.OidcUtils.DEFAULT_TENANT_ID;
import static org.jboss.jandex.AnnotationTarget.Kind.CLASS;
import static org.jboss.jandex.AnnotationTarget.Kind.METHOD;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;

import jakarta.inject.Singleton;

import org.eclipse.microprofile.jwt.Claim;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanDiscoveryFinishedBuildItem;
import io.quarkus.arc.deployment.BeanRegistrationPhaseBuildItem;
import io.quarkus.arc.deployment.InjectionPointTransformerBuildItem;
import io.quarkus.arc.deployment.QualifierRegistrarBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeansRuntimeInitBuildItem;
import io.quarkus.arc.processor.Annotations;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.arc.processor.InjectionPointInfo;
import io.quarkus.arc.processor.InjectionPointsTransformer;
import io.quarkus.arc.processor.QualifierRegistrar;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.deployment.builditem.SystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.oidc.AuthorizationCodeFlow;
import io.quarkus.oidc.BearerTokenAuthentication;
import io.quarkus.oidc.IdToken;
import io.quarkus.oidc.Tenant;
import io.quarkus.oidc.TenantFeature;
import io.quarkus.oidc.TenantIdentityProvider;
import io.quarkus.oidc.TokenIntrospectionCache;
import io.quarkus.oidc.UserInfo;
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
import io.quarkus.oidc.runtime.OidcUtils;
import io.quarkus.oidc.runtime.TenantConfigBean;
import io.quarkus.oidc.runtime.providers.AzureAccessTokenCustomizer;
import io.quarkus.tls.TlsRegistryBuildItem;
import io.quarkus.vertx.core.deployment.CoreVertxBuildItem;
import io.quarkus.vertx.http.deployment.EagerSecurityInterceptorBindingBuildItem;
import io.quarkus.vertx.http.deployment.HttpAuthMechanismAnnotationBuildItem;
import io.quarkus.vertx.http.deployment.SecurityInformationBuildItem;
import io.quarkus.vertx.http.runtime.HttpBuildTimeConfig;
import io.smallrye.jwt.auth.cdi.ClaimValueProducer;
import io.smallrye.jwt.auth.cdi.CommonJwtProducer;
import io.smallrye.jwt.auth.cdi.JsonValueProducer;
import io.smallrye.jwt.auth.cdi.RawClaimTypeProducer;

@BuildSteps(onlyIf = OidcBuildStep.IsEnabled.class)
public class OidcBuildStep {
    private static final DotName TENANT_NAME = DotName.createSimple(Tenant.class);
    private static final DotName TENANT_FEATURE_NAME = DotName.createSimple(TenantFeature.class);
    private static final DotName TENANT_IDENTITY_PROVIDER_NAME = DotName.createSimple(TenantIdentityProvider.class);
    private static final Logger LOG = Logger.getLogger(OidcBuildStep.class);
    private static final DotName USER_INFO_NAME = DotName.createSimple(UserInfo.class);
    private static final DotName JSON_WEB_TOKEN_NAME = DotName.createSimple(JsonWebToken.class);
    private static final DotName ID_TOKEN_NAME = DotName.createSimple(IdToken.class);

    private static final String QUARKUS_TOKEN_PROPAGATION_PACKAGE = "io.quarkus.oidc.token.propagation";
    private static final String SMALLRYE_JWT_PACKAGE = "io.smallrye.jwt";

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
        // TODO: drop @TenantFeature qualifier when 'TenantFeatureFinder' stop using this annotation as a qualifier
        return new QualifierRegistrarBuildItem(new QualifierRegistrar() {
            @Override
            public Map<DotName, Set<String>> getAdditionalQualifiers() {
                return Map.of(TENANT_FEATURE_NAME, Set.of());
            }
        });
    }

    @BuildStep
    InjectionPointTransformerBuildItem makeTenantIdentityProviderInjectionPointsNamed() {
        // @Tenant annotation cannot be a qualifier as it is used on resource methods and lead to illegal states
        return new InjectionPointTransformerBuildItem(new InjectionPointsTransformer() {
            @Override
            public boolean appliesTo(Type requiredType) {
                return requiredType.name().equals(TENANT_IDENTITY_PROVIDER_NAME);
            }

            @Override
            public void transform(TransformationContext ctx) {
                if (ctx.getTarget().kind() == METHOD) {
                    ctx
                            .getAllAnnotations()
                            .stream()
                            .filter(a -> TENANT_NAME.equals(a.name()))
                            .forEach(a -> {
                                var annotationValue = new AnnotationValue[] {
                                        AnnotationValue.createStringValue("value", a.value().asString()) };
                                ctx
                                        .transform()
                                        .add(AnnotationInstance.create(NAMED, a.target(), annotationValue))
                                        .done();
                            });
                } else {
                    // field
                    var tenantAnnotation = Annotations.find(ctx.getAllAnnotations(), TENANT_NAME);
                    if (tenantAnnotation != null && tenantAnnotation.value() != null) {
                        ctx
                                .transform()
                                .add(NAMED, AnnotationValue.createStringValue("value", tenantAnnotation.value().asString()))
                                .done();
                    }
                }
            }
        });
    }

    /**
     * Produce {@link TenantIdentityProvider} with already selected tenant for each {@link TenantIdentityProvider}
     * injection point annotated with {@link Tenant} annotation.
     * For example, we produce {@link TenantIdentityProvider} with pre-selected tenant 'my-tenant' for injection point:
     *
     * <code>
     *  &#064;Inject
     *  &#064;Tenant("my-tenant")
     *  TenantIdentityProvider identityProvider;
     * </code>
     */
    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    void produceTenantIdentityProviders(BuildProducer<SyntheticBeanBuildItem> syntheticBeanProducer,
            OidcRecorder recorder, BeanDiscoveryFinishedBuildItem beans, CombinedIndexBuildItem combinedIndex) {
        if (!combinedIndex.getIndex().getAnnotations(TENANT_NAME).isEmpty()) {
            // create TenantIdentityProviders for tenants selected with @Tenant like: @Tenant("my-tenant")
            beans
                    .getInjectionPoints()
                    .stream()
                    .filter(OidcBuildStep::isTenantIdentityProviderType)
                    .filter(ip -> ip.getRequiredQualifier(NAMED) != null)
                    .map(ip -> ip.getRequiredQualifier(NAMED).value().asString())
                    .distinct()
                    .forEach(tenantName -> syntheticBeanProducer.produce(
                            SyntheticBeanBuildItem
                                    .configure(TenantIdentityProvider.class)
                                    .named(tenantName)
                                    .scope(APPLICATION.getInfo())
                                    .supplier(recorder.createTenantIdentityProvider(tenantName))
                                    .unremovable()
                                    .done()));
        }
        // create TenantIdentityProvider for default tenant when tenant is not explicitly selected via @Tenant
        boolean createTenantIdentityProviderForDefaultTenant = beans
                .getInjectionPoints()
                .stream()
                .filter(ip -> ip.getRequiredQualifier(NAMED) == null)
                .anyMatch(OidcBuildStep::isTenantIdentityProviderType);
        if (createTenantIdentityProviderForDefaultTenant) {
            syntheticBeanProducer.produce(
                    SyntheticBeanBuildItem
                            .configure(TenantIdentityProvider.class)
                            .scope(APPLICATION.getInfo())
                            .addQualifier(DEFAULT)
                            // named beans are implicitly default according to the specs
                            // when no other qualifiers are present other than @Named and @Any
                            // which means we need to handle ambiguous resolution
                            .alternative(true)
                            .priority(1)
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
            BeanRegistrationPhaseBuildItem beanRegistration,
            OidcConfig config,
            OidcRecorder recorder,
            CoreVertxBuildItem vertxBuildItem,
            TlsRegistryBuildItem tlsRegistryBuildItem) {
        return SyntheticBeanBuildItem.configure(TenantConfigBean.class).unremovable().types(TenantConfigBean.class)
                .supplier(recorder.createTenantConfigBean(config, vertxBuildItem.getVertx(),
                        tlsRegistryBuildItem.registry(), detectUserInfoRequired(beanRegistration)))
                .destroyer(TenantConfigBean.Destroyer.class)
                .scope(Singleton.class) // this should have been @ApplicationScoped but fails for some reason
                .setRuntimeInit()
                .done();
    }

    @Consume(SyntheticBeansRuntimeInitBuildItem.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    void initTenantConfigBean(OidcRecorder recorder) {
        recorder.initTenantConfigBean();
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    public void registerTenantResolverInterceptor(Capabilities capabilities, OidcRecorder recorder,
            HttpBuildTimeConfig buildTimeConfig,
            CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<EagerSecurityInterceptorBindingBuildItem> bindingProducer,
            BuildProducer<SystemPropertyBuildItem> systemPropertyProducer) {
        if (!buildTimeConfig.auth.proactive
                && (capabilities.isPresent(Capability.RESTEASY_REACTIVE) || capabilities.isPresent(Capability.RESTEASY))) {
            boolean foundTenantResolver = combinedIndexBuildItem
                    .getIndex()
                    .getAnnotations(TENANT_NAME)
                    .stream()
                    .map(AnnotationInstance::target)
                    // ignore field injection points and injection setters
                    // as we don't want to count in the TenantIdentityProvider injection point;
                    // if class is the target, we know it cannot be a TenantIdentityProvider as we produce it ourselves
                    .anyMatch(t -> isMethodWithTenantAnnButNotInjPoint(t) || t.kind() == CLASS);
            if (foundTenantResolver) {
                // register method interceptor that will be run before security checks
                bindingProducer.produce(
                        new EagerSecurityInterceptorBindingBuildItem(recorder.tenantResolverInterceptorCreator(), TENANT_NAME));
                systemPropertyProducer.produce(new SystemPropertyBuildItem(OidcUtils.ANNOTATION_BASED_TENANT_RESOLUTION_ENABLED,
                        Boolean.TRUE.toString()));
            }
        }
    }

    private static boolean isMethodWithTenantAnnButNotInjPoint(AnnotationTarget t) {
        return t.kind() == METHOD && !t.asMethod().isConstructor() && !t.hasAnnotation(DotNames.INJECT);
    }

    private static boolean detectUserInfoRequired(BeanRegistrationPhaseBuildItem beanRegistrationPhaseBuildItem) {
        return isInjected(beanRegistrationPhaseBuildItem, USER_INFO_NAME, null);
    }

    @BuildStep
    void detectAccessTokenVerificationRequired(BeanRegistrationPhaseBuildItem beanRegistrationPhaseBuildItem,
            BuildProducer<RunTimeConfigurationDefaultBuildItem> runtimeConfigDefaultProducer) {
        if (isInjected(beanRegistrationPhaseBuildItem, JSON_WEB_TOKEN_NAME, ID_TOKEN_NAME)) {
            runtimeConfigDefaultProducer.produce(
                    new RunTimeConfigurationDefaultBuildItem("quarkus.oidc.authentication.verify-access-token", "true"));
            runtimeConfigDefaultProducer.produce(
                    new RunTimeConfigurationDefaultBuildItem("quarkus.oidc.*.authentication.verify-access-token", "true"));
        }
    }

    @BuildStep
    List<HttpAuthMechanismAnnotationBuildItem> registerHttpAuthMechanismAnnotation() {
        return List.of(
                new HttpAuthMechanismAnnotationBuildItem(DotName.createSimple(AuthorizationCodeFlow.class), CODE_FLOW_CODE),
                new HttpAuthMechanismAnnotationBuildItem(DotName.createSimple(BearerTokenAuthentication.class), BEARER_SCHEME));
    }

    private static boolean isInjected(BeanRegistrationPhaseBuildItem beanRegistrationPhaseBuildItem, DotName requiredType,
            DotName withoutQualifier) {
        for (InjectionPointInfo injectionPoint : beanRegistrationPhaseBuildItem.getInjectionPoints()) {
            if (requiredType.equals(injectionPoint.getRequiredType().name())
                    && isApplicationPackage(injectionPoint.getTargetInfo())
                    && (withoutQualifier == null || injectionPoint.getRequiredQualifier(withoutQualifier) == null)) {
                LOG.debugf("%s injection point: %s", requiredType.toString(), injectionPoint.getTargetInfo());
                return true;
            }
        }
        return false;
    }

    private static boolean isApplicationPackage(String injectionPointTargetInfo) {
        return injectionPointTargetInfo != null
                && !injectionPointTargetInfo.startsWith(QUARKUS_TOKEN_PROPAGATION_PACKAGE)
                && !injectionPointTargetInfo.startsWith(SMALLRYE_JWT_PACKAGE);
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
