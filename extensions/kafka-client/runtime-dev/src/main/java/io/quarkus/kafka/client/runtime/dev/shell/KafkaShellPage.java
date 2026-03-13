package io.quarkus.kafka.client.runtime.dev.shell;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import io.quarkus.devshell.runtime.tui.KeyCode;
import io.quarkus.devshell.runtime.tui.ShellExtension;
import io.quarkus.devshell.runtime.tui.pages.BaseExtensionPage;
import io.quarkus.devshell.runtime.tui.widgets.KeyValuePanel;
import io.quarkus.devshell.runtime.tui.widgets.TableView;
import io.quarkus.kafka.client.runtime.dev.ui.KafkaJsonRPCService;
import io.quarkus.kafka.client.runtime.dev.ui.model.response.KafkaMessage;
import io.quarkus.kafka.client.runtime.dev.ui.model.response.KafkaMessagePage;
import io.quarkus.kafka.client.runtime.dev.ui.model.response.KafkaTopic;

/**
 * Shell page for the Kafka extension showing Topics, Consumer Groups, ACL, and Nodes.
 */
public class KafkaShellPage extends BaseExtensionPage {

    @Inject
    KafkaJsonRPCService kafkaService;

    private enum ViewMode {
        TABS,
        MESSAGES,
        MESSAGE_DETAIL
    }

    private ViewMode viewMode = ViewMode.TABS;

    // Data holders
    private final List<TopicInfo> topics = new ArrayList<>();
    private final List<ConsumerGroupInfo> consumerGroups = new ArrayList<>();
    private final List<AclEntry> aclEntries = new ArrayList<>();
    private final List<NodeInfo> nodes = new ArrayList<>();
    private final List<MessageInfo> messages = new ArrayList<>();
    private String broker = "";
    private String clusterId = "";
    private String selectedTopicName = "";
    private MessageInfo selectedMessage = null;
    private int detailScrollOffset = 0;

    // Table views
    private TableView<TopicInfo> topicsTable;
    private TableView<ConsumerGroupInfo> consumerGroupsTable;
    private TableView<AclEntry> aclTable;
    private TableView<NodeInfo> nodesTable;
    private TableView<MessageInfo> messagesTable;

    // No-arg constructor for CDI
    public KafkaShellPage() {
        // Configure tabs via base class
        setTabs("Topics", "Consumer Groups", "Access Control List", "Nodes");
        // Arrow keys used by TableView, so do NOT enable arrow tab navigation
        setTabArrowNavigation(false);

        // Topics table
        topicsTable = new TableView<>();
        topicsTable.addColumn("Name", t -> t.name(), 20);
        topicsTable.addColumn("Topic ID", t -> truncateId(t.topicId()), 15);
        topicsTable.addColumn("Partitions", t -> String.valueOf(t.partitions()), 12);
        topicsTable.addColumn("Internal", t -> t.internal() ? "Yes" : "No", 10);
        topicsTable.addColumn("Messages", t -> String.valueOf(t.messageCount()), 12);

        // Consumer groups table
        consumerGroupsTable = new TableView<>();
        consumerGroupsTable.addColumn("Name", g -> g.name(), 25);
        consumerGroupsTable.addColumn("State", g -> colorState(g.state()), 12);
        consumerGroupsTable.addColumn("Coordinator", g -> g.coordinatorHost(), 20);
        consumerGroupsTable.addColumn("Protocol", g -> g.protocol() != null ? g.protocol() : "-", 15);
        consumerGroupsTable.addColumn("Lag", g -> String.valueOf(g.lag()), 10);

        // ACL table
        aclTable = new TableView<>();
        aclTable.addColumn("Operation", a -> a.operation(), 15);
        aclTable.addColumn("Principal", a -> a.principal(), 20);
        aclTable.addColumn("Permission", a -> a.permission(), 12);
        aclTable.addColumn("Pattern", a -> a.pattern(), 20);

        // Nodes table
        nodesTable = new TableView<>();
        nodesTable.addColumn("ID", n -> n.id(), 10);
        nodesTable.addColumn("Host", n -> n.host(), 30);
        nodesTable.addColumn("Port", n -> String.valueOf(n.port()), 10);

        // Messages table
        messagesTable = new TableView<>();
        messagesTable.addColumn("Partition", m -> String.valueOf(m.partition()), 10);
        messagesTable.addColumn("Offset", m -> String.valueOf(m.offset()), 12);
        messagesTable.addColumn("Timestamp", m -> m.timestamp(), 20);
        messagesTable.addColumn("Key", m -> m.key() != null ? m.key() : "-", 15);
        messagesTable.addColumn("Value", m -> truncateValue(m.value()), 40);
    }

    public KafkaShellPage(ShellExtension extension) {
        this();
        setExtension(extension);
    }

    private String truncateValue(String value) {
        if (value == null)
            return "-";
        // Remove newlines for display
        value = value.replace("\n", " ").replace("\r", "");
        if (value.length() > 50)
            return value.substring(0, 47) + "...";
        return value;
    }

    private String truncateId(String id) {
        if (id == null)
            return "-";
        if (id.length() > 12)
            return id.substring(0, 10) + "..";
        return id;
    }

    private String colorState(String state) {
        if (state == null)
            return "-";
        return state;
    }

    @Override
    public void loadData() {
        loading = true;
        error = null;
        topics.clear();
        redraw();

        try {
            List<KafkaTopic> kafkaTopics = kafkaService.getTopics();
            loading = false;

            if (kafkaTopics != null) {
                for (KafkaTopic kt : kafkaTopics) {
                    if (kt.getName() != null) {
                        topics.add(new TopicInfo(kt.getName(), kt.getTopicId(),
                                kt.getPartitionsCount(), kt.isInternal(), kt.getNmsg()));
                    }
                }
            }

            topicsTable.setItems(topics);
            redraw();
        } catch (Exception ex) {
            loading = false;
            error = "Failed to load Kafka topics: " + ex.getMessage();
            topicsTable.setItems(topics);
            redraw();
        }
    }

    private void loadMessages(String topicName) {
        loading = true;
        error = null;
        selectedTopicName = topicName;
        messages.clear();
        redraw();

        try {
            KafkaMessagePage messagePage = kafkaService.topicMessages(topicName);
            loading = false;

            if (messagePage != null && messagePage.getMessages() != null) {
                for (KafkaMessage km : messagePage.getMessages()) {
                    String timestamp = formatTimestamp(km.getTimestamp());
                    messages.add(new MessageInfo(km.getPartition(), km.getOffset(),
                            timestamp, km.getKey(), km.getValue()));
                }
            }

            messagesTable.setItems(messages);
            viewMode = ViewMode.MESSAGES;
            redraw();
        } catch (Exception ex) {
            loading = false;
            error = "Failed to load messages: " + ex.getMessage();
            messagesTable.setItems(messages);
            redraw();
        }
    }

    private String formatTimestamp(long timestamp) {
        if (timestamp == 0)
            return "-";
        try {
            java.time.Instant instant = java.time.Instant.ofEpochMilli(timestamp);
            java.time.ZonedDateTime zdt = instant.atZone(java.time.ZoneId.systemDefault());
            return java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(zdt);
        } catch (Exception e) {
            return String.valueOf(timestamp);
        }
    }

    // Note: consumerGroups, nodes, ACL data are not loaded in the simplified direct-call version.
    // They would require additional service calls (getInfo, getAclInfo) which are not used in loadData.

    @Override
    public void render(Frame frame) {
        Buffer buffer = frame.buffer();
        renderHeader(buffer, width);

        switch (viewMode) {
            case MESSAGE_DETAIL:
                renderMessageDetailView(buffer);
                break;
            case MESSAGES:
                renderMessagesView(buffer);
                break;
            default:
                renderTabsView(buffer);
                break;
        }
    }

    private void renderTabsView(Buffer buffer) {
        int row = 3;

        // Render broker info using KeyValuePanel
        KeyValuePanel brokerPanel = new KeyValuePanel();
        brokerPanel.add("Broker", broker);
        if (!clusterId.isEmpty()) {
            brokerPanel.add("Cluster ID", clusterId);
        }
        row = brokerPanel.render(buffer, row, 2, width - 4);
        row++;

        // Render tab bar via base class
        row = renderTabBar(buffer, row);

        // Separator
        row++;

        if (loading) {
            renderLoading(buffer, row);
        } else if (error != null) {
            renderError(buffer, row);
        } else {
            renderCurrentTab(buffer, row);
        }

        // Footer
        renderFooter(buffer, "[Enter] View messages");
    }

    private void renderMessagesView(Buffer buffer) {
        int row = 3;

        // Header with topic name
        row += 2;

        // Separator
        row++;

        if (loading) {
            renderLoading(buffer, row);
        } else if (error != null) {
            renderError(buffer, row);
        } else if (messages.isEmpty()) {
            buffer.setString(1, row, "No messages found", Style.create().gray());
        } else {
            int visibleRows = height - row - 4;
            messagesTable.setVisibleRows(visibleRows);
            messagesTable.setWidth(width - 4);
            messagesTable.render(buffer, row, 2);
        }

        // Footer
        renderFooter(buffer, "[Esc] Back to topics  [Enter] View message");
    }

    private void renderMessageDetailView(Buffer buffer) {
        int row = 3;

        if (selectedMessage == null) {
            return;
        }

        // Message metadata using KeyValuePanel
        KeyValuePanel detailPanel = new KeyValuePanel("Message Details");
        detailPanel.add("Topic", selectedTopicName);
        detailPanel.add("Partition", String.valueOf(selectedMessage.partition()));
        detailPanel.add("Offset", String.valueOf(selectedMessage.offset()));
        detailPanel.add("Timestamp", selectedMessage.timestamp());
        detailPanel.add("Key", selectedMessage.key() != null ? selectedMessage.key() : "(null)");
        row = detailPanel.render(buffer, row, 2, width - 4);
        row++;

        // Separator
        row++;

        // Value header
        row++;

        // Value content with scrolling
        String value = selectedMessage.value();
        if (value == null) {
            buffer.setString(1, row, "(null)", Style.create().gray());
        } else {
            // Try to format JSON if it looks like JSON
            if (value.trim().startsWith("{") || value.trim().startsWith("[")) {
                value = formatJson(value);
            }

            // Split into lines
            List<String> lines = splitIntoLines(value, width - 4);
            int visibleLines = height - row - 3;
            int maxScroll = Math.max(0, lines.size() - visibleLines);
            detailScrollOffset = Math.min(detailScrollOffset, maxScroll);

            for (int i = 0; i < visibleLines && (i + detailScrollOffset) < lines.size(); i++) {
                buffer.setString(1, row + i, lines.get(i + detailScrollOffset), Style.EMPTY);
            }

            // Scroll indicator
            if (lines.size() > visibleLines) {
                if (detailScrollOffset > 0) {
                    buffer.setString(width - 5, row, " ^ ", Style.create().gray());
                }
                if (detailScrollOffset < maxScroll) {
                    buffer.setString(width - 5, row + visibleLines - 1, " v ", Style.create().gray());
                }
            }
        }

        // Footer
        renderFooter(buffer, "[Esc] Back to messages  [Up/Down] Scroll");
    }

    private String formatJson(String json) {
        // Simple JSON formatter
        StringBuilder sb = new StringBuilder();
        int indent = 0;
        boolean inString = false;
        boolean escaped = false;

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);

            if (escaped) {
                sb.append(c);
                escaped = false;
                continue;
            }

            if (c == '\\' && inString) {
                sb.append(c);
                escaped = true;
                continue;
            }

            if (c == '"') {
                inString = !inString;
                sb.append(c);
                continue;
            }

            if (inString) {
                sb.append(c);
                continue;
            }

            switch (c) {
                case '{':
                case '[':
                    sb.append(c);
                    sb.append('\n');
                    indent += 2;
                    sb.append(" ".repeat(indent));
                    break;
                case '}':
                case ']':
                    sb.append('\n');
                    indent = Math.max(0, indent - 2);
                    sb.append(" ".repeat(indent));
                    sb.append(c);
                    break;
                case ',':
                    sb.append(c);
                    sb.append('\n');
                    sb.append(" ".repeat(indent));
                    break;
                case ':':
                    sb.append(c);
                    sb.append(' ');
                    break;
                default:
                    if (!Character.isWhitespace(c)) {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    private List<String> splitIntoLines(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] rawLines = text.split("\n");

        for (String rawLine : rawLines) {
            if (rawLine.length() <= maxWidth) {
                lines.add(rawLine);
            } else {
                // Word wrap long lines
                int start = 0;
                while (start < rawLine.length()) {
                    int end = Math.min(start + maxWidth, rawLine.length());
                    lines.add(rawLine.substring(start, end));
                    start = end;
                }
            }
        }
        return lines;
    }

    private void renderCurrentTab(Buffer buffer, int row) {
        int visibleRows = height - row - 4;
        int tableWidth = width - 4;

        switch (getCurrentTabIndex()) {
            case 0: // Topics
                if (topics.isEmpty()) {
                    buffer.setString(1, row, "No topics found", Style.create().gray());
                } else {
                    topicsTable.setVisibleRows(visibleRows);
                    topicsTable.setWidth(tableWidth);
                    topicsTable.render(buffer, row, 2);
                }
                break;

            case 1: // Consumer Groups
                if (consumerGroups.isEmpty()) {
                    buffer.setString(1, row, "No consumer groups found", Style.create().gray());
                } else {
                    consumerGroupsTable.setVisibleRows(visibleRows);
                    consumerGroupsTable.setWidth(tableWidth);
                    consumerGroupsTable.render(buffer, row, 2);
                }
                break;

            case 2: // ACL
                if (aclEntries.isEmpty()) {
                    buffer.setString(1, row, "No ACL entries found", Style.create().gray());
                } else {
                    aclTable.setVisibleRows(visibleRows);
                    aclTable.setWidth(tableWidth);
                    aclTable.render(buffer, row, 2);
                }
                break;

            case 3: // Nodes
                if (nodes.isEmpty()) {
                    buffer.setString(1, row, "No nodes found", Style.create().gray());
                } else {
                    nodesTable.setVisibleRows(visibleRows);
                    nodesTable.setWidth(tableWidth);
                    nodesTable.render(buffer, row, 2);
                }
                break;
        }
    }

    @Override
    public boolean handleKey(int key) {
        if (loading) {
            return true;
        }

        // Handle message detail view
        if (viewMode == ViewMode.MESSAGE_DETAIL) {
            return handleMessageDetailKey(key);
        }

        // Handle messages view
        if (viewMode == ViewMode.MESSAGES) {
            return handleMessagesKey(key);
        }

        // Enter key to view messages (only on Topics tab)
        if (key == KeyCode.ENTER && getCurrentTabIndex() == 0) {
            TopicInfo selectedTopic = topicsTable.getSelectedItem();
            if (selectedTopic != null) {
                loadMessages(selectedTopic.name());
                return true;
            }
        }

        // Handle navigation in current table
        boolean handled = false;
        switch (getCurrentTabIndex()) {
            case 0: // Topics
                handled = topicsTable.handleKey(key);
                break;
            case 1: // Consumer Groups
                handled = consumerGroupsTable.handleKey(key);
                break;
            case 2: // ACL
                handled = aclTable.handleKey(key);
                break;
            case 3: // Nodes
                handled = nodesTable.handleKey(key);
                break;
        }

        if (handled) {
            redraw();
            return true;
        }

        // Delegate to base class for tab switching (Tab key, number keys) and Esc/R
        return super.handleKey(key);
    }

    private boolean handleMessagesKey(int key) {
        // Escape to go back to topics
        if (key == KeyCode.ESCAPE) {
            viewMode = ViewMode.TABS;
            error = null;
            redraw();
            return true;
        }

        // Enter to view message detail
        if (key == KeyCode.ENTER) {
            MessageInfo selected = messagesTable.getSelectedItem();
            if (selected != null) {
                selectedMessage = selected;
                detailScrollOffset = 0;
                viewMode = ViewMode.MESSAGE_DETAIL;
                redraw();
                return true;
            }
        }

        // R to refresh messages
        if (key == 'r' || key == 'R') {
            loadMessages(selectedTopicName);
            return true;
        }

        // Handle navigation in messages table
        if (messagesTable.handleKey(key)) {
            redraw();
            return true;
        }

        return false;
    }

    private boolean handleMessageDetailKey(int key) {
        // Escape to go back to messages
        if (key == KeyCode.ESCAPE) {
            viewMode = ViewMode.MESSAGES;
            selectedMessage = null;
            redraw();
            return true;
        }

        // Scroll up
        if (key == KeyCode.UP || key == 'k') {
            if (detailScrollOffset > 0) {
                detailScrollOffset--;
                redraw();
            }
            return true;
        }

        // Scroll down
        if (key == KeyCode.DOWN || key == 'j') {
            detailScrollOffset++;
            redraw();
            return true;
        }

        // Page up
        if (key == KeyCode.PAGE_UP) {
            detailScrollOffset = Math.max(0, detailScrollOffset - 10);
            redraw();
            return true;
        }

        // Page down
        if (key == KeyCode.PAGE_DOWN) {
            detailScrollOffset += 10;
            redraw();
            return true;
        }

        return false;
    }

    @Override
    protected void renderPanelContent(Buffer buffer, int startRow, int startCol, int panelWidth, int panelHeight) {
        int row = startRow;

        if (loading) {
            return;
        }

        if (error != null) {
            return;
        }

        // Summary view for panel using KeyValuePanel
        KeyValuePanel summaryPanel = new KeyValuePanel();
        summaryPanel.addStyled("Broker", truncate(broker, panelWidth - 10), Style.create().cyan());
        summaryPanel.add("Topics", String.valueOf(topics.size()));
        summaryPanel.add("Groups", String.valueOf(consumerGroups.size()));
        summaryPanel.add("Nodes", String.valueOf(nodes.size()));
        row = summaryPanel.render(buffer, row, startCol, panelWidth);
        row++;

        // Show first few topics
        row++;

        int maxTopics = Math.min(topics.size(), panelHeight - row + startRow - 1);
        for (int i = 0; i < maxTopics; i++) {
            TopicInfo topic = topics.get(i);
            buffer.setString(startCol, row, truncate(topic.name(), panelWidth - 2), Style.EMPTY);
            row++;
        }
    }

    @Override
    public boolean handlePanelKey(int key) {
        if (loading) {
            return false;
        }

        if (topicsTable.handleKey(key)) {
            return true;
        }

        return super.handlePanelKey(key);
    }

    // Data records
    private record TopicInfo(String name, String topicId, int partitions, boolean internal, long messageCount) {
    }

    private record ConsumerGroupInfo(String name, String state, String coordinatorHost, String protocol, long lag) {
    }

    private record AclEntry(String operation, String principal, String permission, String pattern) {
    }

    private record NodeInfo(String id, String host, int port) {
    }

    private record MessageInfo(int partition, long offset, String timestamp, String key, String value) {
    }
}
