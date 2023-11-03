package io.quarkus.agroal.test;

import static io.quarkus.agroal.test.MultipleDataSourcesTestUtil.testDataSource;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import jakarta.enterprise.inject.spi.CDI;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;

@Path("/test")
public class DevModeTestEndpoint {

    @GET
    @Path("/{dataSourceName}/{jdbcUrl}/{username}/{maxSize}")
    public String test(@PathParam("dataSourceName") String dataSourceName, @PathParam("jdbcUrl") String jdbcUrl,
            @PathParam("username") String username,
            @PathParam("maxSize") int maxSize) throws Exception {
        AgroalDataSource ds;
        if (dataSourceName.equals("default")) {
            ds = CDI.current().select(AgroalDataSource.class)
                    .get();
        } else {
            ds = CDI.current().select(AgroalDataSource.class, new DataSource.DataSourceLiteral(dataSourceName))
                    .get();
        }
        testDataSource(dataSourceName, ds, URLDecoder.decode(jdbcUrl, StandardCharsets.UTF_8.name()), username,
                maxSize);
        return "ok";
    }

}
