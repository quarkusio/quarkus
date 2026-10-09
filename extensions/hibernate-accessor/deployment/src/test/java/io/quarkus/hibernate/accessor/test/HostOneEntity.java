package io.quarkus.hibernate.accessor.test;

import io.quarkus.hibernate.accessor.runtime.ReflectionFreeAccessor;

public class HostOneEntity {

    public static class Nested {

        @ReflectionFreeAccessor
        private String value;

        @ReflectionFreeAccessor
        private int number;

        public Nested() {
        }

        public Nested(String value, int number) {
            this.value = value;
            this.number = number;
        }

        public String getValue() {
            return value;
        }

        public int getNumber() {
            return number;
        }
    }
}
