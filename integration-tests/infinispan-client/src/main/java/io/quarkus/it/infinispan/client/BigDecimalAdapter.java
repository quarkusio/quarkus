package io.quarkus.it.infinispan.client;

import java.math.BigDecimal;

import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.descriptors.Type;

@ProtoAdapter(BigDecimal.class)
public class BigDecimalAdapter {
    @ProtoFactory
    BigDecimal create(Double value) {
        return BigDecimal.valueOf(value);
    }

    @ProtoField(number = 1, type = Type.DOUBLE, defaultValue = "0")
    Double value(BigDecimal value) {
        return value.doubleValue();
    }
}
