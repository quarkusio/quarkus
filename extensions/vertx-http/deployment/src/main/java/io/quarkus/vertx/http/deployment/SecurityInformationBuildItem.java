package io.quarkus.vertx.http.deployment;

import java.util.Optional;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Contains information on the security model used in the application
 */
public final class SecurityInformationBuildItem extends MultiBuildItem {

    private final SecurityModel securityModel;
    private final Optional<OpenIDConnectInformation> openIDConnectInformation;

    public static SecurityInformationBuildItem BASIC() {
        return new SecurityInformationBuildItem(SecurityModel.basic, Optional.empty());
    }

    public static SecurityInformationBuildItem JWT() {
        return new SecurityInformationBuildItem(SecurityModel.jwt, Optional.empty());
    }

    public static SecurityInformationBuildItem OPENIDCONNECT(String urlConfigKey) {
        return new SecurityInformationBuildItem(SecurityModel.oidc,
                Optional.of(new OpenIDConnectInformation(urlConfigKey)));
    }

    public SecurityInformationBuildItem(SecurityModel securityModel,
            Optional<OpenIDConnectInformation> openIDConnectInformation) {
        this.securityModel = securityModel;
        this.openIDConnectInformation = openIDConnectInformation;
    }

    public SecurityModel getSecurityModel() {
        return securityModel;
    }

    public Optional<OpenIDConnectInformation> getOpenIDConnectInformation() {
        return openIDConnectInformation;
    }

    public enum SecurityModel {
        basic,
        jwt,
        oidc
    }

    public static class OpenIDConnectInformation {
        private final String urlConfigKey;

        public OpenIDConnectInformation(String urlConfigKey) {
            this.urlConfigKey = urlConfigKey;
        }

        public String getUrlConfigKey() {
            return urlConfigKey;
        }
    }
}