package io.quarkus.it.liquibase;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import io.quarkus.liquibase.LiquibaseFactory;
import liquibase.Liquibase;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.ChangeSetStatus;

@Path("/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class LiquibaseFunctionalityResource {

    @Inject
    LiquibaseFactory liquibaseFactory;

    @GET
    @Path("update")
    public String doUpdateAuto() {
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

}
