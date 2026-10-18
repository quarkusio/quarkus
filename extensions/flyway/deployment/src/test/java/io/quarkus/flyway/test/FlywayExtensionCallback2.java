package io.quarkus.flyway.test;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;

import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;

public class FlywayExtensionCallback2 implements Callback {

    public static List<Event> DEFAULT_EVENTS = Arrays.asList(Event.AFTER_MIGRATE);

    @Override
    public boolean supports(Event event, Context context) {
        return DEFAULT_EVENTS.contains(event);
    }

    @Override
    public boolean canHandleInTransaction(Event event, Context context) {
        return true;
    }

    @Override
    public void handle(Event event, Context context) {
        try (Statement stmt = context.getConnection().createStatement()) {
            stmt.executeUpdate("INSERT INTO quarked_callback(name) VALUES('" + event.getId() + "')");
        } catch (SQLException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @Override
    public String getCallbackName() {
        return "Quarked Flyway Callback 2";
    }
}
