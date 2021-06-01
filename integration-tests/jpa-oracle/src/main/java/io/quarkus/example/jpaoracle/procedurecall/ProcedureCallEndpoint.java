package io.quarkus.example.jpaoracle.procedurecall;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.ParameterMode;
import javax.persistence.StoredProcedureQuery;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;

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
