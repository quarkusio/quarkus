package io.quarkus.google.cloud.pubsub.runtime.graal;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import org.threeten.bp.Duration;

import com.google.api.core.ApiFunction;
import com.google.api.gax.grpc.ChannelPrimer;
import com.google.api.gax.grpc.GrpcHeaderInterceptor;
import com.google.api.gax.grpc.GrpcInterceptorProvider;
import com.google.api.gax.rpc.HeaderProvider;
import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.grpc.*;

@TargetClass(className = "com.google.api.gax.grpc.InstantiatingGrpcChannelProvider")
final class Target_com_google_api_gax_grpc_InstantiatingGrpcChannelProvider {

    @Alias
    private Executor executor;
    @Alias
    private HeaderProvider headerProvider;
    @Alias
    private GrpcInterceptorProvider interceptorProvider;
    @Alias
    private String endpoint;
    @Alias
    private Integer maxInboundMessageSize;
    @Alias
    private Integer maxInboundMetadataSize;
    @Alias
    private Duration keepAliveTime;
    @Alias
    private Duration keepAliveTimeout;
    @Alias
    private Boolean keepAliveWithoutCalls;
    @Alias
    private ChannelPrimer channelPrimer;
    @Alias
    private ApiFunction<ManagedChannelBuilder, ManagedChannelBuilder> channelConfigurator;

    @Substitute
    private ManagedChannel createSingleChannel() throws IOException {
        GrpcHeaderInterceptor headerInterceptor = new GrpcHeaderInterceptor(this.headerProvider.getHeaders());
        Target_com_google_api_gax_grpc_GrpcMetadataHandlerInterceptor metadataHandlerInterceptor = new Target_com_google_api_gax_grpc_GrpcMetadataHandlerInterceptor();
        int colon = this.endpoint.lastIndexOf(58);
        if (colon < 0) {
            throw new IllegalStateException("invalid endpoint - should have been validated: " + this.endpoint);
        } else {
            int port = Integer.parseInt(this.endpoint.substring(colon + 1));
            String serviceAddress = this.endpoint.substring(0, colon);
            //            Object builder;
            //            if (this.isDirectPathEnabled(serviceAddress) && this.credentials instanceof ComputeEngineCredentials) {
            //                builder = ComputeEngineChannelBuilder.forAddress(serviceAddress, port);
            //                ((ManagedChannelBuilder)builder).keepAliveTime(3600L, TimeUnit.SECONDS);
            //                ((ManagedChannelBuilder)builder).keepAliveTimeout(20L, TimeUnit.SECONDS);
            //                ImmutableMap<String, Object> pickFirstStrategy = ImmutableMap.of("pick_first", ImmutableMap.of());
            //                ImmutableMap<String, Object> childPolicy = ImmutableMap.of("childPolicy", ImmutableList.of(pickFirstStrategy));
            //                ImmutableMap<String, Object> grpcLbPolicy = ImmutableMap.of("grpclb", childPolicy);
            //                ImmutableMap<String, Object> loadBalancingConfig = ImmutableMap.of("loadBalancingConfig", ImmutableList.of(grpcLbPolicy));
            //                ((ManagedChannelBuilder)builder).defaultServiceConfig(loadBalancingConfig);
            //            } else {
            //                builder = ManagedChannelBuilder.forAddress(serviceAddress, port);
            //            }

            ManagedChannelBuilder builder = ManagedChannelBuilder.forAddress(serviceAddress, port);
            builder = ((ManagedChannelBuilder) builder).disableServiceConfigLookUp()
                    .intercept(new ClientInterceptor[] { new Target_com_google_api_gax_grpc_GrpcChannelUUIDInterceptor() })
                    .intercept(new ClientInterceptor[] { headerInterceptor })
                    .intercept(new ClientInterceptor[] { metadataHandlerInterceptor })
                    .userAgent(headerInterceptor.getUserAgentHeader()).executor(this.executor);
            if (this.maxInboundMetadataSize != null) {
                builder.maxInboundMetadataSize(this.maxInboundMetadataSize);
            }

            if (this.maxInboundMessageSize != null) {
                builder.maxInboundMessageSize(this.maxInboundMessageSize);
            }

            if (this.keepAliveTime != null) {
                builder.keepAliveTime(this.keepAliveTime.toMillis(), TimeUnit.MILLISECONDS);
            }

            if (this.keepAliveTimeout != null) {
                builder.keepAliveTimeout(this.keepAliveTimeout.toMillis(), TimeUnit.MILLISECONDS);
            }

            if (this.keepAliveWithoutCalls != null) {
                builder.keepAliveWithoutCalls(this.keepAliveWithoutCalls);
            }

            if (this.interceptorProvider != null) {
                builder.intercept(this.interceptorProvider.getInterceptors());
            }

            if (this.channelConfigurator != null) {
                builder = (ManagedChannelBuilder) this.channelConfigurator.apply(builder);
            }

            ManagedChannel managedChannel = builder.build();
            if (this.channelPrimer != null) {
                this.channelPrimer.primeChannel(managedChannel);
            }

            return managedChannel;
        }
    }
}

@TargetClass(className = "com.google.api.gax.grpc.GrpcMetadataHandlerInterceptor")
final class Target_com_google_api_gax_grpc_GrpcMetadataHandlerInterceptor implements ClientInterceptor {

    @Alias()
    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method, final CallOptions callOptions, Channel next) {
        throw new UnsupportedOperationException("Alias should not be called");
    }
}

@TargetClass(className = "com.google.api.gax.grpc.GrpcChannelUUIDInterceptor")
final class Target_com_google_api_gax_grpc_GrpcChannelUUIDInterceptor implements ClientInterceptor {

    @Alias
    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> methodDescriptor, CallOptions callOptions, Channel channel) {
        throw new UnsupportedOperationException("Alias should not be called");
    }
}

class GoogleApiGaxGrpcSubstitutions {
}
