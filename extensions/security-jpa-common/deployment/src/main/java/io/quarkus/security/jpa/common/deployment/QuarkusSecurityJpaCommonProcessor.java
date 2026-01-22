package io.quarkus.security.jpa.common.deployment;

import static io.quarkus.security.jpa.common.deployment.JpaSecurityIdentityUtil.getSingleAnnotatedElement;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_STATIC;

import java.util.List;
import java.util.Optional;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationIndexBuildItem;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.gizmo.ClassTransformer;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.security.jpa.Password;
import io.quarkus.security.jpa.Roles;
import io.quarkus.security.jpa.UserDefinition;
import io.quarkus.security.jpa.Username;
import io.quarkus.security.jpa.common.runtime.JpaIdentityProviderUtil;

class QuarkusSecurityJpaCommonProcessor {

    private static final DotName DOTNAME_USER_DEFINITION = DotName.createSimple(UserDefinition.class.getName());
    private static final DotName DOTNAME_USERNAME = DotName.createSimple(Username.class.getName());
    private static final DotName DOTNAME_ROLES = DotName.createSimple(Roles.class.getName());
    static final DotName DOTNAME_PASSWORD = DotName.createSimple(Password.class.getName());

    @BuildStep
    void provideJpaSecurityDefinition(ApplicationIndexBuildItem index, PanacheEntityPredicateBuildItem panacheEntityPredicate,
            BuildProducer<JpaSecurityDefinitionBuildItem> producer) {

        // Generate an IdentityProvider if we have a @UserDefinition
        List<AnnotationInstance> userDefinitions = index.getIndex().getAnnotations(DOTNAME_USER_DEFINITION);
        if (userDefinitions.size() > 1) {
            throw new RuntimeException("You can only annotate one class with @UserDefinition");
        } else if (!userDefinitions.isEmpty()) {
            ClassInfo userDefinitionClass = userDefinitions.get(0).target().asClass();
            AnnotationTarget annotatedUsername = getSingleAnnotatedElement(index.getIndex(), DOTNAME_USERNAME);
            AnnotationTarget annotatedPassword = getSingleAnnotatedElement(index.getIndex(), DOTNAME_PASSWORD);
            AnnotationTarget annotatedRoles = getSingleAnnotatedElement(index.getIndex(), DOTNAME_ROLES);
            // collect associated getters if required
            JpaSecurityDefinition jpaSecurityDefinition = new JpaSecurityDefinition(index.getIndex(),
                    userDefinitionClass,
                    panacheEntityPredicate.isPanache(userDefinitionClass),
                    annotatedUsername,
                    annotatedPassword,
                    annotatedRoles);
            producer.produce(new JpaSecurityDefinitionBuildItem(jpaSecurityDefinition));
        }
    }

    /**
     * This method produces {@link io.quarkus.security.jpa.SecurityJpa} CDI bean producer for either Hibernate ORM
     * or Hibernate Reactive version of the Quarkus Security JPA.
     */
    @BuildStep
    void registerSecurityJpaImplCdiBean(BuildProducer<BytecodeTransformerBuildItem> bytecodeTransformerProducer,
            BuildProducer<AdditionalBeanBuildItem> additionalBeanProducer,
            Optional<SecurityJpaProviderInfoBuildItem> securityJpaProviderClassBuildItem) {
        if (securityJpaProviderClassBuildItem.isPresent()) {
            var item = securityJpaProviderClassBuildItem.get();
            additionalBeanProducer.produce(AdditionalBeanBuildItem.unremovableOf(item.securityJpaProviderClass));
            bytecodeTransformerProducer
                    .produce(new BytecodeTransformerBuildItem(item.securityJpaProviderClass.getName(), (cls, classVisitor) -> {
                        var newInstanceUtilMethodDesc = MethodDescriptor.ofMethod(JpaIdentityProviderUtil.class, "newInstance",
                                Object.class, Class.class);
                        var classTransformer = new ClassTransformer(cls);
                        classTransformer.removeMethod("newJpaIdentityProvider", item.jpaIdentityProviderClass);
                        try (var mc = classTransformer.addMethod("newJpaIdentityProvider", item.jpaIdentityProviderClass)) {
                            mc.setModifiers(ACC_PRIVATE | ACC_STATIC);
                            // generates method similar to:
                            // private static JpaIdentityProvider newJpaIdentityProvider() {
                            //     return JpaIdentityProviderUtil.newInstance(MyEntity__JpaIdentityProviderImpl.class);
                            // }
                            var clazz = mc.loadClassFromTCCL(item.jpaIdentityProviderImplName);
                            // we don't use "new MyEntity__JpaIdentityProviderImpl()" because the generated class needs TCCL
                            var newInstance = mc.invokeStaticMethod(newInstanceUtilMethodDesc, clazz);
                            mc.returnValue(newInstance);
                        }
                        classTransformer.removeMethod("newJpaTrustedIdentityProvider", item.jpaTrustedIdentityProviderClass);
                        try (var mc = classTransformer.addMethod("newJpaTrustedIdentityProvider",
                                item.jpaTrustedIdentityProviderClass)) {
                            mc.setModifiers(ACC_PRIVATE | ACC_STATIC);
                            // generates method similar to:
                            // private static JpaTrustedIdentityProvider newJpaTrustedIdentityProvider() {
                            //     return JpaIdentityProviderUtil.newInstance(MyEntity__JpaTrustedIdentityProviderImpl.class);
                            // }
                            var clazz = mc.loadClassFromTCCL(item.jpaTrustedIdentityProviderImplName);
                            // we don't use "new MyEntity__JpaTrustedIdentityProviderImpl()",
                            // because the generated class needs TCCL
                            var newInstance = mc.invokeStaticMethod(newInstanceUtilMethodDesc, clazz);
                            mc.returnValue(newInstance);
                        }
                        return classTransformer.applyTo(classVisitor);
                    }));
        }
    }
}
