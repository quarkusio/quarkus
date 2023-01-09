package io.quarkus.example.jpaoracle;

import java.io.IOException;
import java.sql.SQLException;

import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jboss.logging.Logger;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;

@WebServlet(name = "JPATestOracleLdap", urlPatterns = "/jpa-oracle/testldap")
public class LdapUrlTestEndpoint extends HttpServlet {

    private final Logger LOG = Logger.getLogger(LdapUrlTestEndpoint.class.getName());

    @Inject
    @DataSource("ldap")
    AgroalDataSource ds;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            final String output = ldap();
            resp.getWriter().write(output);

        } catch (Exception e) {
            resp.getWriter().write("An error occurred while attempting ldap operations");
        }
    }

    private String ldap() throws SQLException {

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
