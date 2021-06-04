package io.quarkus.spring.cloud.config.client.runtime.credentials;

import io.quarkus.spring.cloud.config.client.runtime.SpringCloudClientCredentialsProvider;
import io.quarkus.spring.cloud.config.client.runtime.SpringCloudConfigClientConfig;
import io.vertx.mutiny.ext.web.client.HttpRequest;

public class SpringCloudClientBasicAuthCredentialsProvider implements SpringCloudClientCredentialsProvider {

    SpringCloudConfigClientConfig springCloudConfigClientConfig;

    public SpringCloudClientBasicAuthCredentialsProvider(SpringCloudConfigClientConfig springCloudConfigClientConfig) {
        this.springCloudConfigClientConfig = springCloudConfigClientConfig;
    }

    @Override
    public void addAuthenticationInfo(HttpRequest<?> request) {
        if (springCloudConfigClientConfig.usernameAndPasswordSet()) {
            request.basicAuthentication(springCloudConfigClientConfig.username.get(),
                    springCloudConfigClientConfig.password.get());
        }
    }
}
