package io.quarkus.it.jpa.h2.basicproxy;

import java.io.IOException;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;

import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
@WebServlet(urlPatterns = "/jpa-h2/testbasicproxy")
public class BasicProxyTestEndpoint extends HttpServlet {

    @Inject
    EntityManager entityManager;

    @Transactional
    public void setup(@Observes StartupEvent startupEvent) {
        ConcreteEntity entity = new ConcreteEntity();
        entity.id = "1";
        entity.type = "Concrete";
        entityManager.persist(entity);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        final List list = entityManager.createQuery("from ConcreteEntity").getResultList();
        if (list.size() != 1) {
            throw new RuntimeException("Expected 1 result, got " + list.size());
        }
        resp.getWriter().write("OK");
    }
}
