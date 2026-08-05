package io.quarkus.it.audit;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.hibernate.SessionFactory;
import org.hibernate.audit.AuditLogFactory;

import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.runtime.StartupEvent;

@Path("/audit")
public class AuditResource {

    @Inject
    @PersistenceUnit("audit")
    SessionFactory sessionFactory;

    @Transactional
    public void startup(@Observes StartupEvent event) {
        sessionFactory.inTransaction(session -> {
            var order = new AuditedOrder();
            order.setId(1L);
            order.setDescription("Initial Order");
            order.setQuantity(10);
            session.persist(order);
        });

        sessionFactory.inTransaction(session -> {
            var order = session.find(AuditedOrder.class, 1L);
            order.setDescription("Updated Order");
            order.setQuantity(20);
        });
    }

    @GET
    @Path("/current/{id}")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public String getCurrentOrder(@PathParam("id") long id) {
        var order = sessionFactory.fromSession(
                session -> session.find(AuditedOrder.class, id));
        return order != null ? order.getDescription() : "not found";
    }

    @GET
    @Path("/history/{id}")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public String getHistoricalOrder(@PathParam("id") long id) {
        return sessionFactory.fromTransaction(session -> {
            var auditLog = AuditLogFactory.create(session);
            var changesetIds = auditLog.getChangesets(AuditedOrder.class, id);
            if (changesetIds.isEmpty()) {
                return "no history";
            }
            try (var s = sessionFactory.withOptions()
                    .atChangeset(changesetIds.get(0)).open()) {
                var order = s.find(AuditedOrder.class, id);
                return order != null ? order.getDescription() : "not found at changeset";
            }
        });
    }

    @GET
    @Path("/changesets/{id}")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public String getChangesetCount(@PathParam("id") long id) {
        return sessionFactory.fromTransaction(session -> {
            var auditLog = AuditLogFactory.create(session);
            var changesetIds = auditLog.getChangesets(AuditedOrder.class, id);
            return String.valueOf(changesetIds.size());
        });
    }
}
