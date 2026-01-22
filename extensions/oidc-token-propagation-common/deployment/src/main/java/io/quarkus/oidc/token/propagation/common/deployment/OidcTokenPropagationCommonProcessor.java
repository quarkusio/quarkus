package io.quarkus.oidc.token.propagation.common.deployment;

import static java.util.stream.Collectors.groupingBy;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.oidc.token.propagation.common.AccessToken;

public class OidcTokenPropagationCommonProcessor {

    private static final DotName ACCESS_TOKEN = DotName.createSimple(AccessToken.class.getName());

    @BuildStep
    public List<AccessTokenInstanceBuildItem> collectAccessTokenInstances(CombinedIndexBuildItem index) {
        record ItemBuilder(AnnotationInstance instance, AnnotationTarget annotationTarget) {

            ItemBuilder(AnnotationInstance instance) {
                this(instance, instance.target());
            }

            private String toClientName() {
                var value = instance.value("exchangeTokenClient");
                return value == null || value.asString().equals("Default") ? "" : value.asString();
            }

            private boolean toExchangeToken() {
                return instance.value("exchangeTokenClient") != null;
            }

            private MethodInfo methodInfo() {
                if (annotationTarget.kind() == AnnotationTarget.Kind.METHOD) {
                    return annotationTarget.asMethod();
                }
                return null;
            }

            private String targetClassName() {
                if (annotationTarget.kind() == AnnotationTarget.Kind.METHOD) {
                    return annotationTarget.asMethod().declaringClass().name().toString();
                }
                return annotationTarget.asClass().name().toString();
            }

            private AccessTokenInstanceBuildItem build() {
                return new AccessTokenInstanceBuildItem(toClientName(), toExchangeToken(), annotationTarget, methodInfo());
            }
        }
        var accessTokenAnnotations = index.getIndex().getAnnotations(ACCESS_TOKEN);
        var itemBuilders = accessTokenAnnotations.stream().map(ItemBuilder::new)
                .collect(Collectors.toCollection(ArrayList::new));
        if (!itemBuilders.isEmpty()) {
            var targetClassToBuilders = itemBuilders.stream().collect(groupingBy(ItemBuilder::targetClassName));
            targetClassToBuilders.forEach((targetClassName, classBuilders) -> {
                if (classBuilders.size() > 1) {
                    var classLevelAnnotations = classBuilders.stream().filter(b -> b.methodInfo() == null).toList();
                    if (!classLevelAnnotations.isEmpty()) {
                        var classItemBuilder = classLevelAnnotations.get(0);
                        // now we have @AccessToken on both class and method, so remove the class-level one
                        // and apply it on all the methods not annotated itself
                        itemBuilders.remove(classItemBuilder);
                        var annotatedClass = classItemBuilder.instance.target().asClass();
                        annotatedClass.methods().stream().filter(m -> !m.hasAnnotation(ACCESS_TOKEN))
                                .map(mi -> new ItemBuilder(classItemBuilder.instance, mi)).forEach(itemBuilders::add);
                    }
                }
            });
        }
        return itemBuilders.stream().map(ItemBuilder::build).toList();
    }

}
