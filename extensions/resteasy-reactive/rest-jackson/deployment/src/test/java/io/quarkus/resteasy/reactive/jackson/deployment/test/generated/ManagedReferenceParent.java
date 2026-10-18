package io.quarkus.resteasy.reactive.jackson.deployment.test.generated;

import com.fasterxml.jackson.annotation.JsonManagedReference;

public class ManagedReferenceParent {

    private String parentName;

    @JsonManagedReference
    private ManagedReferenceChild child;

    public String getParentName() {
        return parentName;
    }

    public void setParentName(String parentName) {
        this.parentName = parentName;
    }

    public ManagedReferenceChild getChild() {
        return child;
    }

    public void setChild(ManagedReferenceChild child) {
        this.child = child;
    }
}
