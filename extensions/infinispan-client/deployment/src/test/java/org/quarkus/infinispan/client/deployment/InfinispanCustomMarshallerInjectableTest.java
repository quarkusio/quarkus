package org.quarkus.infinispan.client.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.commons.marshall.ProtoStreamMarshaller;
import org.infinispan.protostream.BaseMarshaller;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.MessageMarshaller;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.common.annotation.Identifier;

public class InfinispanCustomMarshallerInjectableTest {
    @Inject
    RemoteCacheManager remoteCacheManager;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withEmptyApplication()
            .withConfigurationResource("cache-config-application.properties")
            .withApplicationRoot((jar) -> jar
                    .addClasses(CustomMarshallerProducer.class, MetadataMarshaller.class, Metadata.class)
                    .addAsResource("distributed-cache-config.xml")
                    .addAsResource("local-cache-config.xml"));

    @Test
    void infinispanClientConfigurationWithMetadataMarshaller() throws Exception {
        assertThat(remoteCacheManager).isNotNull();
        ProtoStreamMarshaller protoMarshaller = (ProtoStreamMarshaller) remoteCacheManager.getConfiguration().marshaller();
        BaseMarshaller<Object> customMetadata = protoMarshaller.getSerializationContext()
                .getMarshaller("custom_metadata");
        assertThat(customMetadata).isNotNull();
        assertThat(customMetadata).isInstanceOf(MetadataMarshaller.class);
    }

    @ApplicationScoped
    static class CustomMarshallerProducer {

        @Produces
        @Identifier("metadata-marshaller")
        public MessageMarshaller createMarshaller() {
            return new MetadataMarshaller("custom_metadata");
        }

        @Produces
        public FileDescriptorSource customProtoDef() {
            return FileDescriptorSource.fromString("custom-metadata.proto", PROTO);
        }

        private static final String PROTO = "syntax = \"proto2\";\n" + "\n"
                + "/**\n" + " * @Indexed\n" + " */\n"
                + "message custom_metadata {\n"
                + "   \n"
                + "   /**\n" + "    * @Basic(projectable=true)\n" + "    */\n"
                + "   optional string name = 1;\n"
                + "   \n"
                + "   /**\n" + "    * @Basic(projectable=true)\n" + "    */\n"
                + "   optional string value = 2;\n"
                + "   \n"
                + "   /**\n" + "    * @Basic(projectable=true)\n" + "    */\n"
                + "   optional int64 value_int = 3;\n"
                + "   \n"
                + "   /**\n" + "    * @Basic(projectable=true)\n" + "    */\n"
                + "   optional double value_float = 4;\n"
                + "}\n";
    }

    public static class MetadataMarshaller implements MessageMarshaller<Metadata> {

        private final String typeName;

        public MetadataMarshaller(String typeName) {
            this.typeName = typeName;
        }

        @Override
        public Metadata readFrom(ProtoStreamReader reader) throws IOException {
            String name = reader.readString("name");
            String valueStr = reader.readString("value");
            Long valueInt = reader.readLong("value_int");
            Double valueFloat = reader.readDouble("value_float");
            Object value = valueStr;

            if (value == null) {
                value = valueInt;
            }
            if (value == null) {
                value = valueFloat;
            }

            return new Metadata(name, value);
        }

        @Override
        public void writeTo(ProtoStreamWriter writer, Metadata item) throws IOException {
            writer.writeString("name", item.getName());
            String value = null;
            Long valueInt = null;
            Double valueFloat = null;
            if (item.getValue() instanceof String) {
                value = (String) item.getValue();
            } else if (item.getValue() instanceof Integer) {
                valueInt = ((Integer) item.getValue()).longValue();
            } else if (item.getValue() instanceof Long) {
                valueInt = (Long) item.getValue();
            } else if (item.getValue() instanceof Float) {
                valueFloat = ((Float) item.getValue()).doubleValue();
            } else if (item.getValue() instanceof Double) {
                valueFloat = (Double) item.getValue();
            } else if (item.getValue() != null) {
                value = item.getValue().toString();
            }

            writer.writeString("value", value);
            writer.writeLong("value_int", valueInt);
            writer.writeDouble("value_float", valueFloat);
        }

        @Override
        public Class<? extends Metadata> getJavaClass() {
            return Metadata.class;
        }

        @Override
        public String getTypeName() {
            return typeName;
        }
    }

    public static class Metadata {

        private String name;
        private Object value;

        public Metadata(String name, Object value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public Object getValue() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            Metadata that = (Metadata) o;
            return Objects.equals(name, that.name) && Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, value);
        }

        @Override
        public String toString() {
            return "Metadata{name='" + name + "', value=" + value + '}';
        }
    }
}
