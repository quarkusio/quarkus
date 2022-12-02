import {doPost, errorPopUp} from "../web/web.js";
import {createIcon, createTableItem, createTableItemHtml, hideItem, showItem} from "../util/contentManagement.js";
import {pages} from "./navigator.js";

export default class TopicsPage {
    constructor(navigator, containerId) {
        this.navigator = navigator;
        this.containerId = containerId;
        this.registerButtonHandlers();

        // TODO: move to common function with comment
        Object.getOwnPropertyNames(TopicsPage.prototype).forEach((key) => {
            if (key !== 'constructor') {
                this[key] = this[key].bind(this);
            }
        });
    }

    open() {
        window.currentContext = {};
        this.requestTopics(this.onTopicsLoaded, this.onTopicsFailed);
    }

    registerButtonHandlers() {

        const topicNameInput = $("#topic-name-modal-input");
        $("#create-topic-btn").click(() => {
            if (!this.validateTopicName(topicNameInput.val())) {
                this.showErrorIfInvalid(topicNameInput.val(), this.validateTopicName, topicNameValidationErrorBox);
                return;
            }

            this.createTopic(this.onTopicsLoaded, this.onTopicsFailed);
            $('#create-topic-modal').modal('hide');
            $('#topic-name-modal-input').val("");
            $('#partitions-modal-input').val("");
            $('#replications-modal-input').val("");
        })

        $("#open-create-topic-modal-btn").click(() => {
            this.loadNodesCount();
            $('#create-topic-modal').modal('show');
        });

        $('.close-modal-btn').click(() => {
            hideItem($(".modal"));
            hideItem($("#topic-creation-validation-msg-box"));
            hideItem($("#topic-name-validation-msg"));
            hideItem($("#replication-validation-msg"));
        });

        $("#delete-topic-btn").click(() => {
            const currentTopic = window.currentContext.topicName;
            this.deleteTopic(currentTopic, this.deleteTopicRow, this.onTopicsFailed)
            $("#delete-topic-modal").modal("hide");
        });

        const topicNameValidationErrorBox = $("#topic-name-validation-msg");
        topicNameInput.keyup(() => this.showErrorIfInvalid(topicNameInput.val(), this.validateTopicName, topicNameValidationErrorBox));
        topicNameInput.change(() => this.showErrorIfInvalid(topicNameInput.val(), this.validateTopicName, topicNameValidationErrorBox));

        const replicationInput = $("#replications-modal-input");
        replicationInput.keyup(() => {
            const value = replicationInput.val();
            this.showErrorIfInvalid(value, this.validateReplicationFactor, $("#replication-validation-msg"));
        });
    }

    loadNodesCount() {
        const req = {
            action: "getInfo"
        };
        doPost(req, (data) => {
            window.currentContext.nodesCount = data.clusterInfo.nodes.length;
        }, data => {
            errorPopUp("Could not obtain nodes count.");
        });
    }

    showErrorIfInvalid(value, validationFunction, errBoxSelector) {
        const valid = validationFunction(value);
        if (!valid) {
            showItem($("#topic-creation-validation-msg-box"));
            showItem(errBoxSelector);
            $("#create-topic-btn")
                .addClass("disabled")
                .attr("disabled", true);
        } else {
            hideItem(errBoxSelector);
            const topicMsgValidationBoxChildren = $("#topic-creation-validation-msg-box span");
            const allChildrenHidden = topicMsgValidationBoxChildren
                .filter((x) => !$(x).hasClass("hidden"))
                .length > 0;
            if (allChildrenHidden) {
                hideItem($("#topic-creation-validation-msg-box"));
                $("#create-topic-btn")
                    .removeClass("disabled")
                    .attr("disabled", false);
            }
        }
    }

    validateTopicName(name) {
        const legalChars = /^[a-zA-Z\d\.\_]+$/;
        const maxNameLength = 255;
        return legalChars.test(name) && name.length < maxNameLength;
    }

    validateReplicationFactor(replicationFactor) {
        return currentContext.nodesCount >= replicationFactor;
    }

    requestTopics(onTopicsLoaded, onTopicsFailed) {
        const req = {
            action: "getTopics"
        };
        doPost(req, onTopicsLoaded, onTopicsFailed);
    }

    onTopicsLoaded(data) {
        let tableBody = $('#topics-table tbody');
        tableBody.empty();

        for (let i = 0; i < data.length; i++) {
            let tableRow = $("<tr/>");
            let d = data[i];
            tableRow.append(createTableItem(d.name));
            tableRow.append(createTableItem(d.topicId));
            tableRow.append(createTableItem(d.partitionsCount));
            tableRow.append(createTableItem(("" + d.nmsg)));

            const deleteIcon = createIcon("bi-trash-fill");
            const deleteBtn = $("<btn/>")
                .addClass("btn")
                .click((event) => {
                    window.currentContext.topicName = d.name;
                    $("#delete-topic-modal").modal("show");
                    $("#delete-topic-name-span").text(d.name);
                    event.stopPropagation();
                })
                .append(deleteIcon);


            tableRow.click(() => {
                self.navigator.navigateTo(pages.TOPIC_MESSAGES, [d.name]);
            });
            const controlHolder = $("<div/>")
                .append(deleteBtn);
            tableRow.append(createTableItemHtml(controlHolder));

            const self = this;

            tableBody.append(tableRow);
        }
    }

    onTopicsFailed(data) {
        errorPopUp("Error getting topics: ", data);
    }

    createTopic(onTopicsLoaded, onTopicsFailed) {
        const topicName = $("#topic-name-modal-input").val();
        const partitions = $("#partitions-modal-input").val();
        const replications = $("#replications-modal-input").val();

        const req = {
            action: "createTopic",
            topicName: topicName,
            partitions: partitions,
            replications: replications
        };
        doPost(req, () => this.requestTopics(this.onTopicsLoaded, this.onTopicsFailed), onTopicsFailed);
    }

    // TODO: add pagination here
    deleteTopic(topicName, onTopicsDeleted, onTopicsFailed) {
        const req = {
            action: "deleteTopic",
            key: topicName
        };
        doPost(req, onTopicsDeleted, onTopicsFailed);
    }

    deleteTopicRow(data) {
        const topicName = window.currentContext.topicName;
        $("#topics-table > tbody > tr > td:contains('" + topicName + "')").parent().remove()
    }
}