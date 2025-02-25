package io.quarkus.security.test.permissionsallowed;

public final class ComplexFieldParam {

    public final NestedFieldParam nestedFieldParam;

    ComplexFieldParam(NestedFieldParam nestedFieldParam) {
        this.nestedFieldParam = nestedFieldParam;
    }

    public static final class NestedFieldParam {

        public final SimpleFieldParam simpleFieldParam;

        public NestedFieldParam(SimpleFieldParam simpleFieldParam) {
            this.simpleFieldParam = simpleFieldParam;
        }
    }

}
