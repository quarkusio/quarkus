package io.quarkus.test.derby;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.Map;

import org.apache.derby.drda.NetworkServerControl;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class DerbyDatabaseTestResource implements QuarkusTestResourceLifecycleManager {

    static final int NUMBER_OF_PINGS = 10;
    static final int SLEEP_BETWEEN_PINGS = 500;

    @Override
    public Map<String, String> start() {
        try {
            NetworkServerControl server = new NetworkServerControl();
            server.start(new PrintWriter(System.out));
            for (int i = 1; i <= NUMBER_OF_PINGS; i++) {
                try {
                    System.out.println("[INFO] Attempt " + i + " to see if Derby Network server started");
                    server.ping();
                    break;
                } catch (Exception ex) {
                    if (i == NUMBER_OF_PINGS) {
                        System.out.println("Derby Network server failed to start");
                        ex.printStackTrace();
                        throw ex;
                    }
                    try {
                        Thread.sleep(SLEEP_BETWEEN_PINGS);
                    } catch (InterruptedException ignore) {
                    }
                }
            }
            System.out.println("[INFO] Derby database started in TCP server mode");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return Collections.emptyMap();
    }

    @Override
    public void stop() {
        try {
            NetworkServerControl server = new NetworkServerControl();
            server.shutdown();
            System.out.println("[INFO] Derby database was shut down");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
