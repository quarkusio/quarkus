package com.demo.application;

import com.demo.common.one.model.SharedModelOne;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class ApplicationTest {

    @Test
    void test() {
        SharedModelOne one = new SharedModelOne();
    }
}