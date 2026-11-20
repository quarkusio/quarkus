import { LitElement, html, css} from 'lit';
import { JsonRpc } from 'jsonrpc';
import { notifier } from 'notifier';

import '@vaadin/text-field';
import '@vaadin/select';
import '@vaadin/button';
import { msg, updateWhenLocaleChanges } from 'localization';

const toItems = values => values.map(v => ({label: v, value: v}));

const OPERATIONS = toItems(['ALL', 'READ', 'WRITE', 'CREATE', 'DELETE', 'ALTER', 'DESCRIBE', 'CLUSTER_ACTION',
    'DESCRIBE_CONFIGS', 'ALTER_CONFIGS', 'IDEMPOTENT_WRITE', 'CREATE_TOKENS', 'DESCRIBE_TOKENS']);
const PERMISSION_TYPES = toItems(['ALLOW', 'DENY']);
const RESOURCE_TYPES = toItems(['TOPIC', 'GROUP', 'CLUSTER', 'TRANSACTIONAL_ID', 'DELEGATION_TOKEN', 'USER']);
const PATTERN_TYPES = toItems(['LITERAL', 'PREFIXED']);

// Kafka only accepts this name for ACLs on the CLUSTER resource
const CLUSTER_RESOURCE_NAME = 'kafka-cluster';

/**
 * This component shows the add ACL screen
 */
export class QwcKafkaAddAcl extends LitElement {
    jsonRpc = new JsonRpc(this);

    static styles = css`
        :host {
            display: flex;
            flex-direction: column;
        }
    `;

    static properties = {
        extensionName: {type: String},
        _acl: {state: true}
    };

    constructor() {
        super();
        updateWhenLocaleChanges(this);
        this._reset();
    }

    connectedCallback() {
        super.connectedCallback();
        this._reset();
        this.jsonRpc = new JsonRpc(this.extensionName);
    }

    render(){
        const isCluster = this._acl.resourceType === 'CLUSTER';
        return html`<vaadin-text-field
                        label=${msg('Principal', { id: 'quarkus-kafka-client-principal' })}
                        placeholder=${msg('User:alice', { id: 'quarkus-kafka-client-principal-placeholder' })}
                        .value="${this._acl.principal}"
                        @value-changed="${(e) => this._set('principal', e.detail.value)}"
                        required
                        clear-button-visible>
                    </vaadin-text-field>
                    <vaadin-text-field
                        label=${msg('Host', { id: 'quarkus-kafka-client-host' })}
                        .value="${this._acl.host}"
                        @value-changed="${(e) => this._set('host', e.detail.value)}">
                    </vaadin-text-field>
                    <vaadin-select
                        label=${msg('Operation', { id: 'quarkus-kafka-client-operation' })}
                        .items="${OPERATIONS}"
                        .value="${this._acl.operation}"
                        @value-changed="${(e) => this._set('operation', e.detail.value)}">
                    </vaadin-select>
                    <vaadin-select
                        label=${msg('Permission', { id: 'quarkus-kafka-client-permission' })}
                        .items="${PERMISSION_TYPES}"
                        .value="${this._acl.permissionType}"
                        @value-changed="${(e) => this._set('permissionType', e.detail.value)}">
                    </vaadin-select>
                    <vaadin-select
                        label=${msg('Resource Type', { id: 'quarkus-kafka-client-resource-type' })}
                        .items="${RESOURCE_TYPES}"
                        .value="${this._acl.resourceType}"
                        @value-changed="${(e) => this._resourceTypeChanged(e.detail.value)}">
                    </vaadin-select>
                    <vaadin-text-field
                        label=${msg('Resource Name', { id: 'quarkus-kafka-client-resource-name' })}
                        .value="${this._acl.resourceName}"
                        ?disabled="${isCluster}"
                        @value-changed="${(e) => this._set('resourceName', e.detail.value)}"
                        required>
                    </vaadin-text-field>
                    <vaadin-select
                        label=${msg('Pattern Type', { id: 'quarkus-kafka-client-pattern-type' })}
                        .items="${PATTERN_TYPES}"
                        .value="${this._acl.patternType}"
                        ?disabled="${isCluster}"
                        @value-changed="${(e) => this._set('patternType', e.detail.value)}">
                    </vaadin-select>
                    ${this._renderButtons()}`;
    }

    _renderButtons(){
        return html`<div style="display: flex; flex-direction: row-reverse; gap: 10px;">
                        <vaadin-button theme="secondary" @click=${this._submit}>${msg('Create', { id: 'quarkus-kafka-client-create' })}</vaadin-button>
                        <vaadin-button theme="secondary error" @click=${this._cancel}>${msg('Cancel', { id: 'quarkus-kafka-client-cancel' })}</vaadin-button>
                    </div>`;
    }

    _reset(){
        this._acl = {
            principal: '',
            host: '*',
            operation: 'READ',
            permissionType: 'ALLOW',
            resourceType: 'TOPIC',
            resourceName: '',
            patternType: 'LITERAL'
        };
    }

    // Replace the object so Lit re-renders (needed to enable/disable the resource name field)
    _set(field, value){
        if (this._acl[field] !== value) {
            this._acl = {...this._acl, [field]: value};
        }
    }

    _resourceTypeChanged(resourceType){
        if (resourceType === 'CLUSTER') {
            this._acl = {...this._acl, resourceType, resourceName: CLUSTER_RESOURCE_NAME, patternType: 'LITERAL'};
        } else if (this._acl.resourceType === 'CLUSTER') {
            this._acl = {...this._acl, resourceType, resourceName: ''};
        } else {
            this._set('resourceType', resourceType);
        }
    }

    _cancel(){
        this._reset();
        const canceled = new CustomEvent("kafka-acl-added-canceled", {
            detail: {},
            bubbles: true,
            cancelable: true,
            composed: false
        });
        this.dispatchEvent(canceled);
    }

    _submit(){
        if(this._acl.principal.trim() === '' || this._acl.resourceName.trim() === ''){
            notifier.showErrorMessage(msg('Principal and resource name are required', { id: 'quarkus-kafka-client-acl-required' }));
            return;
        }
        this.jsonRpc.createAcl({
            principal: this._acl.principal.trim(),
            host: this._acl.host.trim(),
            operation: this._acl.operation,
            permissionType: this._acl.permissionType,
            resourceType: this._acl.resourceType,
            resourceName: this._acl.resourceName.trim(),
            patternType: this._acl.patternType
        }).then(jsonRpcResponse => {
            if (jsonRpcResponse.result.errorMessage) {
                // keep the form open so the user can correct the input
                notifier.showErrorMessage(jsonRpcResponse.result.errorMessage);
                return;
            }
            this._reset();
            const success = new CustomEvent("kafka-acl-added-success", {
                detail: {result: jsonRpcResponse.result},
                bubbles: true,
                cancelable: true,
                composed: false
            });
            this.dispatchEvent(success);
        }).catch(error => {
            notifier.showErrorMessage(error?.error?.message ?? String(error));
        });
    }
}

customElements.define('qwc-kafka-add-acl', QwcKafkaAddAcl);
