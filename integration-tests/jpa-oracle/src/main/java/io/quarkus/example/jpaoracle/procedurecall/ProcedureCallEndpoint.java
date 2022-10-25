package io.quarkus.example.jpaoracle.procedurecall;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;

import io.agroal.api.AgroalDataSource;
import io.quarkus.arc.Arc;
import io.quarkus.runtime.StartupEvent;

@WebServlet("/jpa-oracle/procedure-call/")
public class ProcedureCallEndpoint extends HttpServlet {

    private static final String PROCEDURE_NAME = "myproc";

    @Inject
    EntityManager em;

    void installStoredProcedure(@Observes StartupEvent ev) throws SQLException {
        AgroalDataSource dataSource = Arc.container().instance(AgroalDataSource.class).get();
        try (Connection conn = dataSource.getConnection()) {
            try (Statement st = conn.createStatement()) {
                st.execute("CREATE OR REPLACE PROCEDURE " + PROCEDURE_NAME
                        + " (p_pattern IN VARCHAR2, p_cur OUT SYS_REFCURSOR)\n"
                        + "AS\n"
                        + "BEGIN\n"
                        + "   OPEN p_cur FOR SELECT name FROM " + ProcedureCallEntity.NAME + " WHERE name LIKE p_pattern;\n"
                        + "END " + PROCEDURE_NAME + ";");
            }
        }
    }

    @Transactional
    void initData(@Observes StartupEvent ev) {
        persistEntity("prefix#1");
        persistEntity("other#1");
        persistEntity("prefix#2");
        persistEntity("other#2");
    }

    private void persistEntity(String name) {
        ProcedureCallEntity entity = new ProcedureCallEntity();
        entity.setName(name);
        em.persist(entity);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String pattern = req.getParameter("pattern");
            StoredProcedureQuery storedProcedure = em.createStoredProcedureQuery(PROCEDURE_NAME);
            storedProcedure.registerStoredProcedureParameter("p_pattern", String.class, ParameterMode.IN);
            storedProcedure.registerStoredProcedureParameter("p_cur", Object.class, ParameterMode.REF_CURSOR);
            storedProcedure.setParameter("p_pattern", pattern);
            storedProcedure.execute();

            List<String> result = new ArrayList<>();
            ResultSet resultSet = (ResultSet) storedProcedure.getOutputParameterValue("p_cur");
            while (resultSet.next()) {
                result.add(resultSet.getString(1));
            }
            resp.getWriter().write(String.join("\n", result));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
