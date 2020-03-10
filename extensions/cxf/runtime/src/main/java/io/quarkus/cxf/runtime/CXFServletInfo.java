package io.quarkus.cxf.runtime;

import java.util.ArrayList;
import java.util.List;

public class CXFServletInfo {
    private String path;
    private String className;
    private List<String> inInterceptors;
    private List<String> outInterceptors;
    private List<String> outFaultInterceptors;
    private List<String> inFaultInterceptors;
    private List<String> features;
    private String sei;
    private String wsdlPath;

    public CXFServletInfo(String path, String className, String sei, String wsdlPath) {
        super();
        this.path = path;
        this.className = className;
        this.inInterceptors = new ArrayList<>();
        this.outInterceptors = new ArrayList<>();
        this.outFaultInterceptors = new ArrayList<>();
        this.inFaultInterceptors = new ArrayList<>();
        this.features = new ArrayList<>();
        this.sei = sei;
        this.wsdlPath = wsdlPath;
    }

    public String getClassName() {
        return className;
    }

    public String getPath() {
        return path;
    }

    public String getWsdlPath() {
        return wsdlPath;
    }

    public String getSei() {
        return sei;
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

    @Override
    public String toString() {
        return "Web Service " + className + " on " + path;
    }
}
