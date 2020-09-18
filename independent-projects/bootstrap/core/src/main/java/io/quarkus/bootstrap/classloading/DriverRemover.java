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
