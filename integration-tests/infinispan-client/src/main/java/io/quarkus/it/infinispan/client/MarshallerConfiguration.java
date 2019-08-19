package io.quarkus.it.infinispan.client;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.MessageMarshaller;

@ApplicationScoped
public class MarshallerConfiguration {
    @Produces
    MessageMarshaller magazineMarshaller() {
        return new MagazineMarshaller();
    }

    @Produces
    FileDescriptorSource bookProtoDefinition() {
        return FileDescriptorSource.fromString("magazine.proto", "package magazine_sample;\n" +
                "\n" +
                "message Magazine {\n" +
                "  required string name = 1;\n" +
                "  required int32 publicationYear = 2;\n" +
                "  required int32 publicationMonth = 3;\n" +
                "  repeated string stories = 4;\n" +
                "}");
    }
}
