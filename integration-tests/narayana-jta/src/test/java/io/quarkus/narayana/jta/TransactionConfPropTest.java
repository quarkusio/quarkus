package io.quarkus.narayana.jta;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.arjuna.ats.arjuna.common.arjPropertyManager;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class TransactionConfPropTest {

    /*
     * verify that the objectStore directory path for JTA can be configured
     */
    @Test
    void testObjectStoreDirPath() {
        // verify that the quarkus configuration took effect
        Assertions.assertEquals("target/tx-object-store", // this value is set via application.properties
                arjPropertyManager.getObjectStoreEnvironmentBean().getObjectStoreDir());
    }
}
