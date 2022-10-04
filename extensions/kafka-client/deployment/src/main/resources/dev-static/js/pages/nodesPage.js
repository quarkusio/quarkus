import {doPost, errorPopUp} from "../web/web.js";
import {createTableItem} from "../util/contentManagement.js";
import {toggleSpinner} from "../util/spinner.js";

export default class NodesPage {
    constructor(containerId) {
        this.containerId = containerId;
        Object.getOwnPropertyNames(NodesPage.prototype).forEach((key) => {
            if (key !== 'constructor') {
                this[key] = this[key].bind(this);
            }
        });
    }

    open() {
        const req = {
            action: "getInfo"
        };
        doPost(req, (data) => {
            setTimeout(() => {
                this.updateInfo(data);
                toggleSpinner(this.containerId);
            }, 2000);
        }, data => {
            errorPopUp("Error getting Kafka info: ", data);
        });
        toggleSpinner(this.containerId);
    }

    updateInfo(data) {
        $('#cluster-id').html(data.clusterInfo.id);
        $('#cluster-controller').html(data.broker);
        $('#cluster-acl').html(data.clusterInfo.aclOperations);

        const nodes = data.clusterInfo.nodes;
        let clusterNodesTable = $('#cluster-table tbody');
        clusterNodesTable.empty();
        for (let i = 0; i < nodes.length; i++) {
            const d = nodes[i];
            let tableRow = $("<tr/>");
            tableRow.append(createTableItem(d.id));
            tableRow.append(createTableItem(d.host));
            tableRow.append(createTableItem(d.port));
            clusterNodesTable.append(tableRow);
        }
    }
}