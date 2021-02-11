package io.quarkus.arc.deployment.devconsole;

import java.util.ArrayList;
import java.util.List;

public class DevBeanInfo implements Comparable<DevBeanInfo> {

    private ClassName name;
    private String methodName;
    private DevBeanKind kind;
    private ClassName type;
    private List<ClassName> qualifiers = new ArrayList<>();
    private ClassName scope;

    public DevBeanInfo() {
    }

    public DevBeanInfo(ClassName name, String methodName, ClassName type, List<ClassName> qualifiers, ClassName scope,
            DevBeanKind kind) {
        this.name = name;
        this.methodName = methodName;
        this.type = type;
        this.qualifiers = qualifiers;
        this.scope = scope;
        this.kind = kind;
    }

    public void setKind(DevBeanKind kind) {
        this.kind = kind;
    }

    public DevBeanKind getKind() {
        return kind;
    }

    public void setScope(ClassName scope) {
        this.scope = scope;
    }

    public ClassName getScope() {
        return scope;
    }

    public List<ClassName> getQualifiers() {
        return qualifiers;
    }

    public void setQualifiers(List<ClassName> qualifiers) {
        this.qualifiers = qualifiers;
    }

    public void setType(ClassName type) {
        this.type = type;
    }

    public ClassName getType() {
        return type;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public void setName(ClassName name) {
        this.name = name;
    }

    public ClassName getName() {
        return name;
    }

    @Override
    public int compareTo(DevBeanInfo o) {
        return type.getLocalName().compareTo(o.type.getLocalName());
    }
}
