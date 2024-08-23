package io.quarkus.narayana.jta;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class JdbcObjectStoreTestProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
        HashMap<String, String> props = new HashMap<>();
        props.put("quarkus.transaction-manager.object-store.type", "jdbc");
        props.put("quarkus.transaction-manager.object-store.create-table", "true");
        props.put("quarkus.transaction-manager.object-store.datasource", "test");
        props.put("quarkus.transaction-manager.enable-recovery", "true");

        props.put("quarkus.datasource.test.jdbc.url", "jdbc:h2:mem:default");

        return props;
    }
}
