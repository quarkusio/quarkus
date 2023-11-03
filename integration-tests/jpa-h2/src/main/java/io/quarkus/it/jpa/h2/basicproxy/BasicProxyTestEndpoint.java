package io.quarkus.it.jpa.h2.basicproxy;

import java.io.IOException;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;

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
