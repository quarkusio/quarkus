package org.acme;

import jakarta.persistence.Id;
import jakarta.persistence.Entity;

@Entity
public class MyEntity {
	public String field;
	@Id
	public String id;
}
