package io.quarkus.oidc.token.propagation.common.deployment;

import java.util.List;
import java.util.stream.Stream;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.oidc.token.propagation.common.AccessToken;

public class OidcTokenPropagationCommonProcessor {

    private static final DotName DEPRECATED_ACCESS_TOKEN = DotName
            .createSimple(io.quarkus.oidc.token.propagation.AccessToken.class.getName());
    private static final DotName ACCESS_TOKEN = DotName.createSimple(AccessToken.class.getName());

    @BuildStep
    public List<AccessTokenInstanceBuildItem> collectAccessTokenInstances(CombinedIndexBuildItem index) {
        record ItemBuilder(AnnotationInstance instance) {

            private String toClientName() {
                var value = instance.value("exchangeTokenClient");
                return value == null || value.asString().equals("Default") ? "" : value.asString();
            }

            private boolean toExchangeToken() {
                return instance.value("exchangeTokenClient") != null;
            }

            private AccessTokenInstanceBuildItem build() {
                return new AccessTokenInstanceBuildItem(toClientName(), toExchangeToken(), instance.target());
            }
        }
        var accessTokenAnnotations = index.getIndex().getAnnotations(ACCESS_TOKEN);
        var accessTokenDeprecatedAnnotations = index.getIndex().getAnnotations(DEPRECATED_ACCESS_TOKEN);
        return Stream.concat(accessTokenAnnotations.stream(), accessTokenDeprecatedAnnotations.stream())
                .map(ItemBuilder::new).map(ItemBuilder::build).toList();
    }

}
