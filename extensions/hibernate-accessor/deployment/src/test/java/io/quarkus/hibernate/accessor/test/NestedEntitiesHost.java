package io.quarkus.hibernate.accessor.test;

import io.quarkus.hibernate.accessor.runtime.FieldAccess;
import io.quarkus.hibernate.accessor.runtime.ReflectionFreeFieldAccessor;

public class NestedEntitiesHost {

    public static class InnerA {

        @ReflectionFreeFieldAccessor(access = FieldAccess.READ_WRITE)
        private String name;

        @ReflectionFreeFieldAccessor(access = FieldAccess.READ_WRITE)
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

            @ReflectionFreeFieldAccessor(access = FieldAccess.READ_WRITE)
            private String name;

            @ReflectionFreeFieldAccessor(access = FieldAccess.READ_WRITE)
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

        @ReflectionFreeFieldAccessor(access = FieldAccess.READ_WRITE)
        private String name;

        @ReflectionFreeFieldAccessor(access = FieldAccess.READ_WRITE)
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
