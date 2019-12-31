package io.quarkus.cxf.runtime.transport;

import java.io.IOException;

import org.apache.cxf.Bus;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.cxf.transport.http.DestinationRegistry;
import org.apache.cxf.transport.http.HttpDestinationFactory;

public class UndertowDestinationFactory implements HttpDestinationFactory {
    @Override
    public AbstractHTTPDestination createDestination(EndpointInfo endpointInfo, Bus bus,
            DestinationRegistry destinationRegistry) throws IOException {
        return new UndertowDestination(endpointInfo, bus, destinationRegistry);
    }
}
