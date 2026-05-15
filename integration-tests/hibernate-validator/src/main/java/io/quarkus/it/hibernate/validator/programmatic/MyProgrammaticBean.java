package io.quarkus.it.hibernate.validator.programmatic;

public class MyProgrammaticBean {
    String string;

    NestedBean nestedBean;

    public MyProgrammaticBean(String string, NestedBean nestedBean) {
        this.string = string;
        this.nestedBean = nestedBean;
    }

    public static class NestedBean {
        public String string;

        public NestedBean(String string) {
            this.string = string;
        }
    }
}
