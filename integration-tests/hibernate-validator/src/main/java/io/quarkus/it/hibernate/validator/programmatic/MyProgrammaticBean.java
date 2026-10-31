package io.quarkus.it.hibernate.validator.programmatic;

import io.quarkus.hibernate.accessor.runtime.ReflectionFreeFieldAccessor;

public class MyProgrammaticBean {
    // Programmatic constraints are contributed at runtime, so register their readers explicitly.
    @ReflectionFreeFieldAccessor
    String string;

    @ReflectionFreeFieldAccessor
    NestedBean nestedBean;

    public MyProgrammaticBean(String string, NestedBean nestedBean) {
        this.string = string;
        this.nestedBean = nestedBean;
    }

    public static class NestedBean {
        @ReflectionFreeFieldAccessor
        public String string;

        public NestedBean(String string) {
            this.string = string;
        }
    }
}
