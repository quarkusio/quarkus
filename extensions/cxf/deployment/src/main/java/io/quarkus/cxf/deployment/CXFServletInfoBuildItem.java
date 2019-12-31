package io.quarkus.cxf.deployment;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.builder.item.MultiBuildItem;

public final class CXFServletInfoBuildItem extends MultiBuildItem {

    private String path;
    private String className;
    private List<String> inInterceptors;
    private List<String> outInterceptors;
    private List<String> outFaultInterceptors;
    private List<String> inFaultInterceptors;
    private List<String> features;

    public CXFServletInfoBuildItem(String path, String className) {
        this.path = path;
        this.className = className;
        this.inInterceptors = new ArrayList<>();
        this.outInterceptors = new ArrayList<>();
        this.outFaultInterceptors = new ArrayList<>();
        this.inFaultInterceptors = new ArrayList<>();
        this.features = new ArrayList<>();
    }

    public String getClassName() {
        return className;
    }

    public String getPath() {
        return path;
    }

    public List<String> getFeatures() {
        return features;
    }

    public List<String> getInInterceptors() {
        return inInterceptors;
    }

    public List<String> getOutInterceptors() {
        return outInterceptors;
    }

    public List<String> getOutFaultInterceptors() {
        return outFaultInterceptors;
    }

    public List<String> getInFaultInterceptors() {
        return inFaultInterceptors;
    }
}
