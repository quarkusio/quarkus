package io.quarkus.bootstrap.classloading;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;

import org.jboss.logging.Logger;

public class DriverRemover implements Runnable {

    private static final Logger log = Logger.getLogger(DriverRemover.class);

    final ClassLoader goingAwayCl;

    public DriverRemover(ClassLoader goingAwayCl) {
        this.goingAwayCl = goingAwayCl;
    }

    @Override
    public void run() {
        // Run the removal twice. DriverManager.getDrivers() calls isDriverAllowed()
        // which does Class.forName(driverName, true, callerCL) -- the 'true' triggers
        // class initialization. If a Driver class was defined by this classloader but
        // not yet initialized, this causes its static initializer to run, which calls
        // DriverManager.registerDriver() -- registering a new driver instance during
        // the iteration. CopyOnWriteArrayList snapshot semantics mean the first pass
        // doesn't see this newly registered driver. The second pass picks it up.
        deregisterDrivers();
        deregisterDrivers();
    }

    private void deregisterDrivers() {
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver driver = drivers.nextElement();
            try {
                if (driver.getClass().getClassLoader() == goingAwayCl) {
                    DriverManager.deregisterDriver(driver);
                }
            } catch (SQLException t) {
                log.error("Failed to deregister driver", t);
            }
        }
    }
}
