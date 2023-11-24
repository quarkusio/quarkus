import { LitElement, html, css } from 'lit';
import { JsonRpc } from 'jsonrpc';
import { RouterController } from 'router-controller';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import { dialogFooterRenderer, dialogRenderer } from '@vaadin/dialog/lit.js';
import '@vaadin/grid/vaadin-grid-column-group.js';
import '@vaadin/icon';
import '@vaadin/button';
import '@vaadin/list-box';
import '@vaadin/item';
import '@vaadin/tooltip';
import '@vaadin/checkbox';
import '@vaadin/combo-box';
import '@vaadin/text-field';
import '@vaadin/horizontal-layout';
import { notifier } from 'notifier';
import { observeState } from 'lit-element-state';
import { connectionState } from 'connection-state';

/**
 * This component allows users to change the permission sets
 */
export class QwcAuthConfiguration extends observeState(LitElement) {

  static styles = css`

        :host {
          display: block;
          width: 100%;
          height: 100%;
        }

        :host,
        :host * {
          box-sizing: border-box;
        }

        .conf {
          display: flex;
          flex-direction: column;
          height: 100%;
          overflow: hidden;
        }

        .confTopBar {
          display: flex;
          justify-content: space-between;
          align-items: center;
        }

        .permission-sets-wrapper {
          display: flex;
          overflow-y: auto;
        }

        .permission-sets-navigation {
          display: flex;
          flex-direction: column;
          height: 100%;
          width: 10%;
          overflow-y: auto;
        }

        .permission-sets-item {
          display: flex;
          flex-direction: column;
          overflow-y: scroll;
          padding: 0.5em;
          flex: 1;
        }

        .card {
          height: 100%;
          display: flex;
          flex-direction: column;
          border: 1px solid var(--lumo-contrast-10pct);
          border-radius: 4px;
          width: 100%;
          padding: 0.5rem;
          margin-bottom: 0.5em;
          filter: brightness(90%);
        }

        .card:hover {
          box-shadow: 0 4px 8px 0 rgba(0, 0, 0, 0.2);
        }

        .permission-set-form {
          display: flex;
        }

        .permission-set-form-item {
          display: flex;
          flex-direction: column;
          align-items: center;
        }

        .w100 {
          width: 100%;
        }

        .h100 {
          height: 100%;
        }

        .flex {
          display: flex;
        }

        .flex-1 {
          flex: 1;
        }

        .row-path {
          display: flex;
          align-items: center;
          justify-content: space-between;
          width: 100%;
        }

        .methodButton {
          font-size: 0.80rem;
          height: auto;
          margin: 0;
          padding: 0;
        }

        vaadin-grid {
          height: 100%;
        }

        vaadin-grid-cell-content {
          vertical-align: top;
          width: 100%;
        }

        .align-items-list {
          display: flex;
          align-items: center;
          justify-content: space-between;
        }

        .status-color-new {
          color: var(--lumo-success-color);
        }

        .status-color-added {
          color: var(--lumo-primary-text-color);
        }

        .status-color-deleted {
          position: relative;
        }

        .status-color-deleted:before {
            content: " ";
            position: absolute;
            top: 50%;
            left: 0;
            border-bottom: 1px solid var(--lumo-error-color);
            width: 100%;
        }

    `;


  jsonRpc = new JsonRpc(this);
  routerController = new RouterController(this);

  static properties = {
    _filtered: { state: true, type: Array }, // Filter the visible configuration
    _allPermissions: { state: true, type: Array },
    _dialogOpened: { state: true }
  };

  connectedCallback() {
    super.connectedCallback();
    this.permissionMode = this.routerController.getCurrentRoutePath() === '/q/dev-ui/auth-permissions';// true = permission mode, false = policy mode
    this._filteredValue = this.routerController.getQueryParameter("filter");
    this.interval = setInterval(() => this.hideMe(), 50);
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    clearInterval(this.interval)
  }

  _parseResponse(e) {
    this._initialMethods = e.result.metadata.methods;
    this._paths = e.result.metadata.paths;
    this._methods = e.result.metadata.methods;
    this._policies = this._toDropdownList(e.result.metadata.policies);

    e.result.permissions.forEach(p => {
      p.metadata = {
        allMethods: JSON.parse(JSON.stringify(e.result.metadata.methods)),
        allPaths: JSON.parse(JSON.stringify(e.result.metadata.paths)),
        path: "",
        method: ""
      };
      p.metadata.clone = JSON.parse(JSON.stringify(p));
    });
  }

  constructor() {
    super();
    this.jsonRpc.getAllPermissionGroupsConfig().then(e => {
      this._parseResponse(e);
      this._authMechanisms = this._toDropdownList(e.result.metadata.authMechanisms);
      this._allPermissions = e.result.permissions;
      this._filtered = e.result.permissions;
    })
  }

  render() {
    if (this._filtered && this._allPermissions) {
      return this._render();
    } else if (!connectionState.current.isConnected) {
      return html`<span>Waiting for backend connection...</span>`;
    } else {
      return html`<span>Loading permission set properties...</span>`;
    }
  }

  _toDropdownList(list) {
    return list.map(item => ({
      "label": item === "QuarkusNullValue" ? "unset" : item,
      "value": item
    }));
  }

  _filterTextChanged(e) {
    this._filteredValue = (e.detail.value || '').trim();

    let currentURL = new URL(window.location.href);

    if (this._filteredValue !== '') {
      currentURL.searchParams.set('filter', this._filteredValue);
    } else {
      currentURL.searchParams.delete('filter');
    }

    window.history.replaceState({}, document.title, currentURL.href);

    this._filterGrid();
  }

  _filterGrid() {
    if (this._filteredValue === '') {
      this._filtered = this._allPermissions;
      return;
    }

    this._filtered = this._fullTextSearch(this._allPermissions, this._filteredValue);
  }

  _fullTextSearch(objects, searchString) {
    const searchTerms = searchString.toLowerCase().split(' ');

    return objects.filter(obj => {
      return searchTerms.some(term => {
        return this._searchInObject(obj, term)
      });
    });
  }

  _searchInObject(obj, term) {
    for (const key in obj) {
      if (key === 'metadata' || key === 'configDescription') continue;
      term = term.trim();
      if (obj[key] !== null && typeof obj[key] === 'object') {
        if (Array.isArray(obj[key])) {
          for (let i = 0; i < obj[key].length; i++) {
            if (this._searchInObject(obj[key][i], term)) {
              return true;
            }
          }
        } else if (this._searchInObject(obj[key], term)) {
          return true;
        }
      } else if (typeof obj[key] === 'string' && obj[key].toLowerCase().includes(term)) {
        return true;
      }
    }

    return false;
  }

  _render() {
    let renderResults = this._filtered.length > 0;
    return html`<div class="conf">
                    <div class="confTopBar">
                        <vaadin-text-field
                                placeholder="Full text search (e.g. GET POST)"
                                value="${this._filteredValue}"
                                @value-changed="${(e) => this._filterTextChanged(e)}"
                                class="flex-1"
                                >
                        <vaadin-icon slot="prefix" icon="font-awesome-solid:filter"></vaadin-icon>
                        <qui-badge slot="suffix"><span>${this._filtered.length}</span></qui-badge>
                        </vaadin-text-field>
                        <vaadin-button @click="${this._showDialog}" theme="tertiary">Add New Permission</vaadin-button>
                    </div>
                    ${renderResults ? html`
                    <div class="permission-sets-wrapper">
                        <div class="permission-sets-navigation">
                            ${this._renderPermissionsList()}
                        </div>
                        <div class="permission-sets-item">
                            ${this._renderPermissions()}
                        </div>
                    </div>
                    ` : html`No results found with filter value '${this._filteredValue}'`}
                </div>
                ${this._dialogTemplate()}
                `;
  }

  _navigate(id) {
    this.shadowRoot.getElementById('authItem' + id).scrollIntoView({ behavior: 'smooth' });
  }

  _renderPermissionsList() {
    return html`
            ${this._filtered.map(permission =>
      html`<vaadin-button @click="${() => this._navigate(permission.name)}" theme="tertiary">${permission.name}</vaadin-button>`)
      }`;
  }

  _renderPermissions() {
    return html`
           ${this._filtered.map((permission, index) => this._renderPermission(permission, index))}
       `;
  }

  _findSelectedValues(list1, list2) {
    return list1.reduce((indexes, element, index) => {
      const matchingIndex = list2.indexOf(element);

      if (matchingIndex !== -1) {
        indexes.push(index);
      }

      return indexes;
    }, []);
  }

  _findIndexesOfSelected(list, item) {
    const index = list.indexOf(item)
    return matchingIndex !== -1 ? index : null;
  }

  _removeNewMethod(permission, value) {
    this._deleteStringFromList(permission.metadata.allMethods, value)
    this._deleteStringFromList(permission.methods, value)
    this.requestUpdate();
  }

  hideMe() {
    //TODO: remove this when https://github.com/vaadin/web-components/issues/6794 is patched
    const authElement = document.querySelector("qwc-auth");
    if (authElement) {
      const vaadinGridElements = authElement.shadowRoot.querySelectorAll("div.permission-set-form > div > vaadin-grid")
      if (vaadinGridElements.length > 0) {
        Array.from(vaadinGridElements)
          .map(e => e.shadowRoot)
          .flatMap(shadowRoot => Array.from(shadowRoot.querySelectorAll("#items > tr > td")))
          .forEach(e => e.parentElement.style.display = 'flex')
        Array.from(vaadinGridElements)
          .map(e => e.shadowRoot)
          .flatMap(shadowRoot => Array.from(shadowRoot.querySelectorAll("#items > tr > td.hide-me")))
          .forEach(e => e.parentElement.style.display = 'none')
      }
    }
  }

  _removeMethod(permission, value) {
    this._deleteStringFromList(permission.methods, value)
    this.requestUpdate();
  }

  _deleteStringFromList(list, string) {
    const index = list.indexOf(string);
    if (index > -1) list.splice(index, 1);
  }

  _addNewMethod(e, permission) {

    const input = e.target;
    const value = input.value;

    if (e.key === 'Enter') {
      permission.metadata.method = '';
      if (value === '') {
        notifier.showInfoMessage("Method name cannot be empty");
      } else if (!permission.methods.map(m => m.toLowerCase()).includes(value.toLowerCase())) {
        permission.methods.unshift(value);
        if (!permission.metadata.allMethods.includes(value)) {
          permission.metadata.allMethods.unshift(value);
        }
        this.requestUpdate();
        e.currentTarget.clear();
      } else {
        notifier.showInfoMessage("Method '" + value + "' already exists");
      }
    } else {
      permission.metadata.method = value;
    }

  }

  _onMethodItemClick(e, permission, method, state) {

    if (state !== 'NEW') {
      const index = permission.methods.indexOf(method);
      if (index > -1) {
        this._deleteStringFromList(permission.methods, method)
      } else {
        permission.methods.push(method);
      }
    }

    e.stopPropagation()

    this.requestUpdate();
  }

  _addPath(event, permission) {
    const value = event.target.value;
    if (value !== '' && !permission.paths.includes(value)) {
      event.target.value = "";
      permission.paths = [value, ...permission.paths];
      if (!permission.metadata.allPaths.includes(value)) {
        permission.metadata.allPaths = [value, ...permission.metadata.allPaths];
      } else {
        permission.metadata.allPaths = [...permission.metadata.allPaths]; // change the reference so it will render
      }
      this.requestUpdate();
    } else {
      notifier.showInfoMessage("Path '" + value + "' already exists");
    }

  }

  _renderMethods(permission, index) {
    return html`
        <vaadin-tooltip class="qwc-auth-tooltip" for="${permission.name}methodsLabel${index}" slot="tooltip" text="quarkus.http.auth.permission.“${permission.name}”.methods"></vaadin-tooltip>
        <vaadin-text-field id="${permission.name}methodsLabel${index}" value="${permission.metadata.method}" placeholder="Add new method" class="flex-1" clear-button-visible
                @value-changed="${(e) => { permission.metadata.method = e.target.value;this.requestUpdate();}}"
                @keydown=${(e) => this._addNewMethod(e, permission)}>
        </vaadin-text-field>
        <vaadin-list-box multiple .selectedValues="${this._findSelectedValues(permission.metadata.allMethods, permission.methods)}" class="w100" style="min-height: 200px;">
          ${permission.metadata.allMethods.map((method) => {

          let state = 'DEFAULT'; // DEFAULT, ADDED, DELETED, NEW
          let tooltip;

          const defaultState = this._initialMethods.includes(method);
          const deletedState = !permission.methods.includes(method) && permission.metadata.clone.methods.includes(method) && defaultState;
          const newState = !defaultState && !permission.metadata.clone.methods.includes(method);
          const addedState = !newState && !permission.metadata.clone.methods.includes(method) && permission.methods.includes(method);


          if (deletedState) {
            state = 'DELETED';
            tooltip = `The method (${method}) will be deleted`;
          } else if (addedState) {
            state = 'ADDED';
            tooltip = `The method (${method}) will be added`;
          } else if (newState) {
            state = 'NEW';
            tooltip = `The method (${method}) will be created`;
          }

          const clazzToAdd = `status-color-${state.toLowerCase()}`;
          const tooltipId = this._generateId(permission.name + 'tooltipMethodId');
          return html`
                <vaadin-item id="${tooltipId}" @click="${(e) => { this._onMethodItemClick(e, permission, method, state) }}">
                  <vaadin-horizontal-layout class="${clazzToAdd} align-items-list" theme="spacing">
                    ${method}
                    ${state === 'DEFAULT' ? '' : 
                    html`
                      <vaadin-tooltip for="${tooltipId}" slot="tooltip" text="${tooltip}"></vaadin-tooltip>
                      ${state === 'NEW' ? html`
                      <vaadin-button class="methodButton" @click="${(e) => { e.stopPropagation(); this._removeNewMethod(permission, method) }}" theme="tertiary error small">
                        <vaadin-icon icon="font-awesome-solid:trash-can" slot="prefix"></vaadin-icon>
                      </vaadin-button>` : ''}
                    `}
                  </vaadin-horizontal-layout>
                </vaadin-item>
                `;
           }
         )}
        </vaadin-list-box>`
  }


  _renderPaths(permission) {
    return html` 
    <vaadin-combo-box class="w100" placeholder="Search path to add"
          .items="${this._paths.filter((p) => !permission.paths.includes(p) && this._getPermissionState(permission, p).state !== 'DELETED')}"
          value="${permission.metadata.path}"
          @keydown=${(e) => {
                if (e.key === 'Enter') {
                  this._addPath(e, permission);
                }
          }}
          @change="${e => this._addPath(e, permission)}">
      <vaadin-tooltip slot="tooltip" text="quarkus.http.auth.permission.“${permission.name}”.paths"></vaadin-tooltip>
      <vaadin-icon slot="prefix" icon="font-awesome-solid:magnifying-glass"></vaadin-icon>
    </vaadin-combo-box>
    <vaadin-grid .items="${permission.metadata.allPaths}" .cellClassNameGenerator="${(column, model) => this._cellClassNameGenerator(column, model, permission)}">
      <vaadin-grid-column
        header="Path"
        ${columnBodyRenderer((path, model, column) => this._rowRender(path, model, column, permission), [permission.metadata.allPaths])}
      ></vaadin-grid-column>
    </vaadin-grid>
    `;
  }

  _cellClassNameGenerator(column, model, permission) {
    const path = model.item;
    const { state, tooltip, clazzToAdd } = this._getPermissionState(permission, path);

    if ((!permission.paths.includes(path) && state != 'DELETED')) {
      return 'hide-me';
    }
  }
  _renderPermission(permission, index) {
    const hasChanged = this._hasChanged(permission);
    return html`
            <div class="card" id="authItem${permission.name}">
                <div>
                    <qui-badge id="enabledLabel${index}" level="${permission.enabled ? 'success' : 'error'}"><span>${permission.name}</span></qui-badge>
                    <vaadin-checkbox .checked="${permission.enabled}" @click="${(e) => { e.preventDefault(); permission.enabled = !permission.enabled; this.requestUpdate() }}" label="Enabled"></vaadin-checkbox>
                    <vaadin-tooltip for="enabledLabel${index}" slot="tooltip" text="quarkus.http.auth.permission.“${permission.name}”.enabled"></vaadin-tooltip>
                </div>
                <div class="permission-set-form">
                    <div class="permission-set-form-item" style="width:15%">
                        ${this._renderMethods(permission, index)}
                    </div>
                    <div class="permission-set-form-item" style="width:23%">
                        ${this._renderPaths(permission)}
                    </div>
                    <div class="permission-set-form-item" style="width:62%;padding-left: 0.5em;">
                        <div class="w100 flex">
                            <div>
                                <label id="authMechanismLabel${index}">Auth Mechanism</label>
                                <vaadin-tooltip for="authMechanismLabel${index}" slot="tooltip" text="quarkus.http.auth.permission.“${permission.name}”.auth-mechanism"></vaadin-tooltip>
                                <vaadin-select .items="${this._authMechanisms}" .value="${permission.authMechanism}" @value-changed="${e => { permission.authMechanism = e.target.value; this.requestUpdate() }}"></vaadin-select>
                            </div>
                            <div style="margin-left:0.5em">
                                <label id="policyLabel${index}">Policy</label>
                                <vaadin-tooltip for="policyLabel${index}" slot="tooltip" text="quarkus.http.auth.permission.“${permission.name}”.policy"></vaadin-tooltip>
                                <vaadin-select .items="${this._policies}" .value="${permission.policy}" @value-changed="${e => { permission.policy = e.target.value; this.requestUpdate() }}"></vaadin-select>
                            </div>
                        </div>
                        <div class="w100 h100">
                            <vaadin-grid .items="${permission.permissionDescriptions}" class="w100" theme="row-stripes">
                                <vaadin-grid-column-group header="application.properties">
                                    <vaadin-grid-column path="propertyKey"></vaadin-grid-column>
                                    <vaadin-grid-column path="value"></vaadin-grid-column>
                                </vaadin-grid-column-group>
                            </vaadin-grid>
                        </div>
                    </div>
                </div>
                <div>
                    <vaadin-button .disabled="${!hasChanged}" @click="${() => this._updatePermission(permission)}">
                        <vaadin-icon icon="font-awesome-solid:floppy-disk" slot="prefix"></vaadin-icon>
                        Save
                    </vaadin-button>
                    <vaadin-button theme="tertiary error" @click="${() => confirm(`Do you want to remove the ${permission.name} permission set?`) && this._deletePermissionSet(permission.name)}">
                        <vaadin-icon icon="font-awesome-solid:trash-can" slot="prefix"></vaadin-icon>
                        Delete
                    </vaadin-button>
                    <vaadin-button .disabled="${!hasChanged}" @click="${() => this._rollback(permission)}" theme="tertiary">
                        <vaadin-icon icon="font-awesome-solid:arrow-rotate-left" slot="prefix"></vaadin-icon>
                        Rollback
                    </vaadin-button>
                </div>
            </div>
            `
  }


  _replaceObjectBy(array, field, idToFind, newObject) {
    // Find the index of the object with the specified id
    const index = array.findIndex(obj => obj[field] === idToFind);

    // If the object is found, replace it with the new object
    if (index !== -1) {
      array[index] = newObject;
    }
  }


  _rollback(permission) {
    let clone = JSON.parse(JSON.stringify(permission.metadata.clone));
    permission = clone;
    permission.metadata.clone = JSON.parse(JSON.stringify(clone));
    this._replaceObjectBy(this._filtered, 'name', permission.name, permission);
    this.requestUpdate();
  }

  _hasChanged(permission) {
    const permissionToCompare = JSON.parse(JSON.stringify(permission))
    const clone = permissionToCompare.metadata.clone
    delete permissionToCompare.metadata;
    delete clone.metadata;
    return !this._equals(permissionToCompare, clone);
  }

  _equals(a, b) {
    if (a === b) return true;

    if (a && b && typeof a == 'object' && typeof b == 'object') {
      if (a.constructor !== b.constructor) return false;

      var length, i, keys;
      if (Array.isArray(a)) {
        length = a.length;
        if (length != b.length) return false;
        for (i = length; i-- !== 0;)
          if (!this._equals(a[i], b[i])) return false;
        return true;
      }

      if (a.constructor === RegExp) return a.source === b.source && a.flags === b.flags;
      if (a.valueOf !== Object.prototype.valueOf) return a.valueOf() === b.valueOf();
      if (a.toString !== Object.prototype.toString) return a.toString() === b.toString();

      keys = Object.keys(a);
      length = keys.length;
      if (length !== Object.keys(b).length) return false;

      for (i = length; i-- !== 0;)
        if (!Object.prototype.hasOwnProperty.call(b, keys[i])) return false;

      for (i = length; i-- !== 0;) {
        var key = keys[i];

        if (!this._equals(a[key], b[key])) return false;
      }

      return true;
    }

    // true if both NaN, false otherwise
    return a !== a && b !== b;
  };

  _getPermissionState(permission, path) {

    let state = 'DEFAULT'; // DEFAULT, ADDED, DELETED, NEW
    let tooltip;

    const defaultState = this._paths.includes(path);
    const deletedState = !permission.paths.includes(path) && permission.metadata.clone.paths.includes(path) && defaultState;
    const newState = !defaultState && !permission.metadata.clone.paths.includes(path);
    const addedState = !newState && !permission.metadata.clone.paths.includes(path) && permission.paths.includes(path);

    if (deletedState) {
      state = 'DELETED';
      tooltip = `The path (${path}) will be deleted`;
    } else if (addedState) {
      state = 'ADDED';
      tooltip = `The path (${path}) will be added`;
    } else if (newState) {
      state = 'NEW';
      tooltip = `The path (${path}) will be created`;
    }

    const clazzToAdd = `status-color-${state.toLowerCase()}`;

    return {
      state, tooltip, clazzToAdd
    };

  }

  _renderDialog() {
    return html`
          <vaadin-vertical-layout style="align-items: stretch; width: 18rem; max-width: 100%;">
              <vaadin-text-field label="Name" .value="${this._newPermissionSet.name}" @value-changed="${(e) => this._newPermissionSet.name = e.detail.value}"></vaadin-text-field>
              <vaadin-select label="Policy" .items="${this._policies}" .value="${this._newPermissionSet.policy}" @value-changed="${e => this._newPermissionSet.policy = e.target.value}"></vaadin-select>
          </vaadin-vertical-layout>
    `;
  }

  _renderFooter() {
    return html`
               <vaadin-button @click="${this._close}">Cancel</vaadin-button>
               <vaadin-button theme="primary" @click="${this._saveNewPermissionSet}">Add</vaadin-button>
             `;
  }

  _close() {
    this._dialogOpened = false;
  }

  _saveNewPermissionSet() {

    if (this._newPermissionSet.name == '') {
      notifier.showErrorMessage("Permission name cannot be empty");
      return
    }

    const permission = {
      enabled: true,
      name: this._newPermissionSet.name,
      policy: this._newPermissionSet.policy,
    }

    this._updatePermission(permission).then(this._close).then(() => this._navigate(this._newPermissionSet.name));
  }

  _dialogTemplate() {
    if (this._dialogOpened) {
      return html`
      <vaadin-dialog header-title="Add new permission" .opened="${this._dialogOpened}" 
        @opened-changed="${(event) => this._dialogOpened = event.detail.value}" 
        ${dialogRenderer(this._renderDialog, [])} 
        ${dialogFooterRenderer(this._renderFooter, [])}>
      </vaadin-dialog>
      `
    }
  }

  _showDialog() {
    this._newPermissionSet = {
      name: '',
      policy: 'deny'
    };
    this._dialogOpened = true;
    this.requestUpdate();
  }

  _generateId(name) {
    return `${name}-${Math.random().toString(36).substr(2, 10)}`;
  }

  _rowRender(path, model, column, permission) {

    const tooltipId = this._generateId(permission.name + 'tooltipPathId');
    const actionTooltipId = this._generateId(permission.name + 'tooltipActionId');
    const { state, tooltip, clazzToAdd } = this._getPermissionState(permission, path);
    let actionIcon = 'trash-can';
    let actionTooltip = 'Delete';
    let actionColor = 'inherit';
    if (state === 'DELETED') {
      actionIcon = 'arrow-rotate-left';
      actionTooltip = 'Restore';
      actionColor = 'green';
    }
    return html`
        <div class="row-path ${clazzToAdd}">
            ${state === 'DEFAULT' ? '' : html`<vaadin-tooltip for="${tooltipId}" text="${tooltip}" position="top-start"></vaadin-tooltip>`}
            <span id="${tooltipId}">${path}</span>
            <vaadin-tooltip for="${actionTooltipId}" text="${actionTooltip}" position="top-start"></vaadin-tooltip>
            <vaadin-button id="${actionTooltipId}" @click="${() => {
        permission.paths = state === 'DELETED' ? [path, ...permission.paths] : permission.paths.filter(p => p !== path);
        permission.metadata.allPaths = [...permission.metadata.allPaths]; // change the reference so it will render
        this.requestUpdate();
        }}" theme="tertiary error small">
                <vaadin-icon style="color:${actionColor}" icon="font-awesome-solid:${actionIcon}" slot="prefix"></vaadin-icon>
            </vaadin-button>
        </div>
                `
  }

  _deletePermissionSet(name) {
    return this.jsonRpc.deletePermissionSet({
      name: name
    }).then(e => {
      [this._allPermissions, this._filtered].forEach(permissions => permissions = permissions.filter(p => p.name !== name))
      notifier.showInfoMessage(`Permission ${name} deleted`);
      this.requestUpdate();
    });

  }

  _updatePermission(permission) {
    return this.jsonRpc.updatePermissionSet({
      permissionValues: {
        id: permission.name,
        values: {
          PERMISSION_ENABLED: permission.enabled ? "" : false,
          PERMISSION_METHODS: permission.methods,
          PERMISSION_POLICY: permission.policy,
          PERMISSION_PATHS: permission.paths,
          PERMISSION_AUTH_MECHANISM: (permission.authMechanism === 'QuarkusNullValue' ? "" : permission.authMechanism)
        }
      }
    })
      .then(e => {
        this._parseResponse(e);
        const permissionFound = this._allPermissions.find(p => p.name === permission.name);
        const permissionToAddOrReplace = e.result.permissions.find(p => p.name === permission.name);
        if (permissionFound) {
          this._replaceObjectBy(this._allPermissions, 'name', permission.name, permissionToAddOrReplace);
          this._replaceObjectBy(this._filtered, 'name', permission.name, permissionToAddOrReplace);
          notifier.showInfoMessage(`Permission ${permission.name} updated`);
        } else {
          this._allPermissions.push(permissionToAddOrReplace);
          this._filtered.push(permissionToAddOrReplace);
          notifier.showInfoMessage(`Permission ${permission.name} created`);
        }
        this.requestUpdate();
      });

  }

}

customElements.define('qwc-auth', QwcAuthConfiguration);
