package org.acme.quickstart.stm;

import org.jboss.stm.annotations.NestedTopLevel;
import org.jboss.stm.annotations.Transactional;

@Transactional
@NestedTopLevel
public interface FlightService {
    int getNumberOfBookings();

    void makeBooking(String details);
}
