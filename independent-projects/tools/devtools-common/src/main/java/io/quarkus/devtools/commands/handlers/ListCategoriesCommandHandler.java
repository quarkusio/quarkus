package io.quarkus.devtools.commands.handlers;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import io.quarkus.devtools.commands.ListCategories;
import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.commands.data.QuarkusCommandInvocation;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.registry.catalog.Category;

/**
 * Instances of this class are thread-safe. It lists extension categories according to the options passed in as properties of
 * {@link QuarkusCommandInvocation}
 */
public class ListCategoriesCommandHandler implements QuarkusCommandHandler {

    private static final String ID_FORMAT = "%-20s";
    private static final String CONCISE_FORMAT = "%-50s %-50s";
    private static final String FULL_FORMAT = "%-30s %-20s %s";

    @Override
    public QuarkusCommandOutcome execute(QuarkusCommandInvocation invocation) throws QuarkusCommandException {

        final MessageWriter log = invocation.log();
        final boolean batchMode = invocation.getValue(ListCategories.BATCH_MODE, false);
        final String format = invocation.getValue(ListCategories.FORMAT, "");

        final Collection<Category> categories = invocation.getExtensionsCatalog().getCategories();

        if (!batchMode && !format.equalsIgnoreCase("object")) {
            log.info("Available Quarkus extension categories: ");
            log.info("");
        }

        BiConsumer<MessageWriter, Category> formatter;
        switch (format.toLowerCase()) {
            case "object":
                formatter = null;
                break;
            case "full":
                log.info(String.format(FULL_FORMAT, "Category", "CategoryId", "Description"));
                formatter = this::fullFormatter;
                break;
            case "concise":
                formatter = this::conciseFormatter;
                break;
            case "id":
            default:
                formatter = this::idFormatter;
                break;
        }

        if (formatter != null) {
            categories.stream()
                    .sorted(Comparator.comparing(Category::getName))
                    .forEach(c -> formatter.accept(log, c));
        } else {
            List<Category> sortedCategories = categories.stream()
                    .sorted(Comparator.comparing(Category::getName))
                    .collect(Collectors.toList());
            return QuarkusCommandOutcome.success(sortedCategories);
        }
        return QuarkusCommandOutcome.success();
    }

    private void idFormatter(MessageWriter writer, Category category) {
        writer.info(String.format(ID_FORMAT, category.getId()));
    }

    private void conciseFormatter(MessageWriter writer, Category category) {
        writer.info(String.format(CONCISE_FORMAT, category.getName(), category.getId()));
    }

    private void fullFormatter(MessageWriter writer, Category category) {
        writer.info(String.format(FULL_FORMAT, category.getName(), category.getId(), category.getDescription()));
    }

}
