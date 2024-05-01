package io.quarkus.funqy.lambda.event;

import java.util.Iterator;

import com.amazonaws.services.lambda.runtime.events.models.kinesis.Record;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.deser.BeanDeserializerBuilder;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.module.SimpleModule;

import io.quarkus.funqy.lambda.event.kinesis.DateDeserializer;

public class AwsModule extends SimpleModule {

    final String DATE_PROPERTY_NAME = "approximateArrivalTimestamp";

    public AwsModule() {
        this.setDeserializerModifier(new BeanDeserializerModifier() {

            @Override
            public BeanDeserializerBuilder updateBuilder(final DeserializationConfig config,
                    final BeanDescription beanDesc, final BeanDeserializerBuilder builder) {

                for (final Iterator<SettableBeanProperty> iterator = builder.getProperties(); iterator.hasNext();) {
                    SettableBeanProperty property = iterator.next();

                    // Kinesis records need some special treatment. The approximateArrivalTimestamp
                    // cannot be deserialized that easily.
                    if (Record.class.isAssignableFrom(property.getMember().getDeclaringClass())
                            && DATE_PROPERTY_NAME.equalsIgnoreCase(property.getName())) {
                        final DateDeserializer deserializer = new DateDeserializer();
                        property = property.withValueDeserializer(deserializer);
                        builder.addOrReplaceProperty(property, true);
                    }
                }
                return builder;
            }
        });
    }
}
