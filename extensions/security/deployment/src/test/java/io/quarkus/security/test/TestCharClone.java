package io.quarkus.security.test;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

public class TestCharClone {
    @Test
    public void testClone() {
        char[] password = "jb0ss".toCharArray();
        char[] clone = password.clone();
        if (clone == password) {
            System.out.printf("Failure, clone == password\n");
        }
        if (!Arrays.equals(password, clone)) {
            System.out.printf("Failure, clone neq password\n");
        }
        Class<? extends char[]> charArrayClass = password.getClass();
        System.out.printf("char[](%s) methods:\n", charArrayClass.getName());
        for (Method m : charArrayClass.getMethods()) {
            System.out.println(m);
        }
    }
}
