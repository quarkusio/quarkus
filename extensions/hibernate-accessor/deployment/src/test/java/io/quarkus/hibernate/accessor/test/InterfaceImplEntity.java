package io.quarkus.hibernate.accessor.test;

public class InterfaceImplEntity implements AccessorInterface {

    private String label;

    public InterfaceImplEntity() {
    }

    public InterfaceImplEntity(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
