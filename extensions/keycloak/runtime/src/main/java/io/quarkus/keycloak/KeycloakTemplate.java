package io.quarkus.keycloak;

import java.io.InputStream;

import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.undertow.KeycloakServletExtension;
import org.keycloak.representations.adapters.config.AdapterConfig;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.arc.runtime.BeanContainerListener;
import io.quarkus.runtime.annotations.Template;
import io.undertow.servlet.ServletExtension;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@Template
public class KeycloakTemplate {

    public ServletExtension createServletExtension(AdapterDeploymentContext deploymentContext) {
        return new KeycloakServletExtension(deploymentContext);
    }

    public QuarkusDeploymentContext createKeycloakDeploymentContext(AdapterConfig defaultConfig) {
        KeycloakDeployment deployment;

        if (defaultConfig == null) {
            InputStream config = loadConfig(Thread.currentThread().getContextClassLoader());

            if (config == null) {
                config = loadConfig(getClass().getClassLoader());
            }

            deployment = KeycloakDeploymentBuilder.build(config);
        } else {
            deployment = KeycloakDeploymentBuilder.build(defaultConfig);
        }

        return new QuarkusDeploymentContext(deployment);
    }

    public BeanContainerListener createBeanContainerListener(QuarkusDeploymentContext deploymentContext) {
        return new BeanContainerListener() {
            @Override
            public void created(BeanContainer container) {
                deploymentContext.setConfigResolver(container.instance(QuarkusKeycloakConfigResolver.class));
            }
        };
    }

    private InputStream loadConfig(ClassLoader classLoader) {
        return classLoader.getResourceAsStream("keycloak.json");
    }
}
