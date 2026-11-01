package io.quarkus.hibernate.accessor.test;

import io.quarkus.hibernate.accessor.runtime.FieldAccess;
import io.quarkus.hibernate.accessor.runtime.ReflectionFreeFieldAccessor;

public class HostOneEntity {

    public static class Nested {

        @ReflectionFreeFieldAccessor(access = FieldAccess.READ_WRITE)
        private String value;

        @ReflectionFreeFieldAccessor(access = FieldAccess.READ_WRITE)
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
