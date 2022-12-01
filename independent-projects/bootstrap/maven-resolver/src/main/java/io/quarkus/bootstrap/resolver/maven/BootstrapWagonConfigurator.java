package io.quarkus.bootstrap.resolver.maven;

import org.apache.maven.wagon.Wagon;
import org.codehaus.plexus.component.configurator.converters.composite.ObjectWithFieldsConverter;
import org.codehaus.plexus.component.configurator.converters.lookup.ConverterLookup;
import org.codehaus.plexus.component.configurator.converters.lookup.DefaultConverterLookup;
import org.codehaus.plexus.component.configurator.expression.DefaultExpressionEvaluator;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.aether.transport.wagon.WagonConfigurator;

public class BootstrapWagonConfigurator implements WagonConfigurator {

    private static final ExpressionEvaluator DEFAULT_EXPRESSION_EVALUATOR = new DefaultExpressionEvaluator();

    protected ConverterLookup converterLookup = new DefaultConverterLookup();

    @Override
    public void configure(Wagon wagon, Object configuration) throws Exception {
        PlexusConfiguration config;
        if (configuration instanceof PlexusConfiguration) {
            config = (PlexusConfiguration) configuration;
        } else if (configuration instanceof Xpp3Dom) {
            config = new XmlPlexusConfiguration((Xpp3Dom) configuration);
        } else if (configuration == null) {
            return;
        } else {
            throw new IllegalArgumentException("unexpected configuration type: "
                    + configuration.getClass().getName());
        }
        final ObjectWithFieldsConverter converter = new ObjectWithFieldsConverter();
        converter.processConfiguration(converterLookup, wagon, null, config, DEFAULT_EXPRESSION_EVALUATOR, null);
    }
}
