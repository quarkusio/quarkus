package org.acme.gradle.multi.dao;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

@ApplicationScoped
public class ProductService {
	
	@Inject
	EntityManager em;
	
	public List<Product> getProducts() {
		return em.createNamedQuery("Product.findAll", Product.class).getResultList();
	}
}
