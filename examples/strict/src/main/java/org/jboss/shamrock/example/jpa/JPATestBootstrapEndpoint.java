/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.shamrock.example.jpa;

import java.io.IOException;
import java.io.PrintWriter;
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

import org.jboss.shamrock.examples.common.Clown;

/**
 * Various tests for the JPA integration.
 * WARNING: these tests will ONLY pass in Substrate, as it also verifies reflection non-functionality.
 */
@WebServlet(name = "JPATestBootstrapEndpoint", urlPatterns = "/jpa/testbootstrap")
public class JPATestBootstrapEndpoint extends HttpServlet {

    @Inject
    EntityManagerFactory entityManagerFactory;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            testStoreLoadOnJPA();
        }
        catch (Exception e) {
            e.printStackTrace();
           reportException("Oops, shit happened, No boot for you!", e, resp);
        }


        resp.getWriter().write("OK");
    }


    public void testStoreLoadOnJPA() throws Exception {
        doStuffWithHibernate( entityManagerFactory );
        System.out.println( "Hibernate EntityManagerFactory: shut down" );

    }

    private static void doStuffWithHibernate(EntityManagerFactory entityManagerFactory) {
        EntityManager em = entityManagerFactory.createEntityManager();
        EntityTransaction transaction = em.getTransaction();
        transaction.begin();

        persistNewPerson( em );
        persistNewClown( em );

        listExistingPersons( em );

        transaction.commit();
        em.close();
    }

    private static void listExistingPersons(EntityManager em) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<Person> cq = cb.createQuery( Person.class );
        Root<Person> from = cq.from( Person.class );
        cq.select( from );
        TypedQuery<Person> q = em.createQuery( cq );
        List<Person> allpersons = q.getResultList();
        StringBuilder sb = new StringBuilder( "list of stored Person names:\n\t" );
        for ( Person p : allpersons ) {
            p.describeFully( sb );
            sb.append( "\n\t" );
        }
        sb.append( "\nList complete.\n" );
        System.out.print( sb );
    }

    private static void persistNewPerson(EntityManager entityManager) {
        Person person = new Person();
        person.setName( randomName() );
        person.setAddress( new SequencedAddress( "Street " + randomName() ) );
        entityManager.persist( person );
    }

    private static void persistNewClown(EntityManager entityManager) {
        Clown clown = new Clown();
        clown.setName("Bozo");
        entityManager.persist( clown );
    }

    private static String randomName() {
        return UUID.randomUUID().toString();
    }

    private void reportException(final Exception e, final HttpServletResponse resp) throws IOException {
        reportException(null, e, resp);
    }

    private void reportException(String errorMessage, final Exception e, final HttpServletResponse resp) throws IOException {
        final PrintWriter writer = resp.getWriter();
        if ( errorMessage != null ) {
            writer.write(errorMessage);
            writer.write(" ");
        }
        writer.write(e.toString());
        writer.append("\n\t");
        e.printStackTrace(writer);
        writer.append("\n\t");
    }

}
