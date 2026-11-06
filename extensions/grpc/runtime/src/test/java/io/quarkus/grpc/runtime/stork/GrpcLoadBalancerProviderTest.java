package io.quarkus.grpc.runtime.stork;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import io.grpc.Attributes;
import io.grpc.ConnectivityState;
import io.grpc.ConnectivityStateInfo;
import io.grpc.EquivalentAddressGroup;
import io.grpc.LoadBalancer;
import io.grpc.Status;
import io.smallrye.stork.api.ServiceInstance;
import io.smallrye.stork.impl.DefaultServiceInstance;

class GrpcLoadBalancerProviderTest {

    private static final String SERVICE_NAME = "hello-service";

    @Test
    void reusesSubchannelWhenEndpointIsUnchanged() {
        LoadBalancer.Helper helper = mock(LoadBalancer.Helper.class);
        List<LoadBalancer.SubchannelStateListener> listeners = new ArrayList<>();
        LoadBalancer.Subchannel subchannel = subchannel(listeners);
        when(helper.createSubchannel(any())).thenReturn(subchannel);

        LoadBalancer loadBalancer = new GrpcLoadBalancerProvider(true).newLoadBalancer(helper);
        loadBalancer.handleResolvedAddresses(resolvedAddresses(addressGroup(instance(1, 9001), 9001)));
        loadBalancer.handleResolvedAddresses(resolvedAddresses(addressGroup(instance(2, 9001), 9001)));

        verify(helper, times(1)).createSubchannel(any());
        verify(subchannel).updateAddresses(any());
        verify(subchannel, never()).shutdown();
    }

    @Test
    void shutsDownSubchannelWhenEndpointIsRemoved() {
        LoadBalancer.Helper helper = mock(LoadBalancer.Helper.class);
        List<LoadBalancer.SubchannelStateListener> listeners = new ArrayList<>();
        LoadBalancer.Subchannel first = subchannel(listeners);
        LoadBalancer.Subchannel second = subchannel(listeners);
        when(helper.createSubchannel(any())).thenReturn(first, second);

        LoadBalancer loadBalancer = new GrpcLoadBalancerProvider(true).newLoadBalancer(helper);
        loadBalancer.handleResolvedAddresses(resolvedAddresses(
                addressGroup(instance(1, 9001), 9001),
                addressGroup(instance(2, 9002), 9002)));
        loadBalancer.handleResolvedAddresses(resolvedAddresses(addressGroup(instance(2, 9002), 9002)));

        verify(first).shutdown();
        verify(second, never()).shutdown();
    }

    @Test
    void shutsDownSubchannelWhenEndpointChangesForSameServiceInstance() {
        LoadBalancer.Helper helper = mock(LoadBalancer.Helper.class);
        List<LoadBalancer.SubchannelStateListener> listeners = new ArrayList<>();
        LoadBalancer.Subchannel first = subchannel(listeners);
        LoadBalancer.Subchannel second = subchannel(listeners);
        when(helper.createSubchannel(any())).thenReturn(first, second);

        LoadBalancer loadBalancer = new GrpcLoadBalancerProvider(true).newLoadBalancer(helper);
        loadBalancer.handleResolvedAddresses(resolvedAddresses(addressGroup(instance(1, 9001), 9001)));
        loadBalancer.handleResolvedAddresses(resolvedAddresses(addressGroup(instance(1, 9002), 9002)));

        verify(helper, times(2)).createSubchannel(any());
        verify(first).shutdown();
        verify(second, never()).shutdown();
    }

    @Test
    void shutdownClosesManagedSubchannels() {
        LoadBalancer.Helper helper = mock(LoadBalancer.Helper.class);
        List<LoadBalancer.SubchannelStateListener> listeners = new ArrayList<>();
        LoadBalancer.Subchannel first = subchannel(listeners);
        LoadBalancer.Subchannel second = subchannel(listeners);
        when(helper.createSubchannel(any())).thenReturn(first, second);

        LoadBalancer loadBalancer = new GrpcLoadBalancerProvider(true).newLoadBalancer(helper);
        loadBalancer.handleResolvedAddresses(resolvedAddresses(
                addressGroup(instance(1, 9001), 9001),
                addressGroup(instance(2, 9002), 9002)));
        loadBalancer.shutdown();

        verify(first).shutdown();
        verify(second).shutdown();
    }

    @Test
    void transientFailureRefreshesNameResolutionWithoutDuplicatingSubchannels() {
        LoadBalancer.Helper helper = mock(LoadBalancer.Helper.class);
        List<LoadBalancer.SubchannelStateListener> listeners = new ArrayList<>();
        LoadBalancer.Subchannel subchannel = subchannel(listeners);
        when(helper.createSubchannel(any())).thenReturn(subchannel);

        LoadBalancer loadBalancer = new GrpcLoadBalancerProvider(true).newLoadBalancer(helper);
        EquivalentAddressGroup addressGroup = addressGroup(instance(1, 9001), 9001);
        loadBalancer.handleResolvedAddresses(resolvedAddresses(addressGroup));
        clearInvocations(helper);

        listeners.get(0).onSubchannelState(ConnectivityStateInfo.forTransientFailure(Status.UNAVAILABLE));
        loadBalancer.handleResolvedAddresses(resolvedAddresses(addressGroup(instance(1, 9001), 9001)));

        verify(helper).refreshNameResolution();
        verify(helper, never()).createSubchannel(any());
    }

    @Test
    void transientFailureUsesErrorPickerWhenNoSubchannelIsReady() {
        LoadBalancer.Helper helper = mock(LoadBalancer.Helper.class);
        List<LoadBalancer.SubchannelStateListener> listeners = new ArrayList<>();
        LoadBalancer.Subchannel subchannel = subchannel(listeners);
        when(helper.createSubchannel(any())).thenReturn(subchannel);

        LoadBalancer loadBalancer = new GrpcLoadBalancerProvider(true).newLoadBalancer(helper);
        loadBalancer.handleResolvedAddresses(resolvedAddresses(addressGroup(instance(1, 9001), 9001)));
        clearInvocations(helper);

        listeners.get(0).onSubchannelState(ConnectivityStateInfo.forTransientFailure(Status.UNAVAILABLE));

        ArgumentCaptor<LoadBalancer.SubchannelPicker> picker = ArgumentCaptor.forClass(LoadBalancer.SubchannelPicker.class);
        verify(helper).updateBalancingState(eq(ConnectivityState.TRANSIENT_FAILURE), picker.capture());
        assertThat(picker.getValue()).isInstanceOf(GrpcLoadBalancerProvider.ErrorPicker.class);
    }

    @Test
    void transientFailureKeepsReadyStateWhenAnotherSubchannelIsReady() {
        LoadBalancer.Helper helper = mock(LoadBalancer.Helper.class);
        List<LoadBalancer.SubchannelStateListener> listeners = new ArrayList<>();
        LoadBalancer.Subchannel first = subchannel(listeners);
        LoadBalancer.Subchannel second = subchannel(listeners);
        when(helper.createSubchannel(any())).thenReturn(first, second);

        LoadBalancer loadBalancer = new GrpcLoadBalancerProvider(true).newLoadBalancer(helper);
        loadBalancer.handleResolvedAddresses(resolvedAddresses(
                addressGroup(instance(1, 9001), 9001),
                addressGroup(instance(2, 9002), 9002)));
        listeners.get(0).onSubchannelState(ConnectivityStateInfo.forNonError(ConnectivityState.READY));
        clearInvocations(helper);

        listeners.get(1).onSubchannelState(ConnectivityStateInfo.forTransientFailure(Status.UNAVAILABLE));

        verify(helper).refreshNameResolution();
        verify(helper).updateBalancingState(eq(ConnectivityState.READY), any());
    }

    @Test
    void idleRefreshesNameResolutionAndRequestsConnectionWhenEnabled() {
        LoadBalancer.Helper helper = mock(LoadBalancer.Helper.class);
        List<LoadBalancer.SubchannelStateListener> listeners = new ArrayList<>();
        LoadBalancer.Subchannel subchannel = subchannel(listeners);
        when(helper.createSubchannel(any())).thenReturn(subchannel);

        LoadBalancer loadBalancer = new GrpcLoadBalancerProvider(true).newLoadBalancer(helper);
        loadBalancer.handleResolvedAddresses(resolvedAddresses(addressGroup(instance(1, 9001), 9001)));
        clearInvocations(helper, subchannel);

        listeners.get(0).onSubchannelState(ConnectivityStateInfo.forNonError(ConnectivityState.IDLE));

        verify(helper).refreshNameResolution();
        verify(subchannel).requestConnection();
        verify(helper).updateBalancingState(eq(ConnectivityState.CONNECTING), any());
    }

    @Test
    void idleDoesNotRequestConnectionWhenDisabled() {
        LoadBalancer.Helper helper = mock(LoadBalancer.Helper.class);
        List<LoadBalancer.SubchannelStateListener> listeners = new ArrayList<>();
        LoadBalancer.Subchannel subchannel = subchannel(listeners);
        when(helper.createSubchannel(any())).thenReturn(subchannel);

        LoadBalancer loadBalancer = new GrpcLoadBalancerProvider(false).newLoadBalancer(helper);
        loadBalancer.handleResolvedAddresses(resolvedAddresses(addressGroup(instance(1, 9001), 9001)));
        clearInvocations(helper, subchannel);

        listeners.get(0).onSubchannelState(ConnectivityStateInfo.forNonError(ConnectivityState.IDLE));

        verify(helper).refreshNameResolution();
        verify(subchannel, never()).requestConnection();
    }

    @Test
    void shutdownStateFromActiveSubchannelIsIgnored() {
        LoadBalancer.Helper helper = mock(LoadBalancer.Helper.class);
        List<LoadBalancer.SubchannelStateListener> listeners = new ArrayList<>();
        LoadBalancer.Subchannel subchannel = subchannel(listeners);
        when(helper.createSubchannel(any())).thenReturn(subchannel);

        LoadBalancer loadBalancer = new GrpcLoadBalancerProvider(true).newLoadBalancer(helper);
        loadBalancer.handleResolvedAddresses(resolvedAddresses(addressGroup(instance(1, 9001), 9001)));
        clearInvocations(helper);

        listeners.get(0).onSubchannelState(ConnectivityStateInfo.forNonError(ConnectivityState.SHUTDOWN));

        verify(helper, never()).updateBalancingState(any(), any());
        verify(helper, never()).refreshNameResolution();
    }

    @Test
    void ignoresStateChangesFromRemovedSubchannels() {
        LoadBalancer.Helper helper = mock(LoadBalancer.Helper.class);
        List<LoadBalancer.SubchannelStateListener> listeners = new ArrayList<>();
        LoadBalancer.Subchannel first = subchannel(listeners);
        LoadBalancer.Subchannel second = subchannel(listeners);
        when(helper.createSubchannel(any())).thenReturn(first, second);

        LoadBalancer loadBalancer = new GrpcLoadBalancerProvider(true).newLoadBalancer(helper);
        loadBalancer.handleResolvedAddresses(resolvedAddresses(addressGroup(instance(1, 9001), 9001)));
        LoadBalancer.SubchannelStateListener removedListener = listeners.get(0);
        loadBalancer.handleResolvedAddresses(resolvedAddresses(addressGroup(instance(2, 9002), 9002)));
        clearInvocations(helper);

        removedListener.onSubchannelState(ConnectivityStateInfo.forNonError(ConnectivityState.READY));

        verify(helper, never()).updateBalancingState(any(), any());
        verify(helper, never()).refreshNameResolution();
    }

    @Test
    void ignoresTransientFailureFromRemovedSubchannel() {
        LoadBalancer.Helper helper = mock(LoadBalancer.Helper.class);
        List<LoadBalancer.SubchannelStateListener> listeners = new ArrayList<>();
        LoadBalancer.Subchannel first = subchannel(listeners);
        LoadBalancer.Subchannel second = subchannel(listeners);
        when(helper.createSubchannel(any())).thenReturn(first, second);

        LoadBalancer loadBalancer = new GrpcLoadBalancerProvider(true).newLoadBalancer(helper);
        loadBalancer.handleResolvedAddresses(resolvedAddresses(addressGroup(instance(1, 9001), 9001)));
        LoadBalancer.SubchannelStateListener removedListener = listeners.get(0);
        loadBalancer.handleResolvedAddresses(resolvedAddresses(addressGroup(instance(2, 9002), 9002)));
        clearInvocations(helper);

        removedListener.onSubchannelState(ConnectivityStateInfo.forTransientFailure(Status.UNAVAILABLE));

        verify(helper, never()).updateBalancingState(any(), any());
        verify(helper, never()).refreshNameResolution();
    }

    @Test
    void emptyResolutionShutsDownExistingSubchannelsAndReportsUnavailable() {
        LoadBalancer.Helper helper = mock(LoadBalancer.Helper.class);
        List<LoadBalancer.SubchannelStateListener> listeners = new ArrayList<>();
        LoadBalancer.Subchannel subchannel = subchannel(listeners);
        when(helper.createSubchannel(any())).thenReturn(subchannel);

        LoadBalancer loadBalancer = new GrpcLoadBalancerProvider(true).newLoadBalancer(helper);
        loadBalancer.handleResolvedAddresses(resolvedAddresses(addressGroup(instance(1, 9001), 9001)));
        clearInvocations(helper);

        loadBalancer.handleResolvedAddresses(resolvedAddresses());

        verify(subchannel).shutdown();
        verify(helper).updateBalancingState(eq(ConnectivityState.TRANSIENT_FAILURE),
                any(GrpcLoadBalancerProvider.ErrorPicker.class));
    }

    @Test
    void addressGroupWithoutServiceInstanceIsIgnored() {
        LoadBalancer.Helper helper = mock(LoadBalancer.Helper.class);
        LoadBalancer loadBalancer = new GrpcLoadBalancerProvider(true).newLoadBalancer(helper);

        loadBalancer.handleResolvedAddresses(resolvedAddresses(new EquivalentAddressGroup(
                List.of(new InetSocketAddress("127.0.0.1", 9001)))));

        verify(helper, never()).createSubchannel(any());
        verify(helper).updateBalancingState(eq(ConnectivityState.TRANSIENT_FAILURE),
                any(GrpcLoadBalancerProvider.ErrorPicker.class));
    }

    @Test
    void nameResolutionErrorReportsFailureWhenNoSubchannelIsReady() {
        LoadBalancer.Helper helper = mock(LoadBalancer.Helper.class);
        LoadBalancer loadBalancer = new GrpcLoadBalancerProvider(true).newLoadBalancer(helper);

        loadBalancer.handleNameResolutionError(Status.UNAVAILABLE);

        verify(helper).updateBalancingState(eq(ConnectivityState.TRANSIENT_FAILURE),
                any(GrpcLoadBalancerProvider.ErrorPicker.class));
    }

    @Test
    void nameResolutionErrorDoesNotOverrideReadySubchannel() {
        LoadBalancer.Helper helper = mock(LoadBalancer.Helper.class);
        List<LoadBalancer.SubchannelStateListener> listeners = new ArrayList<>();
        LoadBalancer.Subchannel subchannel = subchannel(listeners);
        when(helper.createSubchannel(any())).thenReturn(subchannel);

        LoadBalancer loadBalancer = new GrpcLoadBalancerProvider(true).newLoadBalancer(helper);
        loadBalancer.handleResolvedAddresses(resolvedAddresses(addressGroup(instance(1, 9001), 9001)));
        listeners.get(0).onSubchannelState(ConnectivityStateInfo.forNonError(ConnectivityState.READY));
        clearInvocations(helper);

        loadBalancer.handleNameResolutionError(Status.UNAVAILABLE);

        verify(helper, never()).updateBalancingState(any(), any());
    }

    @Test
    void sameEndpointUpdatesServiceInstanceMetadataWithoutLosingReadyState() {
        LoadBalancer.Helper helper = mock(LoadBalancer.Helper.class);
        List<LoadBalancer.SubchannelStateListener> listeners = new ArrayList<>();
        LoadBalancer.Subchannel subchannel = subchannel(listeners);
        when(helper.createSubchannel(any())).thenReturn(subchannel);

        LoadBalancer loadBalancer = new GrpcLoadBalancerProvider(true).newLoadBalancer(helper);
        loadBalancer.handleResolvedAddresses(resolvedAddresses(addressGroup(instance(1, 9001), 9001)));
        listeners.get(0).onSubchannelState(ConnectivityStateInfo.forNonError(ConnectivityState.READY));
        clearInvocations(helper);

        loadBalancer.handleResolvedAddresses(resolvedAddresses(addressGroup(instance(2, 9001), 9001)));
        loadBalancer.handleNameResolutionError(Status.UNAVAILABLE);

        verify(helper, times(1)).updateBalancingState(eq(ConnectivityState.READY), any());
        verify(helper, never()).updateBalancingState(eq(ConnectivityState.TRANSIENT_FAILURE), any());
        verify(helper, never()).createSubchannel(any());
    }

    @Test
    void pickerReturnsErrorStatus() {
        LoadBalancer.PickResult result = new GrpcLoadBalancerProvider.ErrorPicker(Status.UNAVAILABLE)
                .pickSubchannel(mock(LoadBalancer.PickSubchannelArgs.class));

        assertThat(result.getStatus().getCode()).isEqualTo(Status.Code.UNAVAILABLE);
    }

    private static LoadBalancer.Subchannel subchannel(List<LoadBalancer.SubchannelStateListener> listeners) {
        LoadBalancer.Subchannel subchannel = mock(LoadBalancer.Subchannel.class);
        doAnswer(invocation -> {
            listeners.add(invocation.getArgument(0));
            return null;
        }).when(subchannel).start(any());
        return subchannel;
    }

    private static LoadBalancer.ResolvedAddresses resolvedAddresses(EquivalentAddressGroup... addressGroups) {
        return LoadBalancer.ResolvedAddresses.newBuilder()
                .setAddresses(List.of(addressGroups))
                .setLoadBalancingPolicyConfig(new GrpcLoadBalancerProvider.StorkLoadBalancerConfig(SERVICE_NAME))
                .build();
    }

    private static EquivalentAddressGroup addressGroup(ServiceInstance instance, int port) {
        Attributes attributes = Attributes.newBuilder()
                .set(GrpcStorkServiceDiscovery.SERVICE_INSTANCE, instance)
                .build();
        return new EquivalentAddressGroup(List.of(new InetSocketAddress("127.0.0.1", port)), attributes);
    }

    private static DefaultServiceInstance instance(long id, int port) {
        return new DefaultServiceInstance(id, "127.0.0.1", port, false);
    }
}
