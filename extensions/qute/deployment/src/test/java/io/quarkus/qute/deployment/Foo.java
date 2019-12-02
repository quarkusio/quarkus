package io.quarkus.qute.deployment;

class Foo {

    public String name;

    public Long age;

    public Charlie charlie;

    public Foo(String name, Long age) {
        this.name = name;
        this.age = age;
        this.charlie = new Charlie(name.toUpperCase());
    }

    public static class Charlie {

        private String name;

        public Charlie(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

    }
}