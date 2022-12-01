package io.quarkus.funqy.test;

public class Nested {
    private Simple nestedOne;
    private Simple nestedTwo;

    public Simple getNestedOne() {
        return nestedOne;
    }

    public void setNestedOne(Simple nestedOne) {
        this.nestedOne = nestedOne;
    }

    public Simple getNestedTwo() {
        return nestedTwo;
    }

    public void setNestedTwo(Simple nestedTwo) {
        this.nestedTwo = nestedTwo;
    }
}
