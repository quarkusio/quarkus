package io.quarkus.it.jpa.mapping.xml.modern.app.xmlmappingonly.attributeconverter;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.UserTransaction;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.hibernate.Session;

import io.quarkus.hibernate.orm.PersistenceUnit;

@Path("/xml-mapping-only/attribute-converter")
@ApplicationScoped
public class XmlMappingOnlyAttributeConverterResource {

    @Inject
    @PersistenceUnit("xml-mapping-only-attribute-converter")
    Session session;

    @Inject
    UserTransaction transaction;

    @GET
    @Path("/auto-apply/")
    @Produces(MediaType.TEXT_PLAIN)
    public String autoApply() throws Exception {
        try {
            transaction.begin();
            MyEntity entity = new MyEntity();
            entity.myData = new MyData("foo");
            session.persist(entity);
            transaction.commit();

            transaction.begin();
            assertThat(session.createNativeQuery("select myData from myentity").getResultList())
                    .containsExactly("foo");
            transaction.commit();
        } catch (Exception | AssertionError e) {
            try {
                transaction.rollback();
            } catch (Throwable t) {
                e.addSuppressed(t);
            }
            throw e;
        }

        return "OK";
    }
}
