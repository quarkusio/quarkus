package io.quarkus.hibernate.accessor.test;

import io.quarkus.hibernate.accessor.FieldAccess;
import io.quarkus.hibernate.accessor.GeneratedFieldAccessor;

public class HostOneEntity {

    public static class Nested {

        @GeneratedFieldAccessor(access = FieldAccess.READ_WRITE)
        private String value;

        @GeneratedFieldAccessor(access = FieldAccess.READ_WRITE)
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
