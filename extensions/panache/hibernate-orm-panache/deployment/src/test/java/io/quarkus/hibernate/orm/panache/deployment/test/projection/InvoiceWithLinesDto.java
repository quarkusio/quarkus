package io.quarkus.hibernate.orm.panache.deployment.test.projection;

import java.util.Set;

public record InvoiceWithLinesDto(Long id, String number, Set<InvoiceLineDto> lines) {
}
