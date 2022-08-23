import {createTableItem} from "../util/contentManagement.js";
import {doPost, errorPopUp} from "../web/web.js";
import {pages} from "./navigator.js";
import {toggleSpinner} from "../util/spinner.js";

export default class ConsumerGroupPage {
    constructor(navigator, containerId) {
        this.containerId = containerId;
        this.navigator = navigator;
        Object.getOwnPropertyNames(ConsumerGroupPage.prototype).forEach((key) => {
            if (key !== 'constructor') {
                this[key] = this[key].bind(this);
            }
        });
    }

    open() {
        toggleSpinner(this.containerId);
        const req = {
            action: "getInfo", key: "0", value: "0"
        };
        doPost(req, (data) => {
            this.updateConsumerGroups(data.consumerGroups);
            toggleSpinner(this.containerId);
        }, data => {
            errorPopUp("Error getting Kafka info: ", data);
            toggleSpinner(this.containerId);
        });
    }

    updateConsumerGroups(data) {
        let consumerGroupsTable = $('#consumer-groups-table tbody');
        consumerGroupsTable.empty();
        for (let i = 0; i < data.length; i++) {
            const d = data[i];
            let tableRow = $("<tr/>");
            tableRow.append(createTableItem(d.state));
            tableRow.append(createTableItem(d.name));
            tableRow.append(createTableItem(d.coordinatorId));
            tableRow.append(createTableItem(d.protocol));
            tableRow.append(createTableItem(d.members.length));
            tableRow.append(createTableItem(d.lag));
            tableRow.click(() => this.navigator.navigateTo(pages.CONSUMER_GROUPS_DETAILS, [d.name, d.members]));
            consumerGroupsTable.append(tableRow);
        }
    }
}