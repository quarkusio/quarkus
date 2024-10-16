
package org.acme;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.util.UUID;

@Entity
public class MyEntity {
    @Id
    @GeneratedValue
    private UUID id;
}