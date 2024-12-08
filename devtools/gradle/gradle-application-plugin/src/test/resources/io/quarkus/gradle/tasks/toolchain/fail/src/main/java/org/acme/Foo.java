package org.acme;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "foo")
public interface Foo {
    String string();
}