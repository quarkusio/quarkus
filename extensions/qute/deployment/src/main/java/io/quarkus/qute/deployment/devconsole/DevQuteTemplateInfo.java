package io.quarkus.qute.deployment.devconsole;

import java.util.Map;

public class DevQuteTemplateInfo implements Comparable<DevQuteTemplateInfo> {

    private final String path;
    // variant -> source
    private final Map<String, String> variants;
    private final String methodInfo;
    private final Map<String, String> parameters;

    public DevQuteTemplateInfo(String path, Map<String, String> variants, String methodInfo,
            Map<String, String> parameters) {
        this.path = path;
        this.variants = variants;
        this.methodInfo = methodInfo;
        this.parameters = parameters;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public String getPath() {
        return path;
    }

    public Map<String, String> getVariants() {
        return variants;
    }

    public String getMethodInfo() {
        return methodInfo;
    }

    @Override
    public int compareTo(DevQuteTemplateInfo o) {
        return path.compareTo(o.path);
    }

}
