package io.quarkus.resteasy.reactive.jackson.deployment.test.generated;

import com.fasterxml.jackson.annotation.JsonBackReference;

public class ManagedReferenceChild {

    private String childName;

    @JsonBackReference
    private ManagedReferenceParent parent;

    public String getChildName() {
        return childName;
    }

    public void setChildName(String childName) {
        this.childName = childName;
    }

    public ManagedReferenceParent getParent() {
        return parent;
    }

    public void setParent(ManagedReferenceParent parent) {
        this.parent = parent;
    }
}
