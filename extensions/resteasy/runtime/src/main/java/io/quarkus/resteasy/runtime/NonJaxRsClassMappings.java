package io.quarkus.resteasy.runtime;

import java.util.Map;

public class NonJaxRsClassMappings {

    private String basePath;
    private Map<String, String> methodNameToPath;

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public Map<String, String> getMethodNameToPath() {
        return methodNameToPath;
    }

    public void setMethodNameToPath(Map<String, String> methodNameToPath) {
        this.methodNameToPath = methodNameToPath;
    }
}
