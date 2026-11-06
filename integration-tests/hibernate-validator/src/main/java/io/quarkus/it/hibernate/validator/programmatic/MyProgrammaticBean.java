package io.quarkus.it.hibernate.validator.programmatic;

import io.quarkus.hibernate.accessor.GeneratedFieldAccessor;

public class MyProgrammaticBean {
    // Programmatic constraints are contributed at runtime, so register their readers explicitly.
    // This is only necessary because we set `quarkus.hibernate-accessor.strategy` to the strict,
    // no-fallback `generated` strategy.
    // It would not be necessary in a normal application, where we'd fall back to reflection.
    @GeneratedFieldAccessor
    String string;

    @GeneratedFieldAccessor
    NestedBean nestedBean;

    public MyProgrammaticBean(String string, NestedBean nestedBean) {
        this.string = string;
        this.nestedBean = nestedBean;
    }

    public static class NestedBean {
        @GeneratedFieldAccessor
        public String string;

        public NestedBean(String string) {
            this.string = string;
        }
    }
}
