package io.quarkus.it.infinispan.client;

import org.infinispan.protostream.annotations.ProtoEnumValue;

public enum Type {
    @ProtoEnumValue(number = 1)
    FANTASY,
    @ProtoEnumValue(number = 2)
    PROGRAMMING
}
