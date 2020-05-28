package io.quarkus.rest.data.panache.deployment;

public final class PrivateMethods {

    public static final Method IS_PAGED = new Method("isPaged", boolean.class.getTypeName(), new String[0]);

    public static final class Method {
        private final String name;

        private final String type;

        private final String[] params;

        public Method(String name, String type, String[] params) {
            this.name = name;
            this.type = type;
            this.params = params;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        public String[] getParams() {
            return params;
        }
    }
}
