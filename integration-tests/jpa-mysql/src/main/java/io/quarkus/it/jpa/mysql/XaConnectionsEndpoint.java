package io.quarkus.it.jpa.mysql;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.AgroalConnectionFactoryConfiguration;
import io.quarkus.agroal.DataSource;

@WebServlet(name = "XaConnectionEndpoint", urlPatterns = "/jpa-mysql/testxaconnection")
public class XaConnectionsEndpoint extends HttpServlet {

    @Inject
    @DataSource("samebutxa")
    AgroalDataSource xaDatasource;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        // Test 1#
        // Verify that the connection can be obtained
        try (Connection connection = xaDatasource.getConnection()) {
            //The main goal is to check that the connection could be opened
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        // Test 2#
        // Check it's of the expected configuration
        AgroalConnectionFactoryConfiguration cfg = xaDatasource.getConfiguration().connectionPoolConfiguration()
                .connectionFactoryConfiguration();
        Class<?> connectionProviderClass = cfg.connectionProviderClass();
        if (connectionProviderClass.equals(com.mysql.cj.jdbc.MysqlXADataSource.class)) {
            resp.getWriter().write("OK");
        } else {
            resp.getWriter().write("Unexpected Driver class: " + connectionProviderClass.getName());
        }

    }

}
