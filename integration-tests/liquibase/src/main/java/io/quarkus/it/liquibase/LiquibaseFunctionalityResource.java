package io.quarkus.it.liquibase;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.WebApplicationException;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.liquibase.LiquibaseDataSource;
import io.quarkus.liquibase.LiquibaseFactory;
import liquibase.Liquibase;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.ChangeSetStatus;
import liquibase.command.CommandScope;

@Path("/")
public class LiquibaseFunctionalityResource {

    @Inject
    LiquibaseFactory liquibaseFactory;

    @Inject
    @LiquibaseDataSource("second")
    LiquibaseFactory liquibaseSecondFactory;

    @Inject
    @DataSource("second")
    AgroalDataSource secondDataSource;

    @GET
    @Path("update")
    public String doUpdateAuto() {
        assertCommandScopeResolvesProperly();

        try (Liquibase liquibase = liquibaseFactory.createLiquibase()) {
            liquibase.update(liquibaseFactory.createContexts(), liquibaseFactory.createLabels());
            List<ChangeSetStatus> status = liquibase.getChangeSetStatuses(liquibaseFactory.createContexts(),
                    liquibaseFactory.createLabels());
            List<ChangeSetStatus> changeSets = Objects.requireNonNull(status,
                    "ChangeSetStatus is null! Database update was not applied");

            return changeSets.stream()
                    .filter(ChangeSetStatus::getPreviouslyRan)
                    .map(ChangeSetStatus::getChangeSet)
                    .map(ChangeSet::getId)
                    .collect(Collectors.joining(","));
        } catch (Exception ex) {
            throw new WebApplicationException(ex.getMessage(), ex);
        }
    }

    @GET
    @Path("updateWithDedicatedUser")
    public String updateWithDedicatedUser() {
        try (Liquibase liquibase = liquibaseSecondFactory.createLiquibase()) {
            liquibase.update(liquibaseSecondFactory.createContexts(), liquibaseSecondFactory.createLabels());
            List<ChangeSetStatus> status = liquibase.getChangeSetStatuses(liquibaseSecondFactory.createContexts(),
                    liquibaseSecondFactory.createLabels());
            List<ChangeSetStatus> changeSets = Objects.requireNonNull(status,
                    "ChangeSetStatus is null! Database update was not applied");

            try (Connection connection = secondDataSource.getConnection()) {
                try (Statement s = connection.createStatement()) {
                    ResultSet rs = s.executeQuery("SELECT CREATEDBY FROM QUARKUS_TABLE WHERE ID = 1");
                    if (rs.next()) {
                        return rs.getString("CREATEDBY");
                    }
                    return null;
                }
            }
        } catch (Exception ex) {
            throw new WebApplicationException(ex.getMessage(), ex);
        }

    }

    private void assertCommandScopeResolvesProperly() {
        try {
            new CommandScope("dropAll");
        } catch (Exception e) {
            throw new RuntimeException("Unable to load 'dropAll' via Liquibase's CommandScope", e);
        }
    }
}
