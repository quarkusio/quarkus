package io.quarkus.resteasy.deployment;

import java.lang.reflect.Modifier;

import org.jboss.resteasy.core.ResourceMethodInvoker;
import org.jboss.resteasy.spi.ResourceFactory;
import org.objectweb.asm.Opcodes;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.gizmo.ClassTransformer;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.resteasy.common.spi.ResteasyJaxrsProviderBuildItem;
import io.quarkus.resteasy.runtime.AuthenticationCompletionExceptionMapper;
import io.quarkus.resteasy.runtime.AuthenticationFailedExceptionMapper;
import io.quarkus.resteasy.runtime.AuthenticationRedirectExceptionMapper;
import io.quarkus.resteasy.runtime.CompositeExceptionMapper;
import io.quarkus.resteasy.runtime.EagerSecurityFilter;
import io.quarkus.resteasy.runtime.ForbiddenExceptionMapper;
import io.quarkus.resteasy.runtime.JaxRsPermissionChecker;
import io.quarkus.resteasy.runtime.SecurityContextFilter;
import io.quarkus.resteasy.runtime.StandardSecurityCheckInterceptor;
import io.quarkus.resteasy.runtime.UnauthorizedExceptionMapper;
import io.quarkus.resteasy.runtime.vertx.JsonArrayReader;
import io.quarkus.resteasy.runtime.vertx.JsonArrayWriter;
import io.quarkus.resteasy.runtime.vertx.JsonObjectReader;
import io.quarkus.resteasy.runtime.vertx.JsonObjectWriter;
import io.quarkus.security.spi.DefaultSecurityCheckBuildItem;
import io.quarkus.vertx.http.runtime.security.JaxRsPathMatchingHttpSecurityPolicy;

public class ResteasyBuiltinsProcessor {

    protected static final String META_INF_RESOURCES = "META-INF/resources";

    @BuildStep
    void setUpDenyAllJaxRs(JaxRsSecurityConfig securityConfig,
            BuildProducer<DefaultSecurityCheckBuildItem> defaultSecurityCheckProducer) {
        if (securityConfig.denyUnannotatedEndpoints()) {
            defaultSecurityCheckProducer.produce(DefaultSecurityCheckBuildItem.denyAll());
        } else if (securityConfig.defaultRolesAllowed().isPresent()) {
            defaultSecurityCheckProducer
                    .produce(DefaultSecurityCheckBuildItem.rolesAllowed(securityConfig.defaultRolesAllowed().get()));
        }
    }

    /**
     * Install the JAX-RS security provider.
     */
    @BuildStep
    void setUpSecurity(BuildProducer<ResteasyJaxrsProviderBuildItem> providers,
            BuildProducer<BytecodeTransformerBuildItem> bytecodeTransformerProducer,
            BuildProducer<AdditionalBeanBuildItem> additionalBeanBuildItem, Capabilities capabilities) {
        providers.produce(new ResteasyJaxrsProviderBuildItem(UnauthorizedExceptionMapper.class.getName()));
        providers.produce(new ResteasyJaxrsProviderBuildItem(ForbiddenExceptionMapper.class.getName()));
        providers.produce(new ResteasyJaxrsProviderBuildItem(AuthenticationFailedExceptionMapper.class.getName()));
        providers.produce(new ResteasyJaxrsProviderBuildItem(AuthenticationRedirectExceptionMapper.class.getName()));
        providers.produce(new ResteasyJaxrsProviderBuildItem(AuthenticationCompletionExceptionMapper.class.getName()));
        providers.produce(new ResteasyJaxrsProviderBuildItem(CompositeExceptionMapper.class.getName()));
        if (capabilities.isPresent(Capability.SECURITY)) {
            providers.produce(new ResteasyJaxrsProviderBuildItem(SecurityContextFilter.class.getName()));
            additionalBeanBuildItem.produce(AdditionalBeanBuildItem.unremovableOf(SecurityContextFilter.class));
            providers.produce(new ResteasyJaxrsProviderBuildItem(EagerSecurityFilter.class.getName()));
            additionalBeanBuildItem.produce(AdditionalBeanBuildItem.unremovableOf(EagerSecurityFilter.class));
            transformEagerSecurityNativeMethod(bytecodeTransformerProducer);
            additionalBeanBuildItem.produce(AdditionalBeanBuildItem.unremovableOf(JaxRsPathMatchingHttpSecurityPolicy.class));
            additionalBeanBuildItem.produce(AdditionalBeanBuildItem.unremovableOf(JaxRsPermissionChecker.class));
            additionalBeanBuildItem.produce(
                    AdditionalBeanBuildItem.unremovableOf(StandardSecurityCheckInterceptor.RolesAllowedInterceptor.class));
            additionalBeanBuildItem.produce(AdditionalBeanBuildItem
                    .unremovableOf(StandardSecurityCheckInterceptor.PermissionsAllowedInterceptor.class));
            additionalBeanBuildItem.produce(
                    AdditionalBeanBuildItem.unremovableOf(StandardSecurityCheckInterceptor.PermitAllInterceptor.class));
            additionalBeanBuildItem.produce(
                    AdditionalBeanBuildItem.unremovableOf(StandardSecurityCheckInterceptor.AuthenticatedInterceptor.class));
        }
    }

    @BuildStep
    void vertxProviders(BuildProducer<ResteasyJaxrsProviderBuildItem> providers) {
        // These providers should work even if jackson-databind is not on the classpath
        providers.produce(new ResteasyJaxrsProviderBuildItem(JsonArrayReader.class.getName()));
        providers.produce(new ResteasyJaxrsProviderBuildItem(JsonArrayWriter.class.getName()));
        providers.produce(new ResteasyJaxrsProviderBuildItem(JsonObjectReader.class.getName()));
        providers.produce(new ResteasyJaxrsProviderBuildItem(JsonObjectWriter.class.getName()));
    }

    private static void transformEagerSecurityNativeMethod(
            BuildProducer<BytecodeTransformerBuildItem> bytecodeTransformerProducer) {

        // 1. add getResourceFactory() to ResourceMethodInvoker
        bytecodeTransformerProducer.produce(new BytecodeTransformerBuildItem(ResourceMethodInvoker.class.getName(),
                (cls, classVisitor) -> {
                    var classTransformer = new ClassTransformer(cls);
                    try (var getResourceFactory = classTransformer.addMethod("getResourceFactory", ResourceFactory.class)) {
                        getResourceFactory.setModifiers(Modifier.PUBLIC);
                        var resourceFieldDesc = FieldDescriptor.of(ResourceMethodInvoker.class, "resource",
                                ResourceFactory.class);
                        var resourceField = getResourceFactory.readInstanceField(resourceFieldDesc,
                                getResourceFactory.getThis());
                        getResourceFactory.returnValue(resourceField);
                    }
                    return classTransformer.applyTo(classVisitor);
                }));

        // 2. Create method that returns the field
        bytecodeTransformerProducer.produce(new BytecodeTransformerBuildItem(EagerSecurityFilter.class.getName(),
                (cls, classVisitor) -> {
                    var classTransformer = new ClassTransformer(cls);
                    var methodDescriptor = MethodDescriptor.ofMethod(EagerSecurityFilter.class, "getResourceFactory",
                            ResourceFactory.class, ResourceMethodInvoker.class);

                    classTransformer.removeMethod(methodDescriptor);
                    // we know that 'native' method ^ is deleted first as methods are creates in 'visitEnd'
                    // while the method is deleted by simply being ignored in 'visitMethod'

                    // now create:
                    // static ResourceFactory getResourceFactory(ResourceMethodInvoker invoker) {
                    //      return invoker.getResourceFactory();
                    // }
                    try (var methodCreator = classTransformer.addMethod(methodDescriptor)) {
                        methodCreator.setModifiers(Opcodes.ACC_STATIC);
                        var invoker = methodCreator.getMethodParam(0);
                        var getResourceFactoryDescriptor = MethodDescriptor.ofMethod(ResourceMethodInvoker.class,
                                "getResourceFactory", ResourceFactory.class);
                        var resource = methodCreator.invokeVirtualMethod(getResourceFactoryDescriptor, invoker);
                        methodCreator.returnValue(resource);
                    }
                    return classTransformer.applyTo(classVisitor);
                }));
    }
}
