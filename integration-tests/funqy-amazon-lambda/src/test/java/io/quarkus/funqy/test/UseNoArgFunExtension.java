package io.quarkus.funqy.test;

import org.junit.jupiter.api.extension.Extension;

public class UseNoArgFunExtension implements Extension {
    static {
        System.setProperty("quarkus.funqy.export", "noArgFun");
    }
}
