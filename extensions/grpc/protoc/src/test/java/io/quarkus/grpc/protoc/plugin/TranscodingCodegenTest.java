package io.quarkus.grpc.protoc.plugin;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.api.AnnotationsProto;
import com.google.api.HttpRule;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.compiler.PluginProtos;
import com.salesforce.jprotoc.GeneratorException;
import com.salesforce.jprotoc.ProtoTypeMap;

class TranscodingCodegenTest {

    private static final ProtoTypeMap TYPE_MAP = ProtoTypeMap.of(List.of(
            DescriptorProtos.FileDescriptorProto.newBuilder()
                    .setName("test.proto")
                    .setPackage("test")
                    .setOptions(DescriptorProtos.FileOptions.newBuilder()
                            .setJavaPackage("io.quarkus.grpc.test")
                            .setJavaMultipleFiles(true)
                            .build())
                    .addMessageType(DescriptorProtos.DescriptorProto.newBuilder().setName("TestRequest").build())
                    .addMessageType(DescriptorProtos.DescriptorProto.newBuilder().setName("TestResponse").build())
                    .build()));

    private MutinyGrpcGenerator.MethodContext build(DescriptorProtos.MethodOptions options) {
        DescriptorProtos.MethodDescriptorProto.Builder method = DescriptorProtos.MethodDescriptorProto.newBuilder()
                .setName("GetItem")
                .setInputType(".test.TestRequest")
                .setOutputType(".test.TestResponse");
        if (options != null) {
            method.setOptions(options);
        }
        return new MutinyGrpcGenerator().buildMethodContext(method.build(), TYPE_MAP, List.of(), 0);
    }

    @Test
    void transcodingContextIsNullWhenNoAnnotation() {
        assertThat(build(null).transcodingContext).isNull();
    }

    @Test
    void transcodingContextForGet() {
        DescriptorProtos.MethodOptions options = DescriptorProtos.MethodOptions.newBuilder()
                .setExtension(AnnotationsProto.http, HttpRule.newBuilder()
                        .setGet("/v1/items/{item_id}")
                        .build())
                .build();

        MutinyGrpcGenerator.TranscodingContext tc = build(options).transcodingContext;

        assertThat(tc).isNotNull();
        assertThat(tc.method).isEqualTo("GET");
        assertThat(tc.path).isEqualTo("/v1/items/{item_id}");
        assertThat(tc.body).isEmpty();
        assertThat(tc.responseBody).isEmpty();
    }

    @Test
    void transcodingContextForPostWithBody() {
        DescriptorProtos.MethodOptions options = DescriptorProtos.MethodOptions.newBuilder()
                .setExtension(AnnotationsProto.http, HttpRule.newBuilder()
                        .setPost("/v1/items/{item_id}")
                        .setBody("item")
                        .build())
                .build();

        MutinyGrpcGenerator.TranscodingContext tc = build(options).transcodingContext;

        assertThat(tc).isNotNull();
        assertThat(tc.method).isEqualTo("POST");
        assertThat(tc.path).isEqualTo("/v1/items/{item_id}");
        assertThat(tc.body).isEqualTo("item");
    }

    @Test
    void transcodingContextForPutWithStarBody() {
        DescriptorProtos.MethodOptions options = DescriptorProtos.MethodOptions.newBuilder()
                .setExtension(AnnotationsProto.http, HttpRule.newBuilder()
                        .setPut("/v1/items/{item_id}/replace")
                        .setBody("*")
                        .build())
                .build();

        MutinyGrpcGenerator.TranscodingContext tc = build(options).transcodingContext;

        assertThat(tc).isNotNull();
        assertThat(tc.method).isEqualTo("PUT");
        assertThat(tc.path).isEqualTo("/v1/items/{item_id}/replace");
        assertThat(tc.body).isEqualTo("*");
    }

    // --- generated stub content ---

    private static PluginProtos.CodeGeneratorRequest requestWith(DescriptorProtos.ServiceDescriptorProto service) {
        DescriptorProtos.FileDescriptorProto file = DescriptorProtos.FileDescriptorProto.newBuilder()
                .setName("test.proto")
                .setPackage("test")
                .setOptions(DescriptorProtos.FileOptions.newBuilder()
                        .setJavaPackage("io.quarkus.grpc.test")
                        .setJavaMultipleFiles(true)
                        .build())
                .addMessageType(DescriptorProtos.DescriptorProto.newBuilder().setName("TestRequest").build())
                .addMessageType(DescriptorProtos.DescriptorProto.newBuilder().setName("TestResponse").build())
                .addService(service)
                .build();
        return PluginProtos.CodeGeneratorRequest.newBuilder()
                .addFileToGenerate("test.proto")
                .addProtoFile(file)
                .build();
    }

    private static String stub(PluginProtos.CodeGeneratorRequest request) throws GeneratorException {
        return new MutinyGrpcGenerator().generateFiles(request).stream()
                .filter(f -> f.getName().endsWith("MutinyTestServiceGrpc.java"))
                .findFirst()
                .orElseThrow()
                .getContent();
    }

    @Test
    void stubContainsNoTranscodingWhenNoAnnotation() throws GeneratorException {
        PluginProtos.CodeGeneratorRequest request = requestWith(
                DescriptorProtos.ServiceDescriptorProto.newBuilder()
                        .setName("TestService")
                        .addMethod(DescriptorProtos.MethodDescriptorProto.newBuilder()
                                .setName("GetItem")
                                .setInputType(".test.TestRequest")
                                .setOutputType(".test.TestResponse")
                                .build())
                        .build());

        assertThat(stub(request)).doesNotContain("TranscodingServiceMethod");
    }

    @Test
    void stubContainsTranscodingFieldsForGet() throws GeneratorException {
        DescriptorProtos.MethodOptions options = DescriptorProtos.MethodOptions.newBuilder()
                .setExtension(AnnotationsProto.http, HttpRule.newBuilder()
                        .setGet("/v1/items/{item_id}")
                        .build())
                .build();

        PluginProtos.CodeGeneratorRequest request = requestWith(
                DescriptorProtos.ServiceDescriptorProto.newBuilder()
                        .setName("TestService")
                        .addMethod(DescriptorProtos.MethodDescriptorProto.newBuilder()
                                .setName("GetItem")
                                .setInputType(".test.TestRequest")
                                .setOutputType(".test.TestResponse")
                                .setOptions(options)
                                .build())
                        .build());

        String content = stub(request);
        assertThat(content).contains("getItem_OPTIONS");
        assertThat(content).contains("HttpMethod.valueOf(\"GET\")");
        assertThat(content).contains("\"/v1/items/{item_id}\"");
        assertThat(content).contains(
                "TranscodingServiceMethod<io.quarkus.grpc.test.TestRequest, io.quarkus.grpc.test.TestResponse> getItem");
        assertThat(content).contains("MethodCardinality.UNARY");
        assertThat(content).contains("GrpcMessageDecoder.decoder(io.quarkus.grpc.test.TestRequest.newBuilder())");
    }

    @Test
    void stubContainsTranscodingFieldsForPostWithBody() throws GeneratorException {
        DescriptorProtos.MethodOptions options = DescriptorProtos.MethodOptions.newBuilder()
                .setExtension(AnnotationsProto.http, HttpRule.newBuilder()
                        .setPost("/v1/items/{item_id}")
                        .setBody("item")
                        .build())
                .build();

        PluginProtos.CodeGeneratorRequest request = requestWith(
                DescriptorProtos.ServiceDescriptorProto.newBuilder()
                        .setName("TestService")
                        .addMethod(DescriptorProtos.MethodDescriptorProto.newBuilder()
                                .setName("CreateItem")
                                .setInputType(".test.TestRequest")
                                .setOutputType(".test.TestResponse")
                                .setOptions(options)
                                .build())
                        .build());

        String content = stub(request);
        assertThat(content).contains("createItem_OPTIONS");
        assertThat(content).contains("HttpMethod.valueOf(\"POST\")");
        assertThat(content).contains("\"/v1/items/{item_id}\"");
        assertThat(content).contains(".setBody(\"item\")");
    }

    @Test
    void transcodingContextWithAdditionalBindings() {
        DescriptorProtos.MethodOptions options = DescriptorProtos.MethodOptions.newBuilder()
                .setExtension(AnnotationsProto.http, HttpRule.newBuilder()
                        .setGet("/v1/items/{item_id}")
                        .addAdditionalBindings(HttpRule.newBuilder()
                                .setPost("/v1/items")
                                .setBody("*")
                                .build())
                        .build())
                .build();

        MutinyGrpcGenerator.TranscodingContext tc = build(options).transcodingContext;

        assertThat(tc).isNotNull();
        assertThat(tc.method).isEqualTo("GET");
        assertThat(tc.additionalBindings).hasSize(1);
        assertThat(tc.additionalBindings.get(0).method).isEqualTo("POST");
        assertThat(tc.additionalBindings.get(0).path).isEqualTo("/v1/items");
        assertThat(tc.additionalBindings.get(0).body).isEqualTo("*");
    }
}
