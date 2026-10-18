package io.quarkus.it.infinispan.client;

import java.math.BigDecimal;
import java.util.Set;

import org.infinispan.api.annotations.indexing.Indexed;
import org.infinispan.api.annotations.indexing.Keyword;
import org.infinispan.api.annotations.indexing.Text;
import org.infinispan.protostream.annotations.Proto;

@Proto
@Indexed
public record Book(@Text String title,
        @Keyword(projectable = true, sortable = true, normalizer = "lowercase", indexNullAs = "unnamed", norms = false) String description,
        int publicationYear, Set<Author> authors, Type bookType, BigDecimal price) {
}
