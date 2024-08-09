package io.quarkus.grpc.cli;

import java.util.List;
import java.util.Optional;

import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;

import io.grpc.CallOptions;
import io.grpc.MethodDescriptor;
import io.grpc.protobuf.ProtoUtils;
import io.grpc.reflection.v1.MutinyServerReflectionGrpc;
import io.grpc.reflection.v1.ServerReflectionRequest;
import io.grpc.reflection.v1.ServerReflectionResponse;
import io.grpc.stub.ClientCalls;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import picocli.CommandLine;

@CommandLine.Command(name = "invoke", sortOptions = false, header = "gRPC invoke")
public class InvokeCommand extends GcurlBaseCommand {

    @CommandLine.Option(names = { "-d" }, description = "Request input")
    Optional<String> content;

    public String getAction() {
        return "invoke";
    }

    @Override
    protected void execute(MutinyServerReflectionGrpc.MutinyServerReflectionStub stub) {
        String toInvoke = unmatched.get(1);
        String[] split = toInvoke.split("/");
        String serviceName = split[0];
        String methodName = split[1];
        ServerReflectionRequest request = ServerReflectionRequest
                .newBuilder()
                .setFileContainingSymbol(serviceName)
                .build();
        Multi<ServerReflectionResponse> response = stub.serverReflectionInfo(Multi.createFrom().item(request));
        response.emitOn(Infrastructure.getDefaultWorkerPool()).toUni().map(r -> {
            ServerReflectionResponse.MessageResponseCase responseCase = r.getMessageResponseCase();
            if (responseCase == ServerReflectionResponse.MessageResponseCase.FILE_DESCRIPTOR_RESPONSE) {
                List<ByteString> byteStrings = r.getFileDescriptorResponse().getFileDescriptorProtoList();
                for (Descriptors.FileDescriptor fd : getFileDescriptorsFromProtos(byteStrings)) {
                    fd.getServices().forEach(
                            sd -> {
                                String fullName = sd.getFullName();
                                if (fullName.equals(serviceName)) {
                                    Descriptors.MethodDescriptor md = sd.findMethodByName(methodName);
                                    if (md != null) {
                                        invokeMethod(md);
                                    } else {
                                        log("Method not found: " + methodName);
                                    }
                                }
                            });
                }
            } else {
                err("Unexpected response from server reflection: " + responseCase);
            }
            return null;
        }).await().indefinitely();
    }

    private void invokeMethod(Descriptors.MethodDescriptor md) {
        String fullMethodName = md.getService().getFullName() + "/" + md.getName();
        Descriptors.Descriptor inputType = md.getInputType();
        DynamicMessage.Builder messageBuilder = DynamicMessage.newBuilder(inputType);
        try {
            content.ifPresent(request -> {
                try {
                    JsonFormat.parser().merge(request, messageBuilder);
                } catch (InvalidProtocolBufferException e) {
                    throw new RuntimeException(e);
                }
            });
            DynamicMessage msg = messageBuilder.build();
            MethodDescriptor.MethodType methodType = MethodDescriptor.MethodType.UNARY;
            if (md.isClientStreaming()) {
                methodType = MethodDescriptor.MethodType.CLIENT_STREAMING;
            }
            if (md.isServerStreaming()) {
                methodType = MethodDescriptor.MethodType.SERVER_STREAMING;
            }
            if (md.isClientStreaming() && md.isServerStreaming()) {
                methodType = MethodDescriptor.MethodType.BIDI_STREAMING;
            }
            MethodDescriptor<DynamicMessage, DynamicMessage> methodDescriptor = io.grpc.MethodDescriptor
                    .<DynamicMessage, DynamicMessage> newBuilder()
                    .setType(methodType)
                    .setFullMethodName(fullMethodName)
                    .setRequestMarshaller(ProtoUtils.marshaller(DynamicMessage.getDefaultInstance(inputType)))
                    .setResponseMarshaller(
                            ProtoUtils.marshaller(DynamicMessage.getDefaultInstance(md.getOutputType())))
                    .build();

            execute(channel -> {
                DynamicMessage response = ClientCalls.blockingUnaryCall(
                        channel,
                        methodDescriptor,
                        CallOptions.DEFAULT,
                        msg);

                try {
                    log(JsonFormat.printer().print(response));
                } catch (InvalidProtocolBufferException e) {
                    throw new RuntimeException(e);
                }
                return null;
            });
        } catch (Exception e) {
            err("Error creating dynamic message: " + e.getMessage());
        }
    }
}
