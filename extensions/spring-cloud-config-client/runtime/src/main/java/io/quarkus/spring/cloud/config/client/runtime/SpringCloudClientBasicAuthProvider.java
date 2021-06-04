package io.quarkus.spring.cloud.config.client.runtime;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.arc.DefaultBean;
import io.vertx.mutiny.ext.web.client.HttpRequest;

@DefaultBean
@ApplicationScoped
public class SpringCloudClientBasicAuthProvider implements SpringCloudClientCredentialsProvider {

    @Override
    public void addAuthenticationInfo(HttpRequest<?> request, SpringCloudConfigClientConfig springCloudConfigClientConfig) {
        if (springCloudConfigClientConfig.usernameAndPasswordSet()) {
            request.basicAuthentication(springCloudConfigClientConfig.username.get(),
                    springCloudConfigClientConfig.password.get());
        }
    }
}
