package io.quarkus.funqy.test;

import java.util.List;

public class RecursiveType {
    private int value;
    private RecursiveType parent;
    private List<RecursiveType> children;

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public RecursiveType getParent() {
        return parent;
    }

    public void setParent(RecursiveType parent) {
        this.parent = parent;
    }

    public List<RecursiveType> getChildren() {
        return children;
    }

    public void setChildren(List<RecursiveType> children) {
        this.children = children;
    }

}
