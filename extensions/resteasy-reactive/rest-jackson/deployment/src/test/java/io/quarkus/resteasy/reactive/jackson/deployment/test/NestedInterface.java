package io.quarkus.resteasy.reactive.jackson.deployment.test;

public interface NestedInterface {

    NestedInterface INSTANCE = new NestedInterface() {
        @Override
        public String getString() {
            return "response";
        }

        @Override
        public Integer getInt() {
            return 42;
        }

        @Override
        public char getCharacter() {
            return 'a';
        }
    };

    String getString();

    Integer getInt();

    char getCharacter();

}
