package org.example;

import org.junit.jupiter.api.BeforeEach;


public class TestBase {

    private final TestHelper helper;
    private final MainLibrary1 mainLibrary1;

    public TestBase() {
        this.helper = new TestHelper();
        this.mainLibrary1 = new MainLibrary1();
    }

    @BeforeEach
    public void setUp() {
        helper.setUp();
        mainLibrary1.hereJustForDependency();
    }

}