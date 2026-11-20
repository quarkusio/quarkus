import { QwcHotReloadElement, html, css} from 'qwc-hot-reload-element';
import { JsonRpc } from 'jsonrpc';
import { notifier } from 'notifier';
import '@vaadin/progress-bar';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import '@vaadin/dialog';
import { dialogRenderer } from '@vaadin/dialog/lit.js';
import '@vaadin/button';
import './qwc-kafka-add-acl.js';
import { msg, str, updateWhenLocaleChanges } from 'localization';

/**
 * This component shows the Kafka Access Control List
 */
export class QwcKafkaAccessControlList extends QwcHotReloadElement { 

    jsonRpc = new JsonRpc(this);

    static styles = css`
        .error {
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            padding: 2em;
            gap: 0.75em;
            color: var(--lumo-error-color);
        }
        .error vaadin-icon {
            font-size: 2em;
        }

        .kafka {
            height: 100%;
            display: flex;
            flex-direction: column;
            overflow: hidden;
        }

        .bottom {
            display: flex;
            flex-direction: row-reverse;
        }

        .create-button {
            margin-right: 30px;
            margin-bottom: 10px;
            font-size: xx-large;
            cursor: pointer;
        }

        .delete-button {
            font-size: small;
            color: var(--lumo-error-text-color);
            cursor: pointer;
        }
    `;

    static properties = {
        _aclInfo: {state: true},
        _error: {state: true},
        _createAclDialogOpened: {state: true},
        _deleteAclDialogOpened: {state: true},
        _aclToDelete: {state: true}
    };

    constructor() {
        super();
        updateWhenLocaleChanges(this);
        this._error = null;
        this._createAclDialogOpened = false;
        this._deleteAclDialogOpened = false;
        this._aclToDelete = null;
    }

    connectedCallback() {
        super.connectedCallback();
        this.hotReload();
    }

    hotReload(){
        this._error = null;
        this.jsonRpc.getAclInfo().then(jsonRpcResponse => {
            if (jsonRpcResponse.result.errorMessage) {
                this._error = jsonRpcResponse.result.errorMessage;
            } else {
                this._aclInfo = jsonRpcResponse.result;
            }
        }).catch(error => {
            this._error = error?.error?.message ?? String(error);
        });
    }

    render() {
        if (this._error) {
            return html`
                <div class="error">
                    <vaadin-icon icon="font-awesome-solid:triangle-exclamation"></vaadin-icon>
                    <span>${this._error}</span>
                </div>`;
        } else if (this._aclInfo) {
            return html`<div class="kafka">
                ${this._renderCreateAclDialog()}
                ${this._renderConfirmDeleteDialog()}
                ${this._renderAclGrid()}

                <div class="bottom">
                    <vaadin-icon class="create-button" icon="font-awesome-solid:circle-plus" @click="${() => {this._createAclDialogOpened = true;}}" title=${msg('Create ACL', { id: 'quarkus-kafka-client-create-acl' })}></vaadin-icon>
                </div>
            </div>`;
        } else {
            return html`<vaadin-progress-bar class="progress" indeterminate></vaadin-progress-bar>`;
        }
    }

    _renderAclGrid() {
        return html`<vaadin-grid .items="${this._aclInfo.entries}"
                                class="table" theme="no-border">
                        <vaadin-grid-sort-column auto-width
                            path="operation"
                            header=${msg('Operation', { id: 'quarkus-kafka-client-operation' })}
                            resizable>
                        </vaadin-grid-sort-column>

                        <vaadin-grid-sort-column auto-width
                            path="principal"
                            header=${msg('Principal', { id: 'quarkus-kafka-client-principal' })}
                            resizable>
                        </vaadin-grid-sort-column>

                        <vaadin-grid-sort-column auto-width
                            path="perm"
                            header=${msg('Permission', { id: 'quarkus-kafka-client-permission' })}
                            resizable>
                        </vaadin-grid-sort-column>

                        <vaadin-grid-sort-column auto-width
                            path="host"
                            header=${msg('Host', { id: 'quarkus-kafka-client-host' })}
                            resizable>
                        </vaadin-grid-sort-column>

                        <vaadin-grid-sort-column auto-width
                            path="resourceType"
                            header=${msg('Resource Type', { id: 'quarkus-kafka-client-resource-type' })}
                            resizable>
                        </vaadin-grid-sort-column>

                        <vaadin-grid-sort-column auto-width
                            path="resourceName"
                            header=${msg('Resource Name', { id: 'quarkus-kafka-client-resource-name' })}
                            resizable>
                        </vaadin-grid-sort-column>

                        <vaadin-grid-sort-column auto-width
                            path="patternType"
                            header=${msg('Pattern Type', { id: 'quarkus-kafka-client-pattern-type' })}
                            resizable>
                        </vaadin-grid-sort-column>

                        <vaadin-grid-column
                            text-align="end"
                            ${columnBodyRenderer(this._deleteActionRenderer, [])}>
                        </vaadin-grid-column>

                    </vaadin-grid>`;
    }

    _deleteActionRenderer(acl) {
        return html`<vaadin-icon class="delete-button" icon="font-awesome-solid:trash-can" @click="${() => this._openConfirmDeleteDialog(acl)}" title=${msg('Delete ACL', { id: 'quarkus-kafka-client-delete-acl' })}></vaadin-icon>`;
    }

    _renderCreateAclDialog() {
        if (this._createAclDialogOpened) {
            return html`<vaadin-dialog class="createDialog"
                    header-title=${msg('Create ACL', { id: 'quarkus-kafka-client-create-acl' })}
                    resizable
                    .opened="${this._createAclDialogOpened}"
                    @opened-changed="${(e) => (this._createAclDialogOpened = e.detail.value)}"
                    ${dialogRenderer(() => this._renderCreateAclDialogForm(), msg('Create ACL', { id: 'quarkus-kafka-client-create-acl' }))}
                ></vaadin-dialog>`;
        }
    }

    _renderCreateAclDialogForm() {
        return html`<qwc-kafka-add-acl
                        @kafka-acl-added-success=${this._aclAdded}
                        @kafka-acl-added-canceled=${this._aclAddedCanceled}
                        extensionName="${this.jsonRpc.getExtensionName()}">
                    </qwc-kafka-add-acl>`;
    }

    _aclAdded(e) {
        this._createAclDialogOpened = false;
        this._aclInfo = e.detail.result;
    }

    _aclAddedCanceled() {
        this._createAclDialogOpened = false;
    }

    _renderConfirmDeleteDialog() {
        return html`<vaadin-dialog class="deleteDialog"
                    header-title=${msg('Confirm delete', { id: 'quarkus-kafka-client-confirm-delete' })}
                    .opened="${this._deleteAclDialogOpened}"
                    @opened-changed="${(e) => (this._deleteAclDialogOpened = e.detail.value)}"
                    ${dialogRenderer(() => this._renderDeleteAclDialogForm(), [this._aclToDelete])}
                ></vaadin-dialog>`;
    }

    _renderDeleteAclDialogForm() {
        const acl = this._aclToDelete;
        if (!acl) {
            return html``;
        }
        return html`
            ${msg(str`Are you sure you want to delete this ACL: ${acl.principal} ${acl.perm} ${acl.operation} on ${acl.resourceType} ${acl.resourceName} (${acl.patternType})?`, { id: 'quarkus-kafka-client-delete-acl-confirm' })}<br/>
            <div style="display: flex; flex-direction: row-reverse; gap: 10px;">
                <vaadin-button theme="secondary error" @click=${this._submitDeleteAclForm}>${msg('Delete', { id: 'quarkus-kafka-client-delete' })}</vaadin-button>
                <vaadin-button theme="secondary" @click=${this._resetDeleteAclForm}>${msg('Cancel', { id: 'quarkus-kafka-client-cancel' })}</vaadin-button>
            </div>
        `;
    }

    _openConfirmDeleteDialog(acl) {
        this._aclToDelete = acl;
        this._deleteAclDialogOpened = true;
    }

    _resetDeleteAclForm() {
        this._aclToDelete = null;
        this._deleteAclDialogOpened = false;
    }

    _submitDeleteAclForm() {
        const acl = this._aclToDelete;
        this._resetDeleteAclForm();
        this.jsonRpc.deleteAcl({
            principal: acl.principal,
            host: acl.host,
            operation: acl.operation,
            permissionType: acl.perm,
            resourceType: acl.resourceType,
            resourceName: acl.resourceName,
            patternType: acl.patternType
        }).then(jsonRpcResponse => {
            if (jsonRpcResponse.result.errorMessage) {
                // keep the grid: a failed delete must not hide the ACL list
                notifier.showErrorMessage(jsonRpcResponse.result.errorMessage);
            } else {
                this._aclInfo = jsonRpcResponse.result;
            }
        }).catch(error => {
            notifier.showErrorMessage(error?.error?.message ?? String(error));
        });
    }
}

customElements.define('qwc-kafka-access-control-list', QwcKafkaAccessControlList);