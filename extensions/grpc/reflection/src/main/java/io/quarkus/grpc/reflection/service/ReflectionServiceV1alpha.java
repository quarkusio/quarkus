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

import grpc.reflection.v1alpha.MutinyServerReflectionGrpc;
import grpc.reflection.v1alpha.Reflection;
import io.grpc.ServerServiceDefinition;
import io.grpc.Status;
import io.smallrye.mutiny.Multi;

public class ReflectionServiceV1alpha extends MutinyServerReflectionGrpc.ServerReflectionImplBase {

    private final GrpcServerIndex index;

    public ReflectionServiceV1alpha(List<ServerServiceDefinition> definitions) {
        index = new GrpcServerIndex(definitions);
    }

    @Override
    public Multi<Reflection.ServerReflectionResponse> serverReflectionInfo(Multi<Reflection.ServerReflectionRequest> request) {
        return request
                .onItem().transform(new Function<Reflection.ServerReflectionRequest, Reflection.ServerReflectionResponse>() {
                    @Override
                    public Reflection.ServerReflectionResponse apply(Reflection.ServerReflectionRequest req) {
                        switch (req.getMessageRequestCase()) {
                            case LIST_SERVICES:
                                return ReflectionServiceV1alpha.this.getServiceList(req);
                            case FILE_BY_FILENAME:
                                return ReflectionServiceV1alpha.this.getFileByName(req);
                            case FILE_CONTAINING_SYMBOL:
                                return ReflectionServiceV1alpha.this.getFileContainingSymbol(req);
                            case FILE_CONTAINING_EXTENSION:
                                return ReflectionServiceV1alpha.this.getFileByExtension(req);
                            case ALL_EXTENSION_NUMBERS_OF_TYPE:
                                return ReflectionServiceV1alpha.this.getAllExtensions(req);
                            default:
                                return ReflectionServiceV1alpha.this.getErrorResponse(req, Status.Code.UNIMPLEMENTED,
                                        "not implemented " + req.getMessageRequestCase());

                        }
                    }
                });
    }

    private Reflection.ServerReflectionResponse getServiceList(Reflection.ServerReflectionRequest request) {
        Reflection.ListServiceResponse response = index.getServiceNames().stream()
                .map(new Function<String, Reflection.ServiceResponse>() { // NOSONAR
                    @Override
                    public Reflection.ServiceResponse apply(String s) {
                        return Reflection.ServiceResponse.newBuilder().setName(s).build();
                    }
                })
                .collect(new Supplier<Reflection.ListServiceResponse.Builder>() {
                    @Override
                    public Reflection.ListServiceResponse.Builder get() {
                        return Reflection.ListServiceResponse.newBuilder();
                    }
                },
                        new BiConsumer<Reflection.ListServiceResponse.Builder, Reflection.ServiceResponse>() {
                            @Override
                            public void accept(Reflection.ListServiceResponse.Builder builder,
                                    Reflection.ServiceResponse value) {
                                builder.addService(value);
                            }
                        },
                        new BiConsumer<Reflection.ListServiceResponse.Builder, Reflection.ListServiceResponse.Builder>() { // NOSONAR
                            @Override
                            public void accept(Reflection.ListServiceResponse.Builder b1,
                                    Reflection.ListServiceResponse.Builder b2) {
                                b1.addAllService(b2.getServiceList());
                            }
                        })
                .build();

        return Reflection.ServerReflectionResponse.newBuilder()
                .setValidHost(request.getHost())
                .setOriginalRequest(request)
                .setListServicesResponse(response)
                .build();
    }

    private Reflection.ServerReflectionResponse getFileByName(Reflection.ServerReflectionRequest request) {
        String name = request.getFileByFilename();
        FileDescriptor fd = index.getFileDescriptorByName(name);
        if (fd != null) {
            return getServerReflectionResponse(request, fd);
        } else {
            return getErrorResponse(request, Status.Code.NOT_FOUND, "File not found (" + name + ")");
        }
    }

    private Reflection.ServerReflectionResponse getFileContainingSymbol(Reflection.ServerReflectionRequest request) {
        String symbol = request.getFileContainingSymbol();
        FileDescriptor fd = index.getFileDescriptorBySymbol(symbol);
        if (fd != null) {
            return getServerReflectionResponse(request, fd);
        } else {
            return getErrorResponse(request, Status.Code.NOT_FOUND, "Symbol not found (" + symbol + ")");
        }
    }

    private Reflection.ServerReflectionResponse getFileByExtension(Reflection.ServerReflectionRequest request) {
        Reflection.ExtensionRequest extensionRequest = request.getFileContainingExtension();
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

    private Reflection.ServerReflectionResponse getAllExtensions(Reflection.ServerReflectionRequest request) {
        String type = request.getAllExtensionNumbersOfType();
        Set<Integer> extensions = index.getExtensionNumbersOfType(type);
        if (extensions != null) {
            Reflection.ExtensionNumberResponse.Builder builder = Reflection.ExtensionNumberResponse.newBuilder()
                    .setBaseTypeName(type)
                    .addAllExtensionNumber(extensions);
            return Reflection.ServerReflectionResponse.newBuilder()
                    .setValidHost(request.getHost())
                    .setOriginalRequest(request)
                    .setAllExtensionNumbersResponse(builder)
                    .build();
        } else {
            return getErrorResponse(request, Status.Code.NOT_FOUND, "Type not found.");
        }
    }

    private Reflection.ServerReflectionResponse getServerReflectionResponse(
            Reflection.ServerReflectionRequest request, FileDescriptor fd) {
        Reflection.FileDescriptorResponse.Builder fdRBuilder = Reflection.FileDescriptorResponse.newBuilder();

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
        return Reflection.ServerReflectionResponse.newBuilder()
                .setValidHost(request.getHost())
                .setOriginalRequest(request)
                .setFileDescriptorResponse(fdRBuilder)
                .build();
    }

    private Reflection.ServerReflectionResponse getErrorResponse(
            Reflection.ServerReflectionRequest request, Status.Code code, String message) {
        return Reflection.ServerReflectionResponse.newBuilder()
                .setValidHost(request.getHost())
                .setOriginalRequest(request)
                .setErrorResponse(
                        Reflection.ErrorResponse.newBuilder()
                                .setErrorCode(code.value())
                                .setErrorMessage(message))
                .build();

    }

}
