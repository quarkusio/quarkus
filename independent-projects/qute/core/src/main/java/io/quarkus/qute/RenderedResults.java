package io.quarkus.qute;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

import org.jboss.logging.Logger;

import io.quarkus.qute.RenderedResults.RenderedResult;
import io.quarkus.qute.Template.Fragment;

/**
 * This helper class can be used to collect rendering results and assert the results in a test.
 * <p>
 * You can obtain the results for a specific template with {@link #getResults(String)}. This class also implements iterable -
 * the entry key is the template id and the value is a copy of the list of all results. Finally, it's also possible to filter
 * the results with {@link #setFilter(BiPredicate)} and remove some results with {@link #remove(Predicate)}.
 *
 * @see ResultsCollectingTemplateInstance
 */
public class RenderedResults implements BiConsumer<TemplateInstance, String>, Iterable<Entry<String, List<RenderedResult>>> {

    private static final Logger LOG = Logger.getLogger(RenderedResults.class);

    private final ConcurrentMap<String, List<RenderedResult>> idToResults;

    private final Function<TemplateInstance, String> idExtractor;

    private final AtomicReference<BiPredicate<TemplateInstance, RenderedResult>> filter;

    public RenderedResults() {
        this(new Function<TemplateInstance, String>() {

            @Override
            public String apply(TemplateInstance templateInstance) {
                try {
                    Template template = templateInstance.getTemplate();
                    if (template.isFragment()) {
                        return ((Fragment) template).getOriginalTemplate().getId() + "$" + template.getId();
                    }
                    return templateInstance.getTemplate().getId();
                } catch (Exception e) {
                    return null;
                }
            }
        });
    }

    public RenderedResults(Function<TemplateInstance, String> keyExtractor) {
        this.idToResults = new ConcurrentHashMap<>();
        this.idExtractor = Objects.requireNonNull(keyExtractor);
        this.filter = new AtomicReference<>();
    }

    @Override
    public void accept(TemplateInstance templateInstance, String result) {
        String id = idExtractor.apply(templateInstance);
        if (id == null) {
            LOG.warnf("Unable to extract the id from the template instance: %s", templateInstance.toString());
            return;
        }
        List<RenderedResult> results = idToResults.computeIfAbsent(id, e -> Collections.synchronizedList(new ArrayList<>()));
        RenderedResult r = new RenderedResult(id, result, LocalDateTime.now());
        BiPredicate<TemplateInstance, RenderedResult> filter = this.filter.get();
        if (filter == null || filter.test(templateInstance, r)) {
            results.add(r);
        }
    }

    /**
     * The returned list is an immutable copy of the results for the given template id.
     * <p>
     * For fragments the template id is the template id of the original template followed by the {@code $} and the name of the
     * fragment.
     *
     * @param templateId
     * @return the results for the given template id
     * @see Template#getId()
     */
    public List<RenderedResult> getResults(String templateId) {
        List<RenderedResult> results = idToResults.get(templateId);
        if (results == null || results.isEmpty()) {
            return List.of();
        }
        return List.copyOf(results);
    }

    @Override
    public Iterator<Entry<String, List<RenderedResult>>> iterator() {
        return idToResults.entrySet().stream().map(e -> Map.entry(e.getKey(), List.copyOf(e.getValue()))).iterator();
    }

    /**
     * Remove all results.
     */
    public void clear() {
        idToResults.clear();
    }

    /**
     * Remove all results that match the given predicate.
     *
     * @param predicate
     */
    public void remove(Predicate<RenderedResult> predicate) {
        idToResults.values().forEach(l -> l.removeIf(predicate));
    }

    /**
     * Only results that match the given predicate are stored.
     *
     * @param filter
     */
    public void setFilter(BiPredicate<TemplateInstance, RenderedResult> filter) {
        this.filter.set(filter);
    }

    @Override
    public String toString() {
        return "RenderedResults [" + (idToResults != null ? "idToResults=" + idToResults : "") + "]";
    }

    /**
     *
     * @param id The template id
     * @param result The rendered content
     * @param timestamp The timestamp this result was created at
     */
    public record RenderedResult(String templateId, String result, LocalDateTime timestamp) {

    }

}
