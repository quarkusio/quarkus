package io.quarkus.hibernate.accessor.test;

import io.quarkus.hibernate.accessor.FieldAccess;
import io.quarkus.hibernate.accessor.GeneratedFieldAccessor;

public class NestedEntitiesHost {

    public static class InnerA {

        @GeneratedFieldAccessor(access = FieldAccess.READ_WRITE)
        private String name;

        @GeneratedFieldAccessor(access = FieldAccess.READ_WRITE)
        private int count;

        public InnerA() {
        }

        public InnerA(String name, int count) {
            this.name = name;
            this.count = count;
        }

        public String getName() {
            return name;
        }

        public int getCount() {
            return count;
        }

        public static class DeepInner {

            @GeneratedFieldAccessor(access = FieldAccess.READ_WRITE)
            private String name;

            @GeneratedFieldAccessor(access = FieldAccess.READ_WRITE)
            private long deepValue;

            public DeepInner() {
            }

            public DeepInner(String name, long deepValue) {
                this.name = name;
                this.deepValue = deepValue;
            }

            public String getName() {
                return name;
            }

            public long getDeepValue() {
                return deepValue;
            }
        }
    }

    public static class InnerB {

        @GeneratedFieldAccessor(access = FieldAccess.READ_WRITE)
        private String name;

        @GeneratedFieldAccessor(access = FieldAccess.READ_WRITE)
        private double score;

        public InnerB() {
        }

        public InnerB(String name, double score) {
            this.name = name;
            this.score = score;
        }

        public String getName() {
            return name;
        }

        public double getScore() {
            return score;
        }
    }
}
