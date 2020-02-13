package io.quarkus.it.jpa.h2;

import javax.persistence.Entity;

@Entity
public class CompanyCustomer extends Customer {
    String companyname;
}
