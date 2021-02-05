package io.quarkus.bootstrap.classloading;

import java.lang.reflect.Field;
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

        // this really sucks because it triggers an illegal access warning, but there is no other way
        // to force the DriverManager to re-initialize drivers
        try {
            Field[] declaredFields = DriverManager.class.getDeclaredFields();
            // go through the fields instead of accessing it immediately because in Java 8 this field does not exist
            for (Field field : declaredFields) {
                if (field.getName().equals("driversInitialized")) {
                    field.setAccessible(true);
                    field.set(null, false);
                    break;
                }
            }
        } catch (IllegalAccessException t) {
            log.debug("Failed to clear initialization state of drivers", t);
        }

    }
}
