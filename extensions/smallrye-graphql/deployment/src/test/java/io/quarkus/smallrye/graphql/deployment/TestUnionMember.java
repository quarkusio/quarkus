package io.quarkus.smallrye.graphql.deployment;

/**
 * A test POJO that is a member of a union
 */
public class TestUnionMember implements TestUnion {

    private String name;

    public TestUnionMember() {
    }

    public TestUnionMember(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
