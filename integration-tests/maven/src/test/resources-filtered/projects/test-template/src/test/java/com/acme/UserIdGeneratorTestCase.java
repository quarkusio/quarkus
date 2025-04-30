package com.acme;

public class UserIdGeneratorTestCase {
    private static int GLOBAL_ID = 0;
    private final int id = GLOBAL_ID++;

    public Object getExpectedBody() {
        return "hello";
    }

    public String getDisplayName() {
        return "simple test template test case" + id;
    }
}
