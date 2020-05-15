package io.quarkus.it.panache;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class HibernateProxyFieldAccessTest {

    @Test
    public void folderContentNameTest() {
        Folder f = Folder.findById(1L);
        Content content = f.content;
        String name = content.name;
        Assertions.assertEquals("it is working!", name);
    }
}
