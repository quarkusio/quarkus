package io.quarkus.hibernate.orm.envers;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.resource.spi.IllegalStateException;
import jakarta.transaction.UserTransaction;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.Path;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;

@Path("/envers-store-data-at-delete")
@ApplicationScoped
public class EnversTestStoreDataAtDeleteResource {
    private static final String NAME = "deleted";

    @Inject
    EntityManager em;

    @Inject
    UserTransaction transaction;

    @DELETE
    public String delete() {
        try {
            transaction.begin();
            MyAuditedEntity entity = new MyAuditedEntity();
            entity.setName(NAME);
            em.persist(entity);
            transaction.commit();

            transaction.begin();
            em.remove(em.find(MyAuditedEntity.class, entity.getId()));
            em.flush();
            transaction.commit();

            AuditReader auditReader = AuditReaderFactory.get(em);
            List<Number> revisions = auditReader.getRevisions(MyAuditedEntity.class, entity.getId());
            if (revisions.size() != 2) {
                throw new IllegalStateException(String.format("found {} revisions", revisions.size()));
            }

            for (Number revision : revisions) {
                System.out.println(revision);
                MyAuditedEntity revEntity = auditReader.find(MyAuditedEntity.class, MyAuditedEntity.class.getName(),
                        entity.getId(), revision, true);
                if (revEntity == null) {
                    throw new IllegalStateException("failed to find delete revision");
                }
                if (!NAME.equals(revEntity.getName())) {
                    throw new IllegalStateException("revision listener failed to persist data on delete");
                }
            }

            return "OK";
        } catch (Exception exception) {
            return exception.getMessage();
        }
    }
}
