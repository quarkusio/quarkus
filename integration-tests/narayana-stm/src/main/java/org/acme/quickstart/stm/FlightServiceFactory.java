package org.acme.quickstart.stm;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.stm.Container;

@ApplicationScoped
class FlightServiceFactory {
    private FlightService flightServiceProxy;

    private void initFlightServiceFactory() {
        Container<FlightService> container = new Container<>();
        flightServiceProxy = container.create(new FlightServiceImpl());
    }

    FlightService getInstance() {
        if (flightServiceProxy == null) {
            initFlightServiceFactory();
        }
        return flightServiceProxy;
    }
}
