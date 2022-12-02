package io.quarkus.it.mockbean;

import java.util.List;

public class Foo {
    private final String name;
    private final int count;
    private final Bar bar;

    Foo(String name, int count, Bar bar) {
        this.name = name;
        this.count = count;
        this.bar = bar;
    }

    public String getName() {
        return name;
    }

    public int getCount() {
        return count;
    }

    public Bar getBar() {
        return bar;
    }

    public static class Bar {
        private final List<String> names;

        public Bar(List<String> names) {
            this.names = names;
        }

        public List<String> getNames() {
            return names;
        }
    }

    public static class FooBuilder {
        private String name;
        private int count;
        private Bar bar;

        public FooBuilder(String name) {
            this.name = name;
        }

        public FooBuilder name(String name) {
            this.name = name;
            return this;
        }

        public FooBuilder count(int count) {
            this.count = count;
            return this;
        }

        public FooBuilder bar(Bar bar) {
            this.bar = bar;
            return this;
        }

        public Foo build() {
            return new Foo(this.name, this.count, this.bar);
        }
    }
}
