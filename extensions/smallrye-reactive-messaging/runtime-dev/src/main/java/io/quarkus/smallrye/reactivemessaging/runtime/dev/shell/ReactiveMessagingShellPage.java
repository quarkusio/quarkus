package io.quarkus.smallrye.reactivemessaging.runtime.dev.shell;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import io.quarkus.devshell.runtime.tui.ShellExtension;
import io.quarkus.devshell.runtime.tui.pages.BaseExtensionPage;
import io.quarkus.devshell.runtime.tui.widgets.KeyValuePanel;
import io.quarkus.devshell.runtime.tui.widgets.ListView;
import io.quarkus.smallrye.reactivemessaging.runtime.dev.ui.ReactiveMessagingJsonRpcService;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Shell page for SmallRye Reactive Messaging extension.
 * Displays channels with their publishers and consumers.
 */
public class ReactiveMessagingShellPage extends BaseExtensionPage {

    @Inject
    ReactiveMessagingJsonRpcService messagingService;

    private ListView<Channel> channelList;

    // Parsed data
    private List<Channel> channels = new ArrayList<>();
    private int connectorCount = 0;

    // No-arg constructor for CDI
    public ReactiveMessagingShellPage() {
        setTabs("Info", "Channels");
        setTabArrowNavigation(true);
        this.channelList = new ListView<>(ch -> {
            StringBuilder sb = new StringBuilder();
            sb.append(ch.name);
            sb.append(" [P:").append(ch.publisherCount).append(" C:").append(ch.consumerCount).append("]");
            if (ch.hasConnector) {
                sb.append(" connector");
            }
            return sb.toString();
        });
    }

    public ReactiveMessagingShellPage(ShellExtension extension) {
        this();
        setExtension(extension);
    }

    @Override
    public void loadData() {
        loading = true;
        error = null;
        channels.clear();
        connectorCount = 0;
        redraw();

        try {
            JsonArray channelArray = messagingService.getInfo();
            loading = false;

            if (channelArray != null) {
                for (int i = 0; i < channelArray.size(); i++) {
                    JsonObject channelObj = channelArray.getJsonObject(i);
                    String name = channelObj.getString("name");

                    if (name == null) {
                        continue;
                    }

                    JsonArray publishersArray = channelObj.getJsonArray("publishers");
                    JsonArray consumersArray = channelObj.getJsonArray("consumers");

                    int publisherCount = publishersArray != null ? publishersArray.size() : 0;
                    int consumerCount = consumersArray != null ? consumersArray.size() : 0;

                    boolean hasConnector = hasConnectorIn(publishersArray) || hasConnectorIn(consumersArray);

                    Channel channel = new Channel(name, publisherCount, consumerCount, hasConnector);
                    channels.add(channel);
                    if (hasConnector) {
                        connectorCount++;
                    }
                }
            }

            channelList.setItems(channels);
            redraw();
        } catch (Exception ex) {
            loading = false;
            error = "Failed to load messaging channels: " + ex.getMessage();
            redraw();
        }
    }

    private boolean hasConnectorIn(JsonArray array) {
        if (array == null) {
            return false;
        }
        for (int i = 0; i < array.size(); i++) {
            JsonObject element = array.getJsonObject(i);
            if (element != null && element.getBoolean("isConnector", false)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void render(Frame frame) {
        Buffer buffer = frame.buffer();
        renderHeader(buffer, width);

        int row = renderTabBar(buffer, 3);

        if (loading) {
            renderLoading(buffer, row);
        } else if (error != null) {
            renderError(buffer, row);
        } else {
            switch (getCurrentTabIndex()) {
                case 0:
                    renderInfoTab(buffer, row);
                    break;
                case 1:
                    renderChannelsTab(buffer, row);
                    break;
            }
        }

        renderFooter(buffer, "");
    }

    private void renderInfoTab(Buffer buffer, int row) {
        // Count totals
        int totalPublishers = 0;
        int totalConsumers = 0;
        for (Channel ch : channels) {
            totalPublishers += ch.publisherCount;
            totalConsumers += ch.consumerCount;
        }

        KeyValuePanel panel = new KeyValuePanel("Reactive Messaging");
        panel.add("Channels", String.valueOf(channels.size()));
        if (connectorCount > 0) {
            panel.addStyled("With connectors", String.valueOf(connectorCount), Style.create().yellow());
        }
        panel.addBlank();
        panel.add("Total Publishers", String.valueOf(totalPublishers));
        panel.add("Total Consumers", String.valueOf(totalConsumers));
        panel.addBlank();

        row = panel.render(buffer, row, 2, width - 4);

        // Legend
        KeyValuePanel legend = new KeyValuePanel("Legend");
        legend.add("P", "Publishers");
        legend.add("C", "Consumers");
        row = legend.render(buffer, row, 2, width - 4);
    }

    private void renderChannelsTab(Buffer buffer, int row) {
        row++;

        if (channels.isEmpty()) {
            buffer.setString(1, row, "No channels found", Style.create().gray());
        } else {
            channelList.setVisibleRows(height - row - 4);
            channelList.setWidth(width - 4);
            channelList.render(buffer, row, 2);
        }
    }

    @Override
    public boolean handleKey(int key) {
        // Let list handle navigation when on the Channels tab
        if (getCurrentTabIndex() == 1 && channelList.handleKey(key)) {
            redraw();
            return true;
        }

        return super.handleKey(key);
    }

    @Override
    public void onResize(int width, int height) {
        super.onResize(width, height);
        channelList.setVisibleRows(height - 10);
        channelList.setWidth(width - 4);
    }

    // Data class
    private static class Channel {
        final String name;
        final int publisherCount;
        final int consumerCount;
        final boolean hasConnector;

        Channel(String name, int publisherCount, int consumerCount, boolean hasConnector) {
            this.name = name;
            this.publisherCount = publisherCount;
            this.consumerCount = consumerCount;
            this.hasConnector = hasConnector;
        }
    }
}
