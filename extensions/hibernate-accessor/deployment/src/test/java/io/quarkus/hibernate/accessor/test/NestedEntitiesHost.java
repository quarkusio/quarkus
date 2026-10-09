package io.quarkus.hibernate.accessor.test;

import io.quarkus.hibernate.accessor.runtime.ReflectionFreeAccessor;

public class NestedEntitiesHost {

    public static class InnerA {

        @ReflectionFreeAccessor
        private String name;

        @ReflectionFreeAccessor
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

            @ReflectionFreeAccessor
            private String name;

            @ReflectionFreeAccessor
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

        @ReflectionFreeAccessor
        private String name;

        @ReflectionFreeAccessor
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
