package com.example;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.protean.Hibernate;

public class Main {

	static {
		Hibernate.featureInit();
	}

	public static void main(String[] args) {
		EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory( "templatePU" );
		System.out.println( "Hibernate EntityManagerFactory: booted" );

		doStuffWithHibernate( entityManagerFactory );

		entityManagerFactory.close();
		System.out.println( "Hibernate EntityManagerFactory: shut down" );

//		try {
//			int read = System.in.read();
//		}
//		catch (IOException e) {
//			e.printStackTrace();
//		}
	}

	private static void doStuffWithHibernate(EntityManagerFactory entityManagerFactory) {
		EntityManager em = entityManagerFactory.createEntityManager();
		EntityTransaction transaction = em.getTransaction();
		transaction.begin();

		persistNewPerson( em );

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
			sb.append( p.id ).append( ":\t" ).append( p.name ).append( "\n\t" );
		}
		sb.append( "\nList complete.\n" );
		System.out.print( sb );
	}

	private static void persistNewPerson(EntityManager entityManager) {
		UUID uuid = UUID.randomUUID();
		Person person = new Person();
		person.name = uuid.toString();
		entityManager.persist( person );
	}

}
