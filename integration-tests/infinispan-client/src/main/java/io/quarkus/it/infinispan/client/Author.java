package io.quarkus.it.infinispan.client;

import org.infinispan.protostream.annotations.Proto;

@Proto
public record Author(String name, String surname) {
}
