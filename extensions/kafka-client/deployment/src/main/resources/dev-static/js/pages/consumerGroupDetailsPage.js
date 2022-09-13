import {CollapseRow, createTableHead, createTableItem, createTableItemHtml} from "../util/contentManagement.js";

export default class ConsumerGroupDetailsPage {
    constructor(containerId) {
        this.containerId = containerId;
        Object.getOwnPropertyNames(ConsumerGroupDetailsPage.prototype).forEach((key) => {
            if (key !== 'constructor') {
                this[key] = this[key].bind(this);
            }
        });
    }

    open(params) {
        const membersData = params[1];
        let consumerGroupsTable = $('#consumer-group-details-table tbody');
        consumerGroupsTable.empty();
        for (let i = 0; i < membersData.length; i++) {
            const d = membersData[i];
            const groupId = "group-" + d.memberId;

            let tableRow = $("<tr/>");
            let collapseRow;
            if (d.partitions.length > 0) {
                collapseRow = new CollapseRow(groupId);
                tableRow.append(createTableItemHtml(collapseRow.arrow));
            } else {
                tableRow.append(createTableItem(""));
            }

            const memberId = $("<b/>")
                .text(d.clientId);
            const id = d.memberId.substring(d.clientId.length);
            const text = $("<p/>")
                .append(memberId)
                .append(id);
            tableRow.append(createTableItemHtml(text));
            tableRow.append(createTableItem(d.host));
            tableRow.append(createTableItem("" + new Set(d.partitions.map(x => x.partition)).size));
            tableRow.append(createTableItem("" + d.partitions.map(x => x.lag).reduce((l, r) => l + r, 0)));

            if (d.partitions.length > 0) {
                const content = this.createConsumerGroupCollapseInfo(d);
                tableRow.addClass("pointer")
                tableRow.click(() => collapseRow.collapse());
                consumerGroupsTable.append(tableRow);
                consumerGroupsTable.append(collapseRow
                    .getCollapseContent(tableRow.children().length, content)
                    .addClass("no-hover"));
            } else {
                consumerGroupsTable.append(tableRow);
            }
        }
    }

    createConsumerGroupCollapseInfo(dataItem) {
        const collapseContent = $("<table/>")
            .addClass("table")
            .addClass("table-sm")
            .addClass("no-hover");

        const headers = $("<tr/>")
            .addClass("no-hover")
            .append(createTableHead("Topic"))
            .append(createTableHead("Partition"))
            .append(createTableHead("Lag"));
        const head = $("<thead/>")
            .append(headers);

        const body = $("<tbody/>");
        for (let partition of dataItem.partitions) {
            const row = $("<tr/>")
                .addClass("no-hover");
            row.append(createTableItemHtml(partition.topic))
            row.append(createTableItemHtml(partition.partition))
            row.append(createTableItemHtml(partition.lag))
            body.append(row);
        }

        collapseContent.append(head);
        collapseContent.append(body);

        return collapseContent;
    }

}