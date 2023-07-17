package org.acme.gradle.multi.dao;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@ApplicationScoped
public class ProductService {
	
	@Inject
	EntityManager em;
	
	public List<Product> getProducts() {
		return em.createNamedQuery("Product.findAll", Product.class).getResultList();
	}

}
