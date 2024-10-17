package io.quarkus.narayana.jta.runtime;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.junit.jupiter.api.Test;

public class NarayanaJtaRecorderTest {

    @Test
    void testByteLength() {
        TransactionManagerConfiguration transactions = new TransactionManagerConfiguration();
        transactions.shortenNodeNameIfNecessary = true;
        // create nodeNames larger than 28 bytes
        for (int i = 1; i < 100; i++) {
            String originalNodeName = UUID.randomUUID().toString();
            NarayanaJtaRecorder r = new NarayanaJtaRecorder();
            transactions.nodeName = originalNodeName;
            r.setNodeName(transactions);
            int numberOfBytes = transactions.nodeName.getBytes(StandardCharsets.UTF_8).length;
            assertFalse(numberOfBytes > 28,
                    "node name bytes still exceeded 28 bytes limit, number of bytes is " + numberOfBytes);
        }
        for (int i = 1; i < 1000; i++) {
            byte[] data = new byte[i];
            NarayanaJtaRecorder r = new NarayanaJtaRecorder();
            transactions.nodeName = new String(data);
            r.setNodeName(transactions);
            int numberOfBytes = transactions.nodeName.getBytes(StandardCharsets.UTF_8).length;
            assertFalse(numberOfBytes > 28,
                    "node name bytes still exceeded 28 bytes limit, number of bytes is " + numberOfBytes);
        }
    }

}
