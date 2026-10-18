package io.quarkus.elytron.security.jdbc.it;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import io.agroal.api.AgroalDataSource;
import io.quarkus.security.PermissionChecker;

@ApplicationScoped
public class JdbcPermissionChecker {

    @Inject
    AgroalDataSource defaultDataSource;

    @Transactional
    @PermissionChecker("admin-role-in-db")
    boolean hasAdminRole(String usernameHeader) {
        String username = switch (usernameHeader) {
            case "admin" -> "admin";
            case "user" -> "user";
            default -> throw new IllegalArgumentException("Invalid username: " + usernameHeader);
        };
        try (Connection connection = defaultDataSource.getConnection(); Statement stat = connection.createStatement()) {
            try (ResultSet roleQuery = stat
                    .executeQuery("select u.role from test_user u where u.username='" + username + "'")) {
                if (!roleQuery.first()) {
                    throw new IllegalStateException("Username '%s' not in the 'test_user' table".formatted(username));
                }
                var role = roleQuery.getString(1);
                return "admin".equals(role);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
