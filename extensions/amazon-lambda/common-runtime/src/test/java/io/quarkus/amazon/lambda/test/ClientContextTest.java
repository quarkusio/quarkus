package io.quarkus.amazon.lambda.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.amazonaws.services.lambda.runtime.ClientContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import io.quarkus.amazon.lambda.runtime.ClientContextImpl;

public class ClientContextTest {

    static final String ctx = "{\n" +
            "                        \"client\": {\n" +
            "                                    \"client_id\":\"<client_id>\",\n" +
            "                                    \"app_title\":\"<app_title>\",\n" +
            "                                    \"app_version_name\":\"<app_version_name>\",\n" +
            "                                    \"app_version_code\":\"<app_version_code>\",\n" +
            "                                    \"app_package_name\":\"<app_package_name>\"\n" +
            "                                  },\n" +
            "                        \n" +
            "                        \"custom\": { \"hello\": \"world\"},\n" +
            "                        \n" +
            "                        \"env\":{\n" +
            "                                \"platform\":\"<platform>\",\n" +
            "                                \"model\":\"<model>\",\n" +
            "                                \"make\":\"<make>\",\n" +
            "                                \"platform_version\":\"<platform_version>\",\n" +
            "                                \"locale\":\"<locale>\"\n" +
            "                              },\n" +
            "\n" +
            "                        \"services\": {                        \n" +
            "                                      \"mobile_analytics\": {\n" +
            "                                                            \"app_id\":\"<mobile_analytics_app_id>\"\n" +
            "                                                          }\n" +
            "                                    }\n" +
            "                       }";

    @Test
    public void testContextMarshalling() throws Exception {
        ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);

        ObjectReader reader = mapper.readerFor(ClientContextImpl.class);

        ClientContext clientContext = reader.readValue(ctx);
        Assertions.assertNotNull(clientContext.getClient());
        Assertions.assertNotNull(clientContext.getCustom());
        Assertions.assertNotNull(clientContext.getEnvironment());

        Assertions.assertEquals("<client_id>", clientContext.getClient().getInstallationId());
        Assertions.assertEquals("<app_title>", clientContext.getClient().getAppTitle());
        Assertions.assertEquals("<app_version_name>", clientContext.getClient().getAppVersionName());
        Assertions.assertEquals("<app_version_code>", clientContext.getClient().getAppVersionCode());
        Assertions.assertEquals("<app_package_name>", clientContext.getClient().getAppPackageName());

        Assertions.assertEquals("world", clientContext.getCustom().get("hello"));
        Assertions.assertEquals("<platform>", clientContext.getEnvironment().get("platform"));

    }
}
