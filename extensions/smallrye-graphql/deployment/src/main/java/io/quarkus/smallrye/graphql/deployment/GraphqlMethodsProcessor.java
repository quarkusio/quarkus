package io.quarkus.smallrye.graphql.deployment;

import java.util.function.Predicate;

import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.Query;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.execannotations.ExecutionModelAnnotationsAllowedBuildItem;
import io.smallrye.graphql.api.Subscription;
import io.smallrye.graphql.api.federation.Resolver;

public class GraphqlMethodsProcessor {
    private static final DotName QUERY = DotName.createSimple(Query.class);
    private static final DotName MUTATION = DotName.createSimple(Mutation.class);
    private static final DotName NAME = DotName.createSimple(Name.class);
    private static final DotName SUBSCRIPTION = DotName.createSimple(Subscription.class);
    private static final DotName RESOLVER = DotName.createSimple(Resolver.class);

    @BuildStep
    ExecutionModelAnnotationsAllowedBuildItem graphqlMethods() {
        return new ExecutionModelAnnotationsAllowedBuildItem(new Predicate<MethodInfo>() {
            @Override
            public boolean test(MethodInfo method) {
                // maybe just look for `@GraphQLApi` on the declaring class?
                return method.hasDeclaredAnnotation(QUERY)
                        || method.hasDeclaredAnnotation(MUTATION)
                        || method.hasDeclaredAnnotation(NAME)
                        || method.hasDeclaredAnnotation(SUBSCRIPTION)
                        || method.hasDeclaredAnnotation(RESOLVER);
            }
        });
    }
}
