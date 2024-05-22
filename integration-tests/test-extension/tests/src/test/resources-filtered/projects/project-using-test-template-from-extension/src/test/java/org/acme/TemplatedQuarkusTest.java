package org.acme;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class TemplatedQuarkusTest {

    @TestTemplate
    @ExtendWith(MyContextProvider.class)
    void trivialTestTemplate(ExtensionContext context) {
        Assertions.assertTrue(context != null);
    }

}
