package io.quarkus.cxf.runtime.transport;

import java.io.IOException;
import java.util.logging.Logger;

import org.apache.cxf.Bus;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.cxf.transport.http.DestinationRegistry;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

public class UndertowDestination extends AbstractHTTPDestination {

    public UndertowDestination(EndpointInfo endpointInfo, Bus bus, DestinationRegistry destinationRegistry) throws IOException {
        super(bus, destinationRegistry, endpointInfo, getAddressValue(endpointInfo, true).getAddress(), true);
    }

    @Override
    protected Logger getLogger() {
        return null;
    }

    @Override
    public EndpointReferenceType getAddress() {
        return super.getAddress();
    }
}
