package io.quarkus.qute.deployment.devconsole;

import java.util.List;
import java.util.Map;

public class DevQuteTemplateInfo implements Comparable<DevQuteTemplateInfo> {

    private String path;
    private List<String> variants;
    private Map<String, String> parameters;

    public DevQuteTemplateInfo() {
    }

    public DevQuteTemplateInfo(String path, List<String> variants, Map<String, String> parameters) {
        this.path = path;
        this.variants = variants;
        this.parameters = parameters;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<String> getVariants() {
        return variants;
    }

    public void setVariants(List<String> variants) {
        this.variants = variants;
    }

    @Override
    public int compareTo(DevQuteTemplateInfo o) {
        return path.compareTo(o.path);
    }
}
