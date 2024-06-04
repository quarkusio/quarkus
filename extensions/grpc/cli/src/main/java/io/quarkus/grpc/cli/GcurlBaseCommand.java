package io.quarkus.grpc.cli;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.protobuf.ByteString;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.ProtocolStringList;

import io.grpc.Channel;
import io.grpc.reflection.v1.MutinyServerReflectionGrpc;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.net.SocketAddress;
import io.vertx.grpc.client.GrpcClient;
import io.vertx.grpc.client.GrpcClientChannel;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

public abstract class GcurlBaseCommand implements Callable<Integer> {

    @Spec
    CommandSpec spec;

    @CommandLine.Unmatched
    List<String> unmatched;

    Vertx vertx = Vertx.vertx();

    /**
     * The grpc subcommand (e.g. list, describe, invoke)
     *
     * @return the subcommand
     */
    protected abstract String getAction();

    protected abstract void execute(MutinyServerReflectionGrpc.MutinyServerReflectionStub stub);

    protected void log(String msg) {
        System.out.println(msg);
    }

    protected void err(String msg) {
        System.err.println(msg);
    }

    protected static List<Descriptors.FileDescriptor> getFileDescriptorsFromProtos(List<ByteString> protos) {
        try {
            Map<String, DescriptorProtos.FileDescriptorProto> all = protos
                    .stream()
                    .map(bs -> {
                        try {
                            return DescriptorProtos.FileDescriptorProto.parseFrom(bs);
                        } catch (InvalidProtocolBufferException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .collect(Collectors.toMap(DescriptorProtos.FileDescriptorProto::getName, Function.identity(), (a, b) -> a));
            List<Descriptors.FileDescriptor> fds = new ArrayList<>();
            Map<String, Descriptors.FileDescriptor> resolved = new HashMap<>();
            for (DescriptorProtos.FileDescriptorProto fdp : all.values()) {
                fds.add(toFileDescriptor(fdp, all, resolved));
            }
            return fds;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Descriptors.FileDescriptor toFileDescriptor(DescriptorProtos.FileDescriptorProto fdp,
            Map<String, DescriptorProtos.FileDescriptorProto> all, Map<String, Descriptors.FileDescriptor> resolved) {
        int n = fdp.getDependencyCount();
        ProtocolStringList list = fdp.getDependencyList();
        Descriptors.FileDescriptor[] fds = new Descriptors.FileDescriptor[n];
        for (int i = 0; i < n; i++) {
            String dep = list.get(i);
            // remember resolved FDs, recursively resolve deps
            fds[i] = resolved.computeIfAbsent(dep, key -> {
                DescriptorProtos.FileDescriptorProto proto = all.get(key);
                return toFileDescriptor(proto, all, resolved);
            });
        }
        try {
            return Descriptors.FileDescriptor.buildFrom(fdp, fds);
        } catch (Descriptors.DescriptorValidationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Integer call() {
        if (unmatched == null || unmatched.isEmpty()) {
            log("Missing host:port");
            return CommandLine.ExitCode.USAGE;
        }

        return execute(channel -> {
            try {
                MutinyServerReflectionGrpc.MutinyServerReflectionStub stub = MutinyServerReflectionGrpc.newMutinyStub(channel);
                execute(stub);
                return CommandLine.ExitCode.OK;
            } catch (Exception e) {
                err("Failed to execute grpc " + getAction() + ", due to: " + e.getMessage());
                return CommandLine.ExitCode.SOFTWARE;
            }
        });
    }

    protected <X> X execute(Function<Channel, X> fn) {
        HttpClientOptions options = new HttpClientOptions(); // TODO
        options.setHttp2ClearTextUpgrade(false);

        GrpcClient client = GrpcClient.client(vertx, options);
        String[] split = unmatched.get(0).split(":");
        String host = split[0];
        int port = Integer.parseInt(split[1]);
        Channel channel = new GrpcClientChannel(client, SocketAddress.inetSocketAddress(port, host));
        try {
            return fn.apply(channel);
        } finally {
            client.close().toCompletionStage().toCompletableFuture().join();
        }
    }
}
