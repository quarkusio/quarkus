import MessagesPage from "./messagesPage.js";
import TopicsPage from "./topicsPage.js";
import ConsumerGroupPage from "./consumerGroupPage.js";
import ConsumerGroupDetailsPage from "./consumerGroupDetailsPage.js";
import AccessControlListPage from "./accessControlListPage.js";
import NodesPage from "./nodesPage.js";
import {createIcon} from "../util/contentManagement.js";

export const pages = {
    TOPICS: "topics-page",
    SCHEMA: "schema-page",
    CONSUMER_GROUPS: "consumer-groups-page",
    CONSUMER_GROUPS_DETAILS: "consumer-groups-details-page",
    ACCESS_CONTROL_LIST: "access-control-list-page",
    NODES: "nodes-page",
    TOPIC_MESSAGES: "topic-messages-page",
    DEFAULT: "topics-page"
}

export default class Navigator {
    constructor() {
        this.registerNavbar();
    }

    allPages = {
        [pages.TOPICS]: {
            header: "Topics",
            showInNavbar: true,
            instance: new TopicsPage(this, pages.TOPICS),
            icon: "bi-collection"
        },
        [pages.SCHEMA]: {
            header: "Schema registry",
            showInNavbar: true,
            icon: "bi-file-code"
        },
        [pages.CONSUMER_GROUPS]: {
            header: "Consumer groups",
            showInNavbar: true,
            instance: new ConsumerGroupPage(this, pages.CONSUMER_GROUPS),
            icon: "bi-inboxes"
        },
        [pages.ACCESS_CONTROL_LIST]: {
            header: "Access control list",
            showInNavbar: true,
            instance: new AccessControlListPage(pages.ACCESS_CONTROL_LIST),
            icon: "bi-shield-lock"
        },
        [pages.NODES]: {
            header: "Nodes",
            showInNavbar: true,
            instance: new NodesPage(pages.NODES),
            icon: "bi-diagram-3"
        },
        [pages.TOPIC_MESSAGES]: {
            header: "Messages",
            showInNavbar: false,
            instance: new MessagesPage(pages.TOPIC_MESSAGES),
            parent: pages.TOPICS
        },
        [pages.CONSUMER_GROUPS_DETAILS]: {
            header: "Consumer group details",
            showInNavbar: false,
            instance: new ConsumerGroupDetailsPage(pages.CONSUMER_GROUPS_DETAILS),
            parent: pages.CONSUMER_GROUPS
        }
    };

    registerNavbar() {
        const keys = Object.keys(this.allPages);
        const navbar = $("#navbar-list");
        navbar.empty();

        for (let i = 0; i < keys.length; i++) {
            const key = keys[i];
            const value = this.allPages[key];
            if (!value.showInNavbar) continue;
            const navItem = $("<li/>")
                .addClass("nav-item")
                .addClass("left-padding")
                .addClass("pointer");

            const navHolder = $("<div/>")
                .addClass("d-flex")
                .addClass("left-margin")
                .addClass("nav-row")
                .click(() => this.navigateTo(key));

            const icon = createIcon(value.icon)
                .addClass("align-self-center");
            const navLink = $("<a/>", {
                text: value.header,
                href: "#"
            })
                .addClass("nav-link")
                .addClass("active")
                .addClass("link");
            navHolder.append(icon);
            navHolder.append(navLink);
            navItem.append(navHolder);
            navbar.append(navItem);
        }
    }

    navigateTo(requestedPage, params) {
        const keys = Object.keys(this.allPages);
        for (let i = 0; i < keys.length; i++) {
            const elementName = keys[i];
            const d = $("#" + elementName);
            if (d !== null) {
                if (elementName !== requestedPage) {
                    d.removeClass("shown")
                        .addClass("hidden");
                } else {
                    d.removeClass("hidden")
                        .addClass("shown");
                    this.open(requestedPage, params);
                }
            } else {
                console.error("Can not find page div: ", keys[i]);
            }
        }

        this.navigateBreadcrumb(requestedPage, params);
    }

    navigateToDefaultPage() {
        this.navigateTo(pages.DEFAULT);
    }

    open(pageId, params) {
        const value = this.allPages[pageId];
        value.instance.open(params);
    }

    navigateBreadcrumb(page, params) {
        const breadcrumb = $("#nav-breadcrumb");
        breadcrumb.empty();

        let nextPage = this.allPages[page];
        let pageId = page;

        let i = 0;
        while (nextPage !== undefined) {
            let li;
            // We only need to append possible params to the very first element.
            if (i === 0) {
                li = this.createBreadcrumbItem(nextPage.header, pageId, true, params);
            } else {
                li = this.createBreadcrumbItem(nextPage.header, pageId, false);
            }
            breadcrumb.prepend(li);
            pageId = nextPage.parent;
            nextPage = this.allPages[pageId];
            i++;
        }
    }

    createBreadcrumbItem(text, pageId, isActive, params) {
        let breadcrumbText = text;
        if (params !== undefined && params.length > 0 && (params[0] !== null && params[0] !== undefined)) {
            breadcrumbText = text + " (" + params[0] + ")";
        }
        const a = $("<a/>", {href: "#", text: breadcrumbText})
            .click(() => this.navigateTo(pageId, params));
        if (isActive) {
            a.addClass("active");
        }

        const li = $("<li/>")
            .addClass("breadcrumb-item");
        li.append(a);
        return li;
    }
}