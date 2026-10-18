package io.quarkus.example.jpaoracle;

import java.sql.SQLException;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.logging.Logger;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;

@Path("/jpa-oracle/testldap")
@Produces(MediaType.TEXT_PLAIN)
public class LdapUrlTestEndpoint {

    private final Logger LOG = Logger.getLogger(LdapUrlTestEndpoint.class.getName());

    @Inject
    @DataSource("ldap")
    AgroalDataSource ds;

    @GET
    public String test() throws SQLException {
        try {
            ds.getConnection().close();
        } catch (SQLException e) {
            LOG.info("received exception: " + e);
            if (e.toString().contains("java.net.UnknownHostException: oid")) {
                return "OK";
            }
            throw e;
        }

        return "KO: did not get expected exception";
    }

}
