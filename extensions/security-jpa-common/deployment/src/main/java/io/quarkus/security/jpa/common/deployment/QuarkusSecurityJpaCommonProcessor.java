package io.quarkus.security.jpa.common.deployment;

import static io.quarkus.security.jpa.common.deployment.JpaSecurityIdentityUtil.getSingleAnnotatedElement;

import java.util.List;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationIndexBuildItem;
import io.quarkus.security.jpa.Password;
import io.quarkus.security.jpa.Roles;
import io.quarkus.security.jpa.UserDefinition;
import io.quarkus.security.jpa.Username;

class QuarkusSecurityJpaCommonProcessor {

    private static final DotName DOTNAME_USER_DEFINITION = DotName.createSimple(UserDefinition.class.getName());
    private static final DotName DOTNAME_USERNAME = DotName.createSimple(Username.class.getName());
    private static final DotName DOTNAME_ROLES = DotName.createSimple(Roles.class.getName());
    static final DotName DOTNAME_PASSWORD = DotName.createSimple(Password.class.getName());

    @BuildStep
    void provideJpaSecurityDefinition(ApplicationIndexBuildItem index,
            PanacheEntityPredicateBuildItem panacheEntityPredicate,
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
                    userDefinitionClass, panacheEntityPredicate.isPanache(userDefinitionClass), annotatedUsername,
                    annotatedPassword, annotatedRoles);
            producer.produce(new JpaSecurityDefinitionBuildItem(jpaSecurityDefinition));
        }
    }

}
