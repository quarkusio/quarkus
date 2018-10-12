package org.jboss.shamrock.example.rest;

import java.util.HashSet;
import java.util.Set;

public class ComponentType {

    private String value;
    private SubComponent subComponent;
    private Set<CollectionType> collectionTypes = new HashSet<>();

    public SubComponent getSubComponent() {
        return subComponent;
    }

    public void setSubComponent(SubComponent subComponent) {
        this.subComponent = subComponent;
    }

    public Set<CollectionType> getCollectionTypes() {
        return collectionTypes;
    }

    public void setCollectionTypes(Set<CollectionType> collectionTypes) {
        this.collectionTypes = collectionTypes;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
