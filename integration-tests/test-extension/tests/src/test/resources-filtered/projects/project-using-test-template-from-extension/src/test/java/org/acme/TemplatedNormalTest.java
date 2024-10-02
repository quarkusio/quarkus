package org.acme;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;

// No QuarkusTest annotation

/**
 * It's likely we would never expect Quarkus bytecode changes to be visible in this kind of test; unit tests which don't have
 * a @QuarkusTest
 * annotation would not be able to
 * benefit from bytecode manipulations from extensions.
 */
public class TemplatedNormalTest {

    @TestTemplate
    @ExtendWith(MyContextProvider.class)
    void trivialTestTemplate(ExtensionContext context) {
        Assertions.assertTrue(context != null);

    }
}
