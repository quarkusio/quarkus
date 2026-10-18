package io.quarkus.arc.deployment.devui;

public class Node {

    static Node from(DevBeanInfo beanInfo) {
        return new Node(beanInfo.getId(), beanInfo.getDescription(), beanInfo.getSimpleDescription());
    }

    private final String id;
    private final String description;
    private final String simpleDescription;

    Node(String id, String description, String simpleDescription) {
        this.id = id;
        this.description = description;
        this.simpleDescription = simpleDescription;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public String getSimpleDescription() {
        return simpleDescription;
    }

}
