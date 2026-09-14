package io.quarkus.hibernate.orm.panache.deployment.test.projection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.panache.common.exception.PanacheQueryException;
import io.quarkus.test.QuarkusExtensionTest;

class CollectionProjectionTest {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application-test.properties", "application.properties")
                    .addClasses(Invoice.class, InvoiceLine.class, InvoiceLineDto.class, InvoiceWithLinesDto.class,
                            InvoiceNumberDto.class));

    @Test
    @Transactional
    void projectionWithCollectionFieldIsRejectedWithAClearMessage() {
        persistInvoice();

        PanacheQueryException exception = assertThrows(PanacheQueryException.class,
                () -> Invoice.findAll().project(InvoiceWithLinesDto.class).list());

        assertTrue(exception.getMessage().contains("lines"), exception.getMessage());
        assertTrue(exception.getMessage().contains(InvoiceWithLinesDto.class.getName()), exception.getMessage());
    }

    @Test
    @Transactional
    void projectionWithoutCollectionFieldWorks() {
        persistInvoice();

        InvoiceNumberDto dto = Invoice.findAll().project(InvoiceNumberDto.class).firstResult();

        assertEquals("INV-1", dto.number());
    }

    private static void persistInvoice() {
        Invoice invoice = new Invoice();
        invoice.number = "INV-1";
        invoice.persist();
        InvoiceLine line = new InvoiceLine();
        line.description = "line 1";
        line.invoice = invoice;
        line.persist();
        invoice.lines.add(line);
    }
}
