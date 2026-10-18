package io.quarkus.it.infinispan.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import org.infinispan.protostream.MessageMarshaller;
import org.infinispan.protostream.schema.Schema;
import org.infinispan.protostream.schema.Type;

@ApplicationScoped
public class MarshallerConfiguration {
    @Produces
    MessageMarshaller magazineMarshaller() {
        return new MagazineMarshaller();
    }

    @Produces
    Schema magazineSchema() {
        return new Schema.Builder("magazine.proto")
                .packageName("magazine_sample")
                .addMessage("Magazine")
                .addField(Type.Scalar.STRING, "name", 1)
                .addField(Type.Scalar.INT32, "publicationYear", 2)
                .addField(Type.Scalar.INT32, "publicationMonth", 3)
                .addRepeatedField(Type.Scalar.STRING, "stories", 4)
                .build();
    }
}
