package io.quarkus.hibernate.accessor.test;

public class PackagePrivateInterfaceImplEntity implements PackagePrivateAccessorInterface {

    private String label;

    public PackagePrivateInterfaceImplEntity() {
    }

    public PackagePrivateInterfaceImplEntity(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
