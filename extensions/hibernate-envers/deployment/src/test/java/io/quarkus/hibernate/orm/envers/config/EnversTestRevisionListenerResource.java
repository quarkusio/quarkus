
package io.quarkus.hibernate.orm.envers.config;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;

import io.quarkus.hibernate.orm.envers.AbstractEnversResource;
import io.quarkus.hibernate.orm.envers.MyAuditedEntity;
import io.quarkus.hibernate.orm.envers.MyListenerlessRevisionEntity;
import io.quarkus.hibernate.orm.envers.MyListenerlessRevisionListener;

@Path("/envers-revision-listener")
@ApplicationScoped
public class EnversTestRevisionListenerResource extends AbstractEnversResource {
    private static final String NAME = "test";

    @GET
    public String getRevisionListener() {
        Class<?> expectedClass = MyListenerlessRevisionListener.class;
        Class<?> listenerClass = getGlobalConfiguration().getRevisionListenerClass();
        if (listenerClass.equals(expectedClass)) {
            try {
                transaction.begin();
                MyAuditedEntity entity = new MyAuditedEntity();
                entity.setName(NAME);
                em.persist(entity);
                transaction.commit();

                AuditReader auditReader = AuditReaderFactory.get(em);
                List<Number> revisions = auditReader.getRevisions(MyAuditedEntity.class, entity.getId());
                if (revisions.size() != 1) {
                    return "Expected a single revision but was: " + revisions.size();
                }

                List results = auditReader.createQuery().forRevisionsOfEntity(MyAuditedEntity.class, false, false)
                        .getResultList();
                if (results.size() != 1) {
                    return "Expected a single revision but was: " + results.size();
                }

                Object[] values = (Object[]) results.get(0);
                String actualListenerValue = ((MyListenerlessRevisionEntity) values[1]).getListenerValue();
                String expectedListenerValue = MyListenerlessRevisionListener.class.getName();
                if (!actualListenerValue.startsWith(expectedListenerValue)) {
                    return "Expected listener value to start with " + expectedListenerValue + " but was: "
                            + actualListenerValue;
                }
            } catch (Exception e) {
                return e.getMessage();
            }
            return "OK";
        }
        return "Expected listener class " + expectedClass.getName() + " but was: " + listenerClass.getName();
    }
}
