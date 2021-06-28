package io.quarkus.it.jpa.postgresql;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;

/**
 * First we run a smoke test for basic Hibernate ORM functionality,
 * then we specifically focus on supporting the PgSQLXML mapping abilities for XML types:
 * both need to work.
 */
@WebServlet(name = "JPATestBootstrapEndpoint", urlPatterns = "/jpa-withxml/testfunctionality")
public class JPAFunctionalityTestEndpoint extends HttpServlet {

    @Inject
    EntityManagerFactory entityManagerFactory;

    @Inject
    DataSource ds;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            doStuffWithHibernate(entityManagerFactory);
            doStuffWithDatasource();
        } catch (Exception e) {
            reportException("An error occurred while performing Hibernate operations", e, resp);
        }
        resp.getWriter().write("OK");
    }

    private void doStuffWithDatasource() throws SQLException, TransformerException {
        try (final Connection con = ds.getConnection()) {
            deleteXmlSchema(con);
            createXmlSchema(con);
            writeXmlObject(con);
            checkWrittenXmlObject(con);
        }
    }

    private void checkWrittenXmlObject(Connection con) throws SQLException {
        try (Statement stmt = con.createStatement()) {
            final ResultSet resultSet = stmt.executeQuery("SELECT val FROM xmltest");
            final boolean next = resultSet.next();
            if (!next) {
                throw new IllegalStateException("Stored XML element not found!");
            }
            final String storedValue = resultSet.getString(1);
            if (storedValue == null) {
                throw new IllegalStateException("Stored XML element was loaded as null!");
            }
            String expectedMatch = "<?xml version=\"1.0\" standalone=\"no\"?><root><ele>1</ele><ele>2</ele></root>";
            if (!expectedMatch.equals(storedValue)) {
                throw new IllegalStateException("Stored XML element is not matching expected value of '" + expectedMatch
                        + "', but was '" + storedValue + "'");
            }
        }
    }

    private void writeXmlObject(Connection con) throws SQLException, TransformerException {
        TransformerFactory factory = TransformerFactory.newInstance();
        final String _xmlDocument = "<root><ele>1</ele><ele>2</ele></root>";
        Transformer identityTransformer = factory.newTransformer();
        try (PreparedStatement ps = con.prepareStatement("INSERT INTO xmltest VALUES (?,?)")) {
            SQLXML xml = con.createSQLXML();
            Result result = xml.setResult(DOMResult.class);

            Source source = new StreamSource(new StringReader(_xmlDocument));
            identityTransformer.transform(source, result);

            ps.setInt(1, 1);
            ps.setSQLXML(2, xml);
            ps.executeUpdate();
        }
    }

    private void createXmlSchema(Connection con) throws SQLException {
        Statement stmt = con.createStatement();
        stmt.execute("CREATE TEMP TABLE xmltest(id int primary key, val xml)");
        stmt.close();
    }

    private void deleteXmlSchema(Connection con) {
        try {
            Statement stmt = con.createStatement();
            stmt.execute("DROP TABLE xmltest");
            stmt.close();
        } catch (SQLException throwables) {
            //ignore this one
        }
    }

    /**
     * Lists the various operations we want to test for:
     */
    private static void doStuffWithHibernate(EntityManagerFactory entityManagerFactory) {

        //Store some well known Person instances we can then test on:
        storeTestPersons(entityManagerFactory);

        //Load all persons and run some checks on the query results:
        verifyListOfExistingPersons(entityManagerFactory);

        //Try a JPA named query:
        verifyJPANamedQuery(entityManagerFactory);
    }

    private static void verifyJPANamedQuery(final EntityManagerFactory emf) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction transaction = em.getTransaction();
        transaction.begin();
        TypedQuery<Person> typedQuery = em.createNamedQuery(
                "get_person_by_name", Person.class);
        typedQuery.setParameter("name", "Quarkus");
        final Person singleResult = typedQuery.getSingleResult();

        if (!singleResult.getName().equals("Quarkus")) {
            throw new RuntimeException("Wrong result from named JPA query");
        }

        transaction.commit();
        em.close();
    }

    private static void verifyListOfExistingPersons(final EntityManagerFactory emf) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction transaction = em.getTransaction();
        transaction.begin();
        listExistingPersons(em);
        transaction.commit();
        em.close();
    }

    private static void storeTestPersons(final EntityManagerFactory emf) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction transaction = em.getTransaction();
        transaction.begin();
        persistNewPerson(em, "Gizmo");
        persistNewPerson(em, "Quarkus");
        persistNewPerson(em, "Hibernate ORM");
        transaction.commit();
        em.close();
    }

    private static void listExistingPersons(EntityManager em) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<Person> cq = cb.createQuery(Person.class);
        Root<Person> from = cq.from(Person.class);
        cq.select(from).orderBy(cb.asc(from.get("name")));
        TypedQuery<Person> q = em.createQuery(cq);
        List<Person> allpersons = q.getResultList();
        if (allpersons.size() != 3) {
            throw new RuntimeException("Incorrect number of results");
        }
        if (!allpersons.get(0).getName().equals("Gizmo")) {
            throw new RuntimeException("Incorrect order of results");
        }
        StringBuilder sb = new StringBuilder("list of stored Person names:\n\t");
        for (Person p : allpersons) {
            p.describeFully(sb);
            sb.append("\n\t");
            if (p.getStatus() != Status.LIVING) {
                throw new RuntimeException("Incorrect status " + p);
            }
        }
        sb.append("\nList complete.\n");
        System.out.print(sb);
    }

    private static void persistNewPerson(EntityManager entityManager, String name) {
        Person person = new Person();
        person.setName(name);
        person.setStatus(Status.LIVING);
        person.setAddress(new SequencedAddress("Street " + randomName()));
        entityManager.persist(person);
    }

    private static String randomName() {
        return UUID.randomUUID().toString();
    }

    private void reportException(String errorMessage, final Exception e, final HttpServletResponse resp) throws IOException {
        final PrintWriter writer = resp.getWriter();
        if (errorMessage != null) {
            writer.write(errorMessage);
            writer.write(" ");
        }
        writer.write(e.toString());
        writer.append("\n\t");
        e.printStackTrace(writer);
        writer.append("\n\t");
    }

}
