package io.quarkus.it.jpa.h2;

import jakarta.persistence.Entity;

/**
 * This entity is intentionally marked as final:
 * it implies that ByteBuddy will not be allowed to extend it,
 * and generate an enhanced proxy.
 * We need to test for this to be handled gracefully.
 */
@Entity
public final class CompanyCustomer extends Customer {
    String companyname;
}
