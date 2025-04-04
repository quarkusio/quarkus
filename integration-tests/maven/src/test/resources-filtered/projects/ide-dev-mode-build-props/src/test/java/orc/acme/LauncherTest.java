package org.acme;

import io.quarkus.launcher.QuarkusLauncher;
import org.junit.jupiter.api.Test;

public class LauncherTest {
    @Test
    public void testLauncher() throws Exception {
        try(var app = QuarkusLauncher.launch(LauncherTest.class.getName(), TestApp.class.getName())) {
        }
    }
}