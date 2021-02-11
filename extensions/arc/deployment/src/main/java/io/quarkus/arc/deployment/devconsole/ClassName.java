package io.quarkus.arc.deployment.devconsole;

// FIXME: this should be a template extension method for String
public class ClassName {
    private String name;

    public ClassName() {
    }

    public ClassName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocalName() {
        int lastDot = name.lastIndexOf('.');
        if (lastDot != -1) {
            return name.substring(lastDot + 1);
        }
        return name;
    }
}
