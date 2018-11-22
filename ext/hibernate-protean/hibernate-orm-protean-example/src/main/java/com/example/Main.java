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

package com.example;

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
			p.describeFully( sb );
			sb.append( "\n\t" );
		}
		sb.append( "\nList complete.\n" );
		System.out.print( sb );
	}

	private static void persistNewPerson(EntityManager entityManager) {
		Person person = new Person();
		person.setName( randomName() );
		person.setAddress( new Address( "Street " + randomName() ) );
		entityManager.persist( person );
	}

	private static String randomName() {
		return UUID.randomUUID().toString();
	}

}
