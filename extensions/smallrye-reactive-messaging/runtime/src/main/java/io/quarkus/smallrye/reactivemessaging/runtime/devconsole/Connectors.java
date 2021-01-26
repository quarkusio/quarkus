package io.quarkus.smallrye.reactivemessaging.runtime.devconsole;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.reactive.messaging.spi.Connector;
import org.eclipse.microprofile.reactive.messaging.spi.IncomingConnectorFactory;
import org.eclipse.microprofile.reactive.messaging.spi.OutgoingConnectorFactory;

import io.quarkus.arc.ClientProxy;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.smallrye.reactivemessaging.runtime.devconsole.DevReactiveMessagingInfos.Component;
import io.quarkus.smallrye.reactivemessaging.runtime.devconsole.DevReactiveMessagingInfos.ComponentType;

// we use a separate component due to weird class loading issues that occur when accessing config properties 
@Singleton
public class Connectors {

    final Map<String, Component> incomingConnectors;
    final Map<String, Component> outgoingConnectors;

    public Connectors() {
        incomingConnectors = new HashMap<>();
        outgoingConnectors = new HashMap<>();
    }

    void collect(@Observes StartupEvent event, @Any Instance<IncomingConnectorFactory> incoming,
            @Any Instance<OutgoingConnectorFactory> outgoing, Config config) {

        Set<String> connectors = new HashSet<>();
        for (IncomingConnectorFactory incomingConnector : incoming) {
            if (incomingConnector instanceof ClientProxy) {
                incomingConnector = (IncomingConnectorFactory) ((ClientProxy) incomingConnector)
                        .arc_contextualInstance();
            }
            Connector connector = incomingConnector.getClass().getAnnotation(Connector.class);
            connectors.add(connector.value());
        }
        for (OutgoingConnectorFactory outgoingConnector : outgoing) {
            if (outgoingConnector instanceof ClientProxy) {
                outgoingConnector = (OutgoingConnectorFactory) ((ClientProxy) outgoingConnector)
                        .arc_contextualInstance();
            }
            Connector connector = outgoingConnector.getClass().getAnnotation(Connector.class);
            connectors.add(connector.value());

        }

        String outgoingPrefix = "mp.messaging.outgoing.";
        String incomingPrefix = "mp.messaging.incoming.";
        String connectorSuffix = ".connector";

        List<String> sourceConnectorsProperties = new ArrayList<>();
        List<String> sinkConnectorsProperties = new ArrayList<>();

        for (String propertyName : config.getPropertyNames()) {
            if (propertyName.startsWith(outgoingPrefix) && propertyName.endsWith(connectorSuffix)) {
                sinkConnectorsProperties.add(propertyName);
            } else if (propertyName.startsWith(incomingPrefix) && propertyName.endsWith(connectorSuffix)) {
                sourceConnectorsProperties.add(propertyName);
            }
        }

        for (String sink : sinkConnectorsProperties) {
            String connector = config.getValue(sink, String.class);
            String channel = sink.substring(outgoingPrefix.length(), sink.length() - connectorSuffix.length());
            StringBuilder desc = new StringBuilder();
            desc.append(connector);
            desc.append("<ul>");
            desc.append("<li>Config property: ");
            desc.append(DevReactiveMessagingInfos.asCode(sink.substring(0, sink.length() - connectorSuffix.length())));
            desc.append("</li>");
            for (Entry<String, String> entry : getProperties(config, connectorSuffix, sink).entrySet()) {
                desc.append("<li>");
                desc.append(entry.getKey());
                desc.append("=");
                desc.append(entry.getValue());
                desc.append("</li>");
            }
            desc.append("</ul>");
            incomingConnectors.put(channel,
                    new Component(ComponentType.CONNECTOR, desc.toString()));
        }

        for (String source : sourceConnectorsProperties) {
            String connector = config.getValue(source, String.class);
            String channel = source.substring(incomingPrefix.length(), source.length() - connectorSuffix.length());
            StringBuilder desc = new StringBuilder();
            desc.append(connector);
            desc.append("<ul>");
            desc.append("<li>Config property: ");
            desc.append(DevReactiveMessagingInfos.asCode(source.substring(0, source.length() - connectorSuffix.length())));
            desc.append("</li>");
            for (Entry<String, String> entry : getProperties(config, connectorSuffix, source).entrySet()) {
                desc.append("<li>");
                desc.append(entry.getKey());
                desc.append("=");
                desc.append(entry.getValue());
                desc.append("</li>");
            }
            desc.append("</ul>");
            outgoingConnectors.put(channel,
                    new Component(ComponentType.CONNECTOR, desc.toString()));
        }
    }

    private Map<String, String> getProperties(Config config, String connectorSuffix, String connectorPropertyName) {
        Map<String, String> properties = new HashMap<>();
        String prefix = connectorPropertyName.substring(0, connectorPropertyName.length() - connectorSuffix.length());
        for (String propertyName : config.getPropertyNames()) {
            if (propertyName.startsWith(prefix) && !propertyName.equals(connectorPropertyName)) {
                properties.put(propertyName.substring(prefix.length() + 1, propertyName.length()),
                        config.getValue(propertyName, String.class));
            }
        }
        return properties;
    }

}
