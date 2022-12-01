package io.quarkus.resteasy.server.common.runtime;

import java.util.Map;

import org.jboss.resteasy.core.ResteasyDeploymentImpl;

/**
 * The only reason this class exists is to make {@code properties} available to bytecode recording
 */
public class QuarkusResteasyDeployment extends ResteasyDeploymentImpl {

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }
}
