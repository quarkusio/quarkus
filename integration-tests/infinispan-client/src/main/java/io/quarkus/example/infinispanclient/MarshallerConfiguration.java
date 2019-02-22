package io.quarkus.example.infinispanclient;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.MessageMarshaller;

/**
 * Handles configuration of marshalling code for marshalling
 * 
 * @author William Burns
 */
@ApplicationScoped
public class MarshallerConfiguration {
    @Produces
    MessageMarshaller bookMarshaller() {
        return new BookMarshaller();
    }

    @Produces
    MessageMarshaller authorMarshaller() {
        return new AuthorMarshaller();
    }

    @Produces
    FileDescriptorSource bookProtoDefinition() {
        return FileDescriptorSource.fromString("library.proto", "package book_sample;\n" +
                "\n" +
                "message Book {\n" +
                "  required string title = 1;\n" +
                "  required string description = 2;\n" +
                "  required int32 publicationYear = 3; // no native Date type available in Protobuf\n" +
                "\n" +
                "  repeated Author authors = 4;\n" +
                "}\n" +
                "\n" +
                "message Author {\n" +
                "  required string name = 1;\n" +
                "  required string surname = 2;\n" +
                "}");
    }
}
