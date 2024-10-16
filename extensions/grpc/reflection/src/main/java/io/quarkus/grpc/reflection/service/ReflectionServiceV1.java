package io.quarkus.grpc.reflection.service;

import static com.google.protobuf.Descriptors.FileDescriptor;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import io.grpc.ServerServiceDefinition;
import io.grpc.Status;
import io.grpc.reflection.v1.ErrorResponse;
import io.grpc.reflection.v1.ExtensionNumberResponse;
import io.grpc.reflection.v1.ExtensionRequest;
import io.grpc.reflection.v1.FileDescriptorResponse;
import io.grpc.reflection.v1.ListServiceResponse;
import io.grpc.reflection.v1.MutinyServerReflectionGrpc;
import io.grpc.reflection.v1.ServerReflectionRequest;
import io.grpc.reflection.v1.ServerReflectionResponse;
import io.grpc.reflection.v1.ServiceResponse;
import io.smallrye.mutiny.Multi;

public class ReflectionServiceV1 extends MutinyServerReflectionGrpc.ServerReflectionImplBase {

    private final GrpcServerIndex index;

    public ReflectionServiceV1(List<ServerServiceDefinition> definitions) {
        index = new GrpcServerIndex(definitions);
    }

    @Override
    public Multi<ServerReflectionResponse> serverReflectionInfo(Multi<ServerReflectionRequest> request) {
        return request
                .onItem().transform(new Function<ServerReflectionRequest, ServerReflectionResponse>() {
                    @Override
                    public ServerReflectionResponse apply(ServerReflectionRequest req) {
                        switch (req.getMessageRequestCase()) {
                            case LIST_SERVICES:
                                return ReflectionServiceV1.this.getServiceList(req);
                            case FILE_BY_FILENAME:
                                return ReflectionServiceV1.this.getFileByName(req);
                            case FILE_CONTAINING_SYMBOL:
                                return ReflectionServiceV1.this.getFileContainingSymbol(req);
                            case FILE_CONTAINING_EXTENSION:
                                return ReflectionServiceV1.this.getFileByExtension(req);
                            case ALL_EXTENSION_NUMBERS_OF_TYPE:
                                return ReflectionServiceV1.this.getAllExtensions(req);
                            default:
                                return ReflectionServiceV1.this.getErrorResponse(req, Status.Code.UNIMPLEMENTED,
                                        "not implemented " + req.getMessageRequestCase());

                        }
                    }
                });
    }

    private ServerReflectionResponse getServiceList(ServerReflectionRequest request) {
        ListServiceResponse response = index.getServiceNames().stream()
                .map(new Function<String, ServiceResponse>() { // NOSONAR
                    @Override
                    public ServiceResponse apply(String s) {
                        return ServiceResponse.newBuilder().setName(s).build();
                    }
                })
                .collect(new Supplier<ListServiceResponse.Builder>() {
                    @Override
                    public ListServiceResponse.Builder get() {
                        return ListServiceResponse.newBuilder();
                    }
                },
                        new BiConsumer<ListServiceResponse.Builder, ServiceResponse>() {
                            @Override
                            public void accept(ListServiceResponse.Builder builder, ServiceResponse value) {
                                builder.addService(value);
                            }
                        },
                        new BiConsumer<ListServiceResponse.Builder, ListServiceResponse.Builder>() { // NOSONAR
                            @Override
                            public void accept(ListServiceResponse.Builder b1,
                                    ListServiceResponse.Builder b2) {
                                b1.addAllService(b2.getServiceList());
                            }
                        })
                .build();

        return ServerReflectionResponse.newBuilder()
                .setValidHost(request.getHost())
                .setOriginalRequest(request)
                .setListServicesResponse(response)
                .build();
    }

    private ServerReflectionResponse getFileByName(ServerReflectionRequest request) {
        String name = request.getFileByFilename();
        FileDescriptor fd = index.getFileDescriptorByName(name);
        if (fd != null) {
            return getServerReflectionResponse(request, fd);
        } else {
            return getErrorResponse(request, Status.Code.NOT_FOUND, "File not found (" + name + ")");
        }
    }

    private ServerReflectionResponse getFileContainingSymbol(ServerReflectionRequest request) {
        String symbol = request.getFileContainingSymbol();
        FileDescriptor fd = index.getFileDescriptorBySymbol(symbol);
        if (fd != null) {
            return getServerReflectionResponse(request, fd);
        } else {
            return getErrorResponse(request, Status.Code.NOT_FOUND, "Symbol not found (" + symbol + ")");
        }
    }

    private ServerReflectionResponse getFileByExtension(ServerReflectionRequest request) {
        ExtensionRequest extensionRequest = request.getFileContainingExtension();
        String type = extensionRequest.getContainingType();
        int extension = extensionRequest.getExtensionNumber();
        FileDescriptor fd = index.getFileDescriptorByExtensionAndNumber(type, extension);
        if (fd != null) {
            return getServerReflectionResponse(request, fd);
        } else {
            return getErrorResponse(request, Status.Code.NOT_FOUND,
                    "Extension not found (" + type + ", " + extension + ")");
        }
    }

    private ServerReflectionResponse getAllExtensions(ServerReflectionRequest request) {
        String type = request.getAllExtensionNumbersOfType();
        Set<Integer> extensions = index.getExtensionNumbersOfType(type);
        if (extensions != null) {
            ExtensionNumberResponse.Builder builder = ExtensionNumberResponse.newBuilder()
                    .setBaseTypeName(type)
                    .addAllExtensionNumber(extensions);
            return ServerReflectionResponse.newBuilder()
                    .setValidHost(request.getHost())
                    .setOriginalRequest(request)
                    .setAllExtensionNumbersResponse(builder)
                    .build();
        } else {
            return getErrorResponse(request, Status.Code.NOT_FOUND, "Type not found.");
        }
    }

    private ServerReflectionResponse getServerReflectionResponse(
            ServerReflectionRequest request, FileDescriptor fd) {
        FileDescriptorResponse.Builder fdRBuilder = FileDescriptorResponse.newBuilder();

        // Traverse the descriptors to get the full list of dependencies and add them to the builder
        Set<String> seenFiles = new HashSet<>();
        Queue<FileDescriptor> frontier = new ArrayDeque<>();
        seenFiles.add(fd.getName());
        frontier.add(fd);
        while (!frontier.isEmpty()) {
            FileDescriptor nextFd = frontier.remove();
            fdRBuilder.addFileDescriptorProto(nextFd.toProto().toByteString());
            for (FileDescriptor dependencyFd : nextFd.getDependencies()) {
                if (!seenFiles.contains(dependencyFd.getName())) {
                    seenFiles.add(dependencyFd.getName());
                    frontier.add(dependencyFd);
                }
            }
        }
        return ServerReflectionResponse.newBuilder()
                .setValidHost(request.getHost())
                .setOriginalRequest(request)
                .setFileDescriptorResponse(fdRBuilder)
                .build();
    }

    private ServerReflectionResponse getErrorResponse(
            ServerReflectionRequest request, Status.Code code, String message) {
        return ServerReflectionResponse.newBuilder()
                .setValidHost(request.getHost())
                .setOriginalRequest(request)
                .setErrorResponse(
                        ErrorResponse.newBuilder()
                                .setErrorCode(code.value())
                                .setErrorMessage(message))
                .build();

    }

}
