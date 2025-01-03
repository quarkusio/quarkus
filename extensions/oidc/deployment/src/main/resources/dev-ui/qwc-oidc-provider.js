import { QwcHotReloadElement, html, css} from 'qwc-hot-reload-element';
import {classMap} from 'lit/directives/class-map.js';
import {unsafeHTML} from 'lit/directives/unsafe-html.js';
import {JsonRpc} from 'jsonrpc';
import { LitState } from 'lit-element-state';
import '@vaadin/button';
import '@vaadin/details';
import '@vaadin/horizontal-layout';
import '@vaadin/icon';
import '@vaadin/message-list';
import '@vaadin/password-field';
import '@vaadin/split-layout';
import { notifier } from 'notifier';
import { Router } from '@vaadin/router';
import {
    devRoot
} from 'build-time-data';

/**
 * This keeps state of OIDC properties that can potentially change on hot reload.
 */
class OidcPropertiesState extends LitState {

    static get stateVars() {
        return {
            hideLogInErr: false,
            hideImplicitLoggedIn: false,
            hideImplLoggedOut: false,
            swaggerUiPath: null,
            graphqlUiPath: null,
            oidcProviderName: null,
            oidcApplicationType: null,
            oidcGrantType: null,
            swaggerIsAvailable: false,
            graphqlIsAvailable: false,
            introspectionIsAvailable: null,
            keycloakAdminUrl: null,
            keycloakRealms: null,
            clientId: null,
            clientSecret: null,
            authorizationUrl: null,
            tokenUrl: null,
            logoutUrl: null,
            postLogoutUriParam: null,
            scopes: null,
            authExtraParams: null,
            httpPort: 8080,
            accessToken: null,
            idToken: null,
            userName: null,
            propertiesStateId: null,
            testServiceResponses: null
        };
    }

    static updateProperties(response) {
        if (response.result && response.result.propertiesStateId !== propertiesState.propertiesStateId) {

            // here we keep knowledge of last properties state id in session storage as a safety measure - if we
            // already have matching id stored in the storage, then we know there is no reason to remove query
            // params, thus we keep user 'logged'
            // goal is to address 'very hard to reproduce' scenario when Quarkus app is hot reloaded, user log in to
            // Keycloak get redirected back and hot reload is called again; we can stop storing id in sessionStorage
            // when we are confident it won't happen again, however it is simple and it prevents ugly behavior
            const propertiesStateDontMatchesSessionState = sessionStorage.getItem('oidcPropertiesStateId')
                !== response.result.propertiesStateId;
            const logout = propertiesStateDontMatchesSessionState && response.result.alwaysLogoutUserInDevUiOnReload;
            sessionStorage.setItem('oidcPropertiesStateId', response.result.propertiesStateId);

            this.clearTestServiceResponses();
            propertiesState.clientId = response.result.clientId;
            propertiesState.clientSecret = response.result.clientSecret;
            propertiesState.authorizationUrl = response.result.authorizationUrl;
            propertiesState.tokenUrl = response.result.tokenUrl;
            propertiesState.logoutUrl = response.result.logoutUrl;
            propertiesState.postLogoutUriParam = response.result.postLogoutUriParam;
            propertiesState.scopes = response.result.scopes;
            propertiesState.authExtraParams = response.result.authExtraParams;
            propertiesState.httpPort = response.result.httpPort;
            propertiesState.oidcProviderName = response.result.oidcProviderName;
            propertiesState.oidcApplicationType = response.result.oidcApplicationType;
            propertiesState.oidcGrantType = response.result.oidcGrantType;
            propertiesState.swaggerIsAvailable = response.result.swaggerIsAvailable;
            propertiesState.graphqlIsAvailable = response.result.graphqlIsAvailable;
            propertiesState.introspectionIsAvailable = response.result.introspectionIsAvailable;
            propertiesState.keycloakAdminUrl = response.result.keycloakAdminUrl;
            propertiesState.keycloakRealms = response.result.keycloakRealms;
            propertiesState.swaggerUiPath = response.result.swaggerUiPath;
            propertiesState.graphqlUiPath = response.result.graphqlUiPath;

            return {
                // logout === true will trigger query params removal
                logout,
                // we try to update properties state id together with tokens to limit repeated rendering
                onUpdateFinish: () => propertiesState.propertiesStateId = response.result.propertiesStateId
            };
        }

        // nothing changed, do nothing
        return {
            logout: false,
            onUpdateFinish: () => {}
        };
    }

    static updateTestServiceResponses(newTestServiceResponse) {
        // don't push changes, we want to change array to a new one so that component is rendered again
        propertiesState.testServiceResponses = [...propertiesState.testServiceResponses, newTestServiceResponse];
    }

    static clearTestServiceResponses() {
        propertiesState.testServiceResponses = [];
    }
}

export const propertiesState = new OidcPropertiesState();

export class QwcOidcProvider extends QwcHotReloadElement {

    static ERROR_DESCRIPTION = 'error_description';

    static styles = css`
        .full-height {
          height: 100%;
        }
        .full-width {
          width: 100%;
        }
        
        .container {
          width: 93%;
          margin: auto;
          align-items: stretch;
        }
        
        .frm-field {
          width: 83.333333%;
          margin-left: 20px;
        }
        .txt-field-form {
          text-align: end;
        }
        .btn-icon {
          width: var(--lumo-font-size-m);
          height: var(--lumo-font-size-m);
          margin-top: -4px;
        }
        .keycloak-btn {
          align-self: flex-start;
        }
        .hidden {
          display: none;
        }
        .heading {
            font-size: larger;
        }
        .error-color {
          color: var(--lumo-error-text-color);
        }
        .vertical-center {
          align-items: center;
        }
        .margin-left-auto {
          margin-left: auto;
        }
        .primary-color {
          color: var(--lumo-primary-text-color);
        }
        .black-5pct {
          background-color: var(--lumo-contrast-10pct);
        }
        .margin-l-m {
          margin-left: var(--lumo-space-m);
        }
        .fill-space {
          flex-grow: 1;
        }
        .margin-right-auto {
          margin-right: auto;
        }
        .default-cursor {
          cursor: default;
        }
        .display-none {
          display: none;
        }
        .test-svc-msg-list {
          margin-left: calc(16.6667% - var(--lumo-space-m));
          --vaadin-user-color-1: var(--lumo-primary-background-color);
        }
        vaadin-details-summary::part(toggle), vaadin-details-summary::part(content) {
          color: var(--lumo-primary-text-color);
        }
        vaadin-details-summary::part(content) {
          width: 100%;
        }
        .token-detail {
          margin-left: var(--lumo-space-m);
          margin-right: var(--lumo-space-m);
          .token-detail-summary {
            display: flex; 
            align-items: baseline;
          }
        }
        .decoded-token, .encoded-token {
          padding: 0 var(--lumo-space-m);
          word-break: break-word;
          word-wrap: break-word;
          background-color: var(--lumo-contrast-5pct);
        }
        .decoded-token pre {
          white-space: break-spaces;
        }
        .token-payload {
          color: var(--lumo-success-color);
        }
        .token-headers {
          color: var(--lumo-error-color);
        }
        .token-signature {
          color: var(--quarkus-blue);
        }
        .token-encryption {
          color: var(--quarkus-blue);
        }
        .margin-top-space-m {
          margin-top: var(--lumo-space-m);
        }
        .margin-right-space-m {
          margin-right: var(--lumo-space-m);
        }
        .half-width {
          width: 50%;
        }
        .jwt-tooltip-bg {
            background: rgba(0, 0, 0, .1);
        }
        .jwt-tooltip-cursor {
          cursor: url("data:image/svg+xml,%3Csvg height='0.8rem' width='0.8rem' fill='%23000000' viewBox='0 0 318.293 318.293' xml:space='preserve' xmlns='http://www.w3.org/2000/svg' xmlns:svg='http://www.w3.org/2000/svg'%3E%3Cg%3E%3Cpath d='M 159.148,0 C 106.452,0 63.604,39.326 63.604,87.662 h 47.736 c 0,-22.007 21.438,-39.927 47.808,-39.927 26.367,0 47.804,17.92 47.804,39.927 v 6.929 c 0,23.39 -10.292,34.31 -25.915,50.813 -20.371,21.531 -45.744,48.365 -45.744,105.899 h 47.745 c 0,-38.524 15.144,-54.568 32.692,-73.12 17.368,-18.347 38.96,-41.192 38.96,-83.592 V 87.662 C 254.689,39.326 211.845,0 159.148,0 Z' style='fill:%234087d4;fill-opacity:1' /%3E%3Crect x='134.475' y='277.996' width='49.968' height='40.297' style='fill:%234087d4;fill-opacity:1' /%3E%3C/g%3E%3C/svg%3E"), help;
        }
    `;

    jsonRpc = new JsonRpc(this);

    static properties = {
        _listeningToRouterLocationChanged: {state: false, type: Boolean},
        _selectedRealm: {state: false, type: String},
        _passwordGrantPwd: {state: false, type: String},
        _passwordGrantUsername: {state: false, type: String},
        _selectedClientId: {state: false, type: String},
        _selectedClientSecret: {state: false, type: String},
        _servicePath: {state: false, type: String},
        _devRoot: {state: false, type: String}
    };

    constructor() {
        super();
        this._devRoot = (devRoot?.replaceAll('/', '%2F') ?? '') + 'dev-ui'; // e.g. /q/dev-ui

        this._selectedRealm = null;
        this._servicePath = '/';
        this._selectedClientId = null;
        this._selectedClientSecret = null;
        this._passwordGrantPwd = '';
        this._passwordGrantUsername = '';

        if (!this._listeningToRouterLocationChanged) {
            this._listeningToRouterLocationChanged = true;

            // when we show/hide elements based on existence of query params, we can't manage internal state without managing
            // query params (AKA we need to always update query params), however route rendering is async operation
            // and this component constructor is invoked before even location changed, we don't have
            // a choice but listen for location change between component instances and never remove listener
            // TODO: we should watch 'qwc-menu.js' and 'qwc-head.js' that also use the listener if they find a better to do this
            window.addEventListener('vaadin-router-location-changed', evt => {
                // I'm a bit worried what would happen if we were listening but route already changed to another
                // extension (if that's possible?) so let's check this is our extension that is displayed
                if (window.location.pathname.includes('io.quarkus.quarkus-oidc')) {
                    QwcOidcProvider._updateQueryParamsProperties(this.jsonRpc, () => this.requestUpdate());
                }
            });
        }
    }

    connectedCallback() {
        // we want to render component in one of 4 situations:
        // - id token or access token changed
        // - properties state id changed
        // - query params or hash params changed
        // - test service results changed
        // we try update them in a same time so that changes are more likely to be handled by same next scheduled update

        this.alwaysUpdatePropertiesStateObserver = () => this.requestUpdate();
        propertiesState.addObserver(this.alwaysUpdatePropertiesStateObserver, 'propertiesStateId');

        // in situation when properties state id is null, there is no point to request rendering as props are not loaded
        this.conditionalUpdatePropertiesStateObserver = () => {
            if (propertiesState.propertiesStateId) {
                this.requestUpdate();
            }
        }
        propertiesState.addObserver(this.conditionalUpdatePropertiesStateObserver, 'idToken');
        propertiesState.addObserver(this.conditionalUpdatePropertiesStateObserver, 'testServiceResponses');
        propertiesState.addObserver(this.conditionalUpdatePropertiesStateObserver, 'accessToken');

        super.connectedCallback();
        QwcOidcProvider._loadProperties(this.jsonRpc)
            .then(result => QwcOidcProvider._updateQueryParamsProperties(this.jsonRpc, result.onUpdateFinish));
    }

    disconnectedCallback() {
        propertiesState.removeObserver(this.alwaysUpdatePropertiesStateObserver);
        propertiesState.removeObserver(this.conditionalUpdatePropertiesStateObserver);
        super.disconnectedCallback();
    }

    hotReload(){
        propertiesState.propertiesStateId = null;
        OidcPropertiesState.clearTestServiceResponses();
        QwcOidcProvider._loadProperties(this.jsonRpc).then(result => {
            if (result.logout && window.location.search?.length > 1) {
                // we need to remove code, state and so on from URL
                // this shouldn't lead to re-creating of the component as we don't really change path
                // but event listener will invoke '_updateQueryParamsProperties' for us
                result.onUpdateFinish();
                Router.go({pathname: window.location.pathname});
            } else {
                QwcOidcProvider._updateQueryParamsProperties(this.jsonRpc, result.onUpdateFinish);
            }
        });
    }

    static _loadProperties(jsonRpc) {
        return jsonRpc.getProperties()
            .then(response => OidcPropertiesState.updateProperties(response))
            .catch(response => {
                notifier.showErrorMessage('Failed to request runtime properties. Error message: '
                    + response?.error?.message, 'top-end');
            });
    }

    render() {
        if (propertiesState.propertiesStateId) {
            return this._renderProvider();
        } else {
            return html`
            <div class="container" style="color: var(--lumo-secondary-text-color);border: 0">
                <div>Loading...</div>
                <vaadin-progress-bar indeterminate></vaadin-progress-bar>
            </div>`;
        }
    }

    _renderProvider() {
        if (QwcOidcProvider._isServiceOrHybridApp()) {
            switch (propertiesState.oidcGrantType) {
                case 'password':
                    return this._passwordGrantTypeCard();
                case 'client_credentials':
                    return this._clientCredentialsCard();
                case 'implicit':
                    return this._implicitOrCodeGrantTypeCard();
                case 'code':
                    return this._implicitOrCodeGrantTypeCard();
                default:
                    return html``;
            }
        } else {
            return this._webAppLoginCard();
        }
    }

    _webAppLoginCard() {
        const servicePathForm = this._servicePathForm();
        return html`
            <vaadin-vertical-layout theme="spacing padding" class="height-4xl container" 
                                    ?hidden="${propertiesState.hideImplLoggedOut}">
                ${servicePathForm}
                <vaadin-vertical-layout class="margin-left-auto frm-field">
                    <vaadin-button theme="primary success" class="full-width"
                                   @click=${() => this._signInToService()}>
                        Log into your Web Application
                    </vaadin-button>
                </vaadin-vertical-layout>
            </vaadin-vertical-layout>
            ${this._displayTokenCard()}
        `;
    }

    _signInToService() {
        window.open("http://localhost:" + propertiesState.httpPort + this._servicePath);
    }

    static _isServiceOrHybridApp() {
        return propertiesState.oidcApplicationType === 'service' || propertiesState.oidcApplicationType === 'hybrid';
    }

    _passwordGrantTypeCard() {
        return this._testServiceForm(
            () => this._testServiceWithPassword(),
            () => this._testServiceWithPasswordInSwaggerUi(),
            () => this._testServiceWithPasswordInGraphQLUi(),
            'Get access token and test your service',
            html`
                <vaadin-form-layout class="txt-field-form full-width">
                    <vaadin-form-item class="full-width">
                        <label slot="label">User name</label>
                        <vaadin-text-field class="frm-field" title="User" value="" required
                                           @value-changed="${e => {
                                               this._passwordGrantUsername = (e.detail?.value || '').trim();
                                           }}"></vaadin-text-field>
                    </vaadin-form-item>
                </vaadin-form-layout>
                <vaadin-form-layout class="txt-field-form full-width">
                    <vaadin-form-item class="full-width">
                        <label slot="label">Password</label>
                        <vaadin-password-field class="frm-field" title="Password" value=""
                                               @value-changed="${e => {
                                                   this._passwordGrantPwd = (e.detail?.value || '').trim();
                                               }}"></vaadin-password-field>
                    </vaadin-form-item>
                </vaadin-form-layout>
            `
        );
    }

    _testServiceForm(testSvcFun, testSvcSwaggerFun, testSvcGraphQlFun, title, extraFields) {
        const keycloakRealms = this._keycloakRealmsForm(true);
        const servicePathForm = this._servicePathForm();
        const testServiceResultsHtml = QwcOidcProvider._testServiceResultsHtml();
        return html`
        <vaadin-vertical-layout theme="spacing" class="height-4xl container margin-top-space-m">
                    <vaadin-horizontal-layout class="black-5pct vertical-center" theme="padding">
                        <span class="default-cursor">${title}</span>
                    </vaadin-horizontal-layout>
                    <vaadin-vertical-layout theme="padding">
                        ${keycloakRealms}
                        ${extraFields}
                        ${servicePathForm}
                        <vaadin-horizontal-layout class="margin-left-auto frm-field">
                            <vaadin-horizontal-layout class="full-width">
                                <vaadin-button class="fill-space margin-right-auto" theme="primary" title="Test service" 
                                               @click=${testSvcFun}>
                                    Test service
                                </vaadin-button>
                                <vaadin-button theme="tertiary" class="margin-l-m" title="Test in Swagger UI"
                                               @click=${testSvcSwaggerFun} 
                                               ?hidden="${!propertiesState.swaggerIsAvailable}">
                                    <vaadin-icon icon="font-awesome-solid:up-right-from-square" slot="prefix" 
                                                 class="btn-icon"></vaadin-icon>
                                    Swagger UI
                                </vaadin-button>
                                <vaadin-button theme="tertiary" class="margin-l-m" title="Test in GraphQL UI"
                                               @click=${testSvcGraphQlFun} 
                                               ?hidden="${!propertiesState.graphqlIsAvailable}">
                                    <vaadin-icon icon="font-awesome-solid:up-right-from-square" slot="prefix" 
                                                 class="btn-icon"></vaadin-icon>
                                    GraphQL UI
                                </vaadin-button>
                            </vaadin-horizontal-layout>
                        </vaadin-horizontal-layout>
                        ${testServiceResultsHtml}
                    </vaadin-vertical-layout>
                </vaadin-vertical-layout>
        `;
    }

    _testServiceWithPassword() {
        const servicePath = this._servicePath;
        const username = this._passwordGrantUsername;
        QwcOidcProvider._handleTestServiceResponse(
            this.jsonRpc
                .testServiceWithPassword({
                    tokenUrl: this._getTokenUrl(),
                    serviceUrl: QwcOidcProvider._toServiceUrl(servicePath),
                    clientId: this._getClientId(),
                    clientSecret: this._getClientSecret(),
                    username,
                    password: this._passwordGrantPwd
                }),
            username,
            servicePath
        );
    }

    static _handleTestServiceResponse(reqPromise, headline, servicePath) {
        reqPromise
            .then(response => {
                if (response.result) {
                    QwcOidcProvider._addResponseData(response.result, headline, servicePath);
                } else {
                    // illegal state, won't happen unless there is a bug in JSON RPC client
                    QwcOidcProvider._addResponseData('failed, please check application log', headline, servicePath);
                }
            })
            .catch(QwcOidcProvider._logFailedServiceTest);
    }

    static _toServiceUrl(servicePath) {
        return "http://localhost:" + propertiesState.httpPort + servicePath;
    }

    _testServiceWithPasswordInSwaggerUi() {
        this.jsonRpc
            .testServiceWithPassword({
                tokenUrl: this._getTokenUrl(),
                serviceUrl: null,
                clientId: this._getClientId(),
                clientSecret: this._getClientSecret(),
                username: this._passwordGrantUsername,
                password: this._passwordGrantPwd
            })
            .then(response => {
                if (response.result) {
                    QwcOidcProvider._navigateToSwaggerUiWithToken(response.result);
                }
            })
            .catch(QwcOidcProvider._logFailedServiceTest);
    }

    _testServiceWithPasswordInGraphQLUi() {
        this.jsonRpc
            .testServiceWithPassword({
                tokenUrl: this._getTokenUrl(),
                serviceUrl: null,
                clientId: this._getClientId(),
                clientSecret: this._getClientSecret(),
                username: this._passwordGrantUsername,
                password: this._passwordGrantPwd
            })
            .then(response => {
                if (response.result) {
                    QwcOidcProvider._navigateToGraphQLUiWithToken(response.result);
                }
            })
            .catch(QwcOidcProvider._logFailedServiceTest);
    }

    _testServiceWithClientCredentials() {
        const servicePath = this._servicePath;
        QwcOidcProvider._handleTestServiceResponse(
            this.jsonRpc
                .testServiceWithClientCred({
                    tokenUrl: this._getTokenUrl(),
                    serviceUrl: QwcOidcProvider._toServiceUrl(servicePath),
                    clientId: this._getClientId(),
                    clientSecret: this._getClientSecret()
                }),
            'Client credentials',
            servicePath
        );
    }

    _testServiceWithClientCredentialsInSwaggerUi() {
        this.jsonRpc
            .testServiceWithClientCred({
                tokenUrl: this._getTokenUrl(),
                serviceUrl: null,
                clientId: this._getClientId(),
                clientSecret: this._getClientSecret()
            })
            .then(response => {
                if (response.result) {
                    QwcOidcProvider._navigateToSwaggerUiWithToken(response.result);
                }
            })
            .catch(QwcOidcProvider._logFailedServiceTest);
    }

    _testServiceWithClientCredentialsInGraphQLUi() {
        this.jsonRpc
            .testServiceWithClientCred({
                tokenUrl: this._getTokenUrl(),
                serviceUrl: null,
                clientId: this._getClientId(),
                clientSecret: this._getClientSecret()
            })
            .then(response => {
                if (response.result) {
                    QwcOidcProvider._navigateToGraphQLUiWithToken(response.result);
                }
            })
            .catch(QwcOidcProvider._logFailedServiceTest);
    }

    static _logFailedServiceTest(response) {
        notifier.showErrorMessage('Failed to test service. Error message: '
            + response?.error?.message, 'top-end');
    }

    _clientCredentialsCard() {
        return this._testServiceForm(
            () => this._testServiceWithClientCredentials(),
            () => this._testServiceWithClientCredentialsInSwaggerUi(),
            () => this._testServiceWithClientCredentialsInGraphQLUi(),
            'Get access token for the client and test your service',
            html``
        );
    }

    _keycloakRealmsForm(showSecret = false) {
        if (propertiesState.keycloakAdminUrl && propertiesState.keycloakRealms?.length > 0) {
            const realms = [];
            propertiesState.keycloakRealms.forEach((element) => realms.push({'label': element, 'value': element}));
            let secret = html``;
            if (showSecret) {
                secret = html`
                    <vaadin-form-layout class="txt-field-form full-width">
                        <vaadin-form-item class="full-width">
                            <label slot="label">Secret</label>
                            <vaadin-password-field class="frm-field" title="Secret"
                                    @value-changed="${e => {
                                        this._selectedClientSecret = (e.detail?.value || '').trim();
                                    }}"
                                    value="${propertiesState.keycloakRealms?.length === 1 ? (propertiesState.clientSecret ?? '') : ''}">
                            </vaadin-password-field>
                        </vaadin-form-item>
                    </vaadin-form-layout>
                `;
            }
            return html`
                <vaadin-form-layout class="txt-field-form full-width">
                    <vaadin-form-item class="full-width">
                        <label slot="label">Realm</label>
                        <vaadin-select class="frm-field"
                                .items="${realms}"
                                .value="${propertiesState.keycloakRealms[0]}"
                                @value-changed="${e => this._selectedRealm = e.detail?.value}"
                        ></vaadin-select>
                    </vaadin-form-item>
                </vaadin-form-layout>
                <vaadin-form-layout class="txt-field-form full-width">
                    <vaadin-form-item class="full-width">
                        <label slot="label">Client</label>
                        <vaadin-text-field class="frm-field" title="Client ID"
                                           @value-changed="${e => {
                                                this._selectedClientId = (e.detail?.value || '').trim();
                                            }}"
                                           value="${propertiesState.keycloakRealms?.length === 1 ? (propertiesState.clientId ?? '') : ''}">
                        </vaadin-text-field>
                    </vaadin-form-item>
                </vaadin-form-layout>
                ${secret}
            `;
        } else {
            return html``;
        }
    }

    _implicitOrCodeGrantTypeCard() {
        const keycloakRealms = this._keycloakRealmsForm();

        return html`
            <vaadin-vertical-layout theme="spacing padding" class="height-4xl container" 
                                    ?hidden="${propertiesState.hideImplLoggedOut}">
                ${keycloakRealms}
                <vaadin-form-layout class="margin-left-auto txt-field-form frm-field">
                        <vaadin-form-item class="full-width">
                            <vaadin-button theme="primary success" class="full-width"
                                        title="Log into Single Page Application to Get Access and ID Tokens"
                                        @click=${() => this._signInToOidcProviderAndGetTokens()}>
                                <vaadin-icon icon="font-awesome-solid:user" slot="prefix" class="btn-icon"></vaadin-icon>
                                Log into Single Page Application
                            </vaadin-button>            
                        </vaadin-form-item>
                    </vaadin-form-layout>

                
            </vaadin-vertical-layout>
            <vaadin-horizontal-layout theme="spacing padding" class="height-4xl container vertical-center" 
                                      ?hidden="${propertiesState.hideLogInErr}">
                <vaadin-icon icon="font-awesome-regular:circle-xmark" class="error-color"></vaadin-icon>
                <span class="primary-color">${QwcOidcProvider._getQueryParameter(QwcOidcProvider.ERROR_DESCRIPTION)}</span>
                <vaadin-button theme="tertiary small" title="Click to start again" class="margin-left-auto"
                               @click=${() => QwcOidcProvider._showLoginToSpa()}>
                    <vaadin-icon icon="font-awesome-solid:right-from-bracket" slot="prefix" class="btn-icon">
                    </vaadin-icon>
                </vaadin-button>
            </vaadin-horizontal-layout>
            ${this._displayTokenCard()}
        `;
    }

    _displayTokenCard() {
        const servicePathForm = this._servicePathForm();
        const testServiceResultsHtml = QwcOidcProvider._testServiceResultsHtml();
        return html`
            <vaadin-vertical-layout class="full-width" ?hidden="${propertiesState.hideImplicitLoggedIn}">
                <vaadin-vertical-layout class="height-4xl container">
                    <vaadin-horizontal-layout class="black-5pct vertical-center" theme="padding">
                        <span class="margin-right-auto default-cursor heading">Your tokens</span>
                        <span class="margin-right-space-m ${classMap({'display-none': !propertiesState.userName})}">
                            Logged in as ${propertiesState.userName}</span>
                        <vaadin-button theme="tertiary small" title="Click to logout and start again" 
                                       @click=${() => this._logout()} ?hidden="${!propertiesState.logoutUrl}">
                            <vaadin-icon icon="font-awesome-solid:up-right-from-square" slot="prefix" class="btn-icon">
                            </vaadin-icon>
                        </vaadin-button>
                    </vaadin-horizontal-layout>
                    <vaadin-details class="token-detail">
                        <vaadin-details-summary slot="summary">
                            <div class="token-detail-summary">
                                <span class="margin-right-auto">View Access Token</span>
                                <vaadin-button theme="tertiary small" title="Copy to clipboard"
                                               @click=${QwcOidcProvider._copyAccessTokenToClipboard}>
                                    <vaadin-icon icon="font-awesome-solid:clipboard" slot="prefix" class="btn-icon">
                                    </vaadin-icon>
                                </vaadin-button>
                            </div>
                        </vaadin-details-summary>
                        <div>
                            <vaadin-split-layout theme="minimal">
                                <div class="encoded-token half-width">
                                    <h3>Encoded</h3>
                                    <p>
                                        ${QwcOidcProvider._prettyToken(propertiesState.accessToken)}
                                    </p>
                                </div>
                                <div class="decoded-token half-width">
                                    <h3>Decoded</h3>
                                    <p>
                                        ${QwcOidcProvider._decodeToken(propertiesState.accessToken)}
                                    </p>
                                </div>
                            </vaadin-split-layout>
                        </div>
                    </vaadin-details>
                    <vaadin-details class="token-detail">
                        <vaadin-details-summary slot="summary">
                            <div class="token-detail-summary">
                                <span class="margin-right-auto">View ID Token</span>
                                <vaadin-button theme="tertiary small" title="Copy to clipboard"
                                               @click=${QwcOidcProvider._copyIdTokenToClipboard}>
                                    <vaadin-icon icon="font-awesome-solid:clipboard" slot="prefix" class="btn-icon">
                                    </vaadin-icon>
                                </vaadin-button>
                            </div>
                        </vaadin-details-summary>
                        <div>
                            <vaadin-split-layout theme="minimal">
                                <div class="encoded-token half-width">
                                    <h3>Encoded</h3>
                                    <p>
                                        ${QwcOidcProvider._prettyToken(propertiesState.idToken)}
                                    </p>
                                </div>
                                <div class="decoded-token half-width">
                                    <h3>Decoded</h3>
                                    <p>
                                        ${QwcOidcProvider._decodeToken(propertiesState.idToken)}
                                    </p>
                                </div>
                            </vaadin-split-layout>
                        </div>
                    </vaadin-details>
                </vaadin-vertical-layout>
                <vaadin-vertical-layout theme="spacing" class="height-4xl container margin-top-space-m">
                    <vaadin-horizontal-layout class="black-5pct vertical-center" theme="padding">
                        <span class="margin-right-auto default-cursor heading">Test your service</span>
                        <vaadin-button theme="tertiary small" title="Test in Swagger UI" 
                                       @click=${() => QwcOidcProvider._navigateToSwaggerUi()} 
                                       ?hidden="${!propertiesState.swaggerIsAvailable}">
                            <vaadin-icon icon="font-awesome-solid:up-right-from-square" slot="prefix" class="btn-icon">
                            </vaadin-icon>
                            Swagger UI
                        </vaadin-button>
                        <vaadin-button theme="tertiary small" class="margin-l-m" title="Test in GraphQL UI"
                                       @click=${() => QwcOidcProvider._navigateToGraphQLUi()} 
                                       ?hidden="${!propertiesState.graphqlIsAvailable}">
                            <vaadin-icon icon="font-awesome-solid:up-right-from-square" slot="prefix" class="btn-icon">
                            </vaadin-icon>
                            GraphQL UI
                        </vaadin-button>
                    </vaadin-horizontal-layout>
                    <vaadin-vertical-layout theme="padding">
                        ${servicePathForm}
                        <vaadin-horizontal-layout class="full-width">
                            <vaadin-horizontal-layout class="margin-left-auto frm-field">
                                <vaadin-button class="fill-space" theme="primary" title="Test With Access Token" 
                                               @click=${() => this._testServiceWithAccessToken()}>
                                    With Access Token
                                </vaadin-button>
                                <vaadin-button class="fill-space margin-l-m" theme="primary" title="Test With ID Token" 
                                               @click=${() => this._testServiceWithIdToken()}>
                                    With ID Token
                                </vaadin-button>
                            </vaadin-horizontal-layout>
                        </vaadin-horizontal-layout>
                        ${testServiceResultsHtml}
                    </vaadin-vertical-layout>
                </vaadin-vertical-layout>
            </vaadin-vertical-layout>
        `;
    }

    _servicePathForm() {
        return html`
            <vaadin-form-layout class="txt-field-form full-width">
                <vaadin-form-item class="full-width">
                    <label slot="label">Service path</label>
                    <vaadin-text-field class="frm-field"
                                       value="/"
                                       @value-changed="${e => {
            this._servicePath = (e.detail?.value || '').trim();
            if (!this._servicePath.startsWith('/')) {
                this._servicePath = '/' + this._servicePath;
            }
        }}">
                    </vaadin-text-field>
                </vaadin-form-item>
            </vaadin-form-layout>
        `;
    }

    static _testServiceResultsHtml() {
        return html`
            <vaadin-horizontal-layout class="full-width">
                <vaadin-message-list class="test-svc-msg-list" .items="${propertiesState.testServiceResponses}">
                </vaadin-message-list>
                <vaadin-button theme="tertiary small" title="Clear results" class="margin-left-auto"
                               @click=${() => OidcPropertiesState.clearTestServiceResponses()}>
                    <vaadin-icon icon="font-awesome-solid:eraser" slot="prefix" class="btn-icon">
                    </vaadin-icon>
                </vaadin-button>
            </vaadin-horizontal-layout>
        `;
    }

    _testServiceWithAccessToken(){
        this._testServiceWithToken(propertiesState.accessToken, "Access Token");
    }

    _testServiceWithToken(token, tokenType){
        const servicePath = this._servicePath;
        QwcOidcProvider._handleTestServiceResponse(
            this.jsonRpc
                .testServiceWithToken({
                    serviceUrl: QwcOidcProvider._toServiceUrl(servicePath),
                    token
                }),
            tokenType,
            servicePath
        )
    }

    static _addResponseData(statusCode, headline, servicePath) {
        let icon;
        if (statusCode && ("" + statusCode).startsWith("2")) {
            icon = QwcOidcProvider._getFontAwesomeSuccessSvgAsImgSrc();
        } else {
            icon = QwcOidcProvider._getFontAwesomeFailureSvgAsImgSrc();
        }
        OidcPropertiesState.updateTestServiceResponses({
            text: 'service path: ' + servicePath + ', result : ' + statusCode,
            time: new Date().toLocaleString(),
            userName: headline,
            userImg: icon,
            userColorIndex: 1
        });
    }

    _testServiceWithIdToken(){
        this._testServiceWithToken(propertiesState.idToken, "ID Token")
    }

    static _showLoginToSpa() {
        // we can't just change internal state here, as when component re-render, changes would be lost
        // because we show/hide elements based on query args, we need to get rid of error description
        const pathname = window.location.pathname;
        const search = QwcOidcProvider._getQueryParamsWithoutErrorDesc();
        Router.go({pathname, search});
    }

    _signInToOidcProviderAndGetTokens() {
        const clientId = this._getClientId();
        const scopes = propertiesState.scopes ?? '';
        const authExtraParams = propertiesState.authExtraParams ?? '';

        let address;
        let state;
        if (propertiesState.keycloakAdminUrl && propertiesState.keycloakRealms?.length > 0) {
            address = this._getKeycloakAuthorizationUrl();
            state = QwcOidcProvider._makeId() + "_" + this._selectedRealm + "_" + clientId;
        } else {
            address = propertiesState.authorizationUrl ?? '';
            state = QwcOidcProvider._makeId();
        }

        let responseType;
        if (propertiesState.oidcGrantType === 'implicit') {
            responseType = "token id_token";
        } else {
            responseType = "code";
        }

        window.location.href = address
            + "?client_id=" + clientId
            + "&redirect_uri=" + this._getEncodedPath()
            + "&scope=" + scopes + "&response_type=" + responseType
            + "&response_mode=query&prompt=login&nonce=" + QwcOidcProvider._makeId()
            + "&state=" + state
            + authExtraParams;
    }

    _getEncodedPath() {
        // this is the last part of this path: /q/dev-ui/io.quarkus.quarkus-oidc/keycloak-provider -> keycloak-provider
        const subPath = window.location.pathname.substring(window.location.pathname.lastIndexOf('/') + 1);
        return "http%3A%2F%2Flocalhost%3A" + propertiesState.httpPort + this._devRoot
            + "%2Fio.quarkus.quarkus-oidc%2F" + subPath;
    }

    _getClientId() {
        if (propertiesState.keycloakAdminUrl && propertiesState.keycloakRealms?.length > 0) {
            return this._selectedClientId;
        } else {
            return propertiesState.clientId;
        }
    }

    _getClientSecret() {
        if (propertiesState.keycloakAdminUrl && propertiesState.keycloakRealms?.length > 0) {
            return this._selectedClientSecret;
        } else {
            return propertiesState.clientSecret;
        }
    }

    static _makeId() {
        let result = '';
        const characters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
        const charactersLength = characters.length;
        for (let i = 0; i < 7; i++ ) {
            result += characters.charAt(Math.floor(Math.random() * charactersLength));
        }
        return result;
    }

    static _areTokensInUrl() {
        return QwcOidcProvider._getHashQueryStringParam('id_token')
            && QwcOidcProvider._getHashQueryStringParam('access_token');
    }

    static _hasCodeInUrl() {
        return !!QwcOidcProvider._getQueryParameter('code');
    }

    static _isErrorInUrl() {
        return !!QwcOidcProvider._getQueryParameter(QwcOidcProvider.ERROR_DESCRIPTION);
    }


    static _getQueryParameters() {
        return new Proxy(new URLSearchParams(window.location.search), {
            get: (searchParams, prop) => searchParams.get(prop),
        });
    }

    static _getQueryParameter(param){
        const params = QwcOidcProvider._getQueryParameters();
        if(params){
            return params[param] || null;
        }else {
            return null;
        }
    }

    static _copyIdTokenToClipboard(e) {
        e.stopPropagation();
        QwcOidcProvider._copyToClipboard(propertiesState.idToken, 'ID token');
    }

    static _copyAccessTokenToClipboard(e) {
        e.stopPropagation();
        QwcOidcProvider._copyToClipboard(propertiesState.accessToken, 'access token');
    }

    static _copyToClipboard(txt, what) {
        navigator.clipboard.writeText(txt).then(
            () => {
                notifier.showInfoMessage('Copied "' + what + '" to clipboard.', 'top-end');
            },
            () => {
                notifier.showErrorMessage('Failed to copy "' + what + '" to clipboard.', 'top-end');
            }
        );
    }

    static _getQueryParamsWithoutErrorDesc() {
        const params = new URLSearchParams(window.location.search);
        if (params) {
            const paramsWithoutFrom = [];
            params.forEach((value, key) => {
                if (key !== QwcOidcProvider.ERROR_DESCRIPTION) {
                    paramsWithoutFrom.push(key + '=' + value);
                }
            });
            if (paramsWithoutFrom.length > 0) {
                return paramsWithoutFrom.join('&');
            }
        }
        return '';
    }

    static _updateQueryParamsProperties(jsonRpc, onUpdateDone) {
        if (QwcOidcProvider._areTokensInUrl()) {
            // logged in

            propertiesState.hideImplLoggedOut = true;
            propertiesState.hideLogInErr = true;
            propertiesState.hideImplicitLoggedIn = false;
            const accessToken = QwcOidcProvider._getHashQueryStringParam('access_token');
            const idToken = QwcOidcProvider._getHashQueryStringParam('id_token');
            propertiesState.userName = QwcOidcProvider._parseUserName(accessToken, idToken);

            propertiesState.idToken = idToken;
            propertiesState.accessToken = accessToken;
            onUpdateDone();
        } else if (QwcOidcProvider._hasCodeInUrl()) {
            // logged in

            propertiesState.hideImplLoggedOut = true;
            propertiesState.hideLogInErr = true;
            propertiesState.hideImplicitLoggedIn = false;
            const code = QwcOidcProvider._getQueryParameter('code');
            const state = QwcOidcProvider._getQueryParameter('state');
            QwcOidcProvider._exchangeCodeForTokens(code, state, jsonRpc, onUpdateDone);
        } else if (propertiesState.oidcApplicationType === 'web-app') {
            QwcOidcProvider._checkSessionCookie(jsonRpc, () => {
                // logged in
                propertiesState.hideImplLoggedOut = true;
                propertiesState.hideLogInErr = true;
                propertiesState.hideImplicitLoggedIn = false;
                onUpdateDone();
            }, () => {
                // logged out
                propertiesState.hideImplicitLoggedIn = true;
                propertiesState.userName = null;
                
                if (QwcOidcProvider._isErrorInUrl()) {
                    propertiesState.hideImplLoggedOut = true;
                    propertiesState.hideLogInErr = false;
                } else {
                    propertiesState.hideLogInErr = true;
                    propertiesState.hideImplLoggedOut = false;
                }
                
                propertiesState.accessToken = null;
                propertiesState.idToken = null;
                onUpdateDone();
            });
        } else {
            // logged out

            propertiesState.hideImplicitLoggedIn = true;
            propertiesState.userName = null;

            if (QwcOidcProvider._isErrorInUrl()) {
                propertiesState.hideImplLoggedOut = true;
                propertiesState.hideLogInErr = false;
            } else {
                propertiesState.hideLogInErr = true;
                propertiesState.hideImplLoggedOut = false;
            }

            propertiesState.accessToken = null;
            propertiesState.idToken = null;
            onUpdateDone();
        }
    }

    static _navigateToSwaggerUiWithToken(token){
        const authorizedValue = {
            "SecurityScheme":{
                "schema":{
                    "flow":"implicit",
                    "authorizationUrl": (propertiesState.authorizationUrl ?? ''),
                    "tokenUrl": (propertiesState.tokenUrl ?? ''),
                    "type":"oauth2",
                    "description":"Authentication"
                },
                "clientId": propertiesState.clientId,
                "name":"SecurityScheme",
                "token":{
                    "access_token":token,
                    "token_type":"Bearer",
                    "expires_in":"900"
                }
            }
        };

        localStorage.setItem('authorized', JSON.stringify(authorizedValue));
        window.open(propertiesState.swaggerUiPath, '_blank').focus();
    }

    static _exchangeCodeForTokens(authorizationCode, state, jsonRpc, onUpdateDone) {
        // if token url is not available, it means 'getProperties' request is still in progress,
        // but no worry, this method will be invoked again when properties are loaded
        if (propertiesState.tokenUrl) {
            let tokenUrl = propertiesState.tokenUrl;
            let clientId = propertiesState.clientId;
            if (state && state.includes("_")) {
                const parts = state.split("_");
                const index = tokenUrl.indexOf("/realms/");
                tokenUrl = tokenUrl.substring(0, index + 8) + parts[1] + "/protocol/openid-connect/token";
                clientId = parts[2];
            }
            const redirectUri = "http://localhost:" + propertiesState.httpPort + window.location.pathname;
            const clientSecret = propertiesState.clientSecret;
            jsonRpc
                .exchangeCodeForTokens({tokenUrl, clientId, clientSecret, authorizationCode, redirectUri})
                .then(response => {
                    if (response.result) {
                        const tokens = JSON.parse(response.result);
                        const hasIdToken = "id_token" in tokens;
                        propertiesState.userName = QwcOidcProvider._parseUserName(tokens.access_token,
                            hasIdToken ? tokens.id_token : null);

                        propertiesState.accessToken = tokens.access_token;

                        if (hasIdToken) {
                            propertiesState.idToken = tokens.id_token;
                        } else {
                            propertiesState.idToken = null;
                        }
                    }
                    onUpdateDone();
                })
                .catch(response => {
                    notifier.showErrorMessage('Failed to exchange code for tokens. Error message: '
                        + response?.error?.message, 'top-end');
                    propertiesState.userName = null;
                    propertiesState.accessToken = null;
                    propertiesState.idToken = null;
                    onUpdateDone();
                });
        } else {
            propertiesState.userName = null;
            propertiesState.accessToken = null;
            propertiesState.idToken = null;
            onUpdateDone();
        }
    }

    static _checkSessionCookie(jsonRpc, onLoggedIn, onLoggedOut) {
        // FIXME: hardcoded path?
        const port = propertiesState.httpPort ?? 8080
        fetch("http://localhost:" + port + "/q/io.quarkus.quarkus-oidc/readSessionCookie")
           .then(response => response.json())
                .then(result => {
                    if ("id_token" in result || "access_token" in result) {
                        const tokens = result;
                        const hasIdToken = "id_token" in tokens;
                        propertiesState.userName = QwcOidcProvider._parseUserName(tokens.access_token,
                            hasIdToken ? tokens.id_token : null);

                        propertiesState.accessToken = tokens.access_token;

                        if (hasIdToken) {
                            propertiesState.idToken = tokens.id_token;
                        } else {
                            propertiesState.idToken = null;
                        }
                        propertiesState.logoutUrl = "http://localhost:8080/q/io.quarkus.quarkus-oidc/logout";
                        propertiesState.postLogoutUriParam = "redirect_uri";
                        onLoggedIn();
                    } else {
                        onLoggedOut();
                    }
                })
                .catch(response => {
                    notifier.showErrorMessage('Failed to exchange code for tokens. Error message: '
                        + response?.error?.message, 'top-end');
                    onLoggedOut();
                });
    }

    static _getTokenForNavigation() {
        if (propertiesState.introspectionIsAvailable) {
            return propertiesState.accessToken;
        } else {
            const parts = propertiesState.accessToken?.split(".");
            return parts?.length === 3 ? propertiesState.accessToken : propertiesState.idToken;
        }
    }

    static _navigateToGraphQLUi() {
        QwcOidcProvider._navigateToGraphQLUiWithToken(QwcOidcProvider._getTokenForNavigation());
    }

    static _navigateToGraphQLUiWithToken(token) {
        let url = propertiesState.graphqlUiPath;
        const headerJson = '{"authorization": "Bearer ' + token + '"}';
        url += '/?' + encodeURIComponent('headers') + '=' + encodeURIComponent(headerJson);
        window.open(url, '_blank').focus();
    }

    static _navigateToSwaggerUi() {
        QwcOidcProvider._navigateToSwaggerUiWithToken(QwcOidcProvider._getTokenForNavigation());
    }

    static _getHashQueryStringParam(paramName) {
        const params = new Proxy(new URLSearchParams(window.location.hash.substring(1)), {
            get: (searchParams, prop) => searchParams.get(prop),
        });
        if (params) {
            return params[paramName] || null;
        } else {
            return null;
        }
    }

    _logout() {
        localStorage.removeItem('authorized');
        const clientId = this._getClientId();

        let address;
        if (propertiesState.keycloakAdminUrl && this._selectedRealm) {
            address = propertiesState.keycloakAdminUrl + '/realms/' + this._selectedRealm + '/protocol/openid-connect/logout';
        } else {
            address = propertiesState.logoutUrl;
        }

        window.location.assign(address
            + '?' + (propertiesState.postLogoutUriParam ?? '') + '=' + this._getEncodedPath()
            + '&id_token_hint=' + propertiesState.idToken
            + "&client_id=" + clientId);
    }

    static _prettyToken(token){
        if (token) {
            const parts = token.split(".");
            if (parts.length === 3) {
                const headers = parts[0]?.trim();
                const payload = parts[1]?.trim();
                const signature = parts[2]?.trim();

                return html`
                <span class='token-headers jwt-tooltip-cursor' title='Header'>${headers}</span>.<span class='token-payload jwt-tooltip-cursor' title='Claims'
                >${payload}</span>.<span class='token-signature jwt-tooltip-cursor' title='Signature'>${signature}</span>
            `;
            } else if (parts.length === 5) {
                const headers = parts[0]?.trim();
                const encryptedKey = parts[1]?.trim();
                const initVector = parts[2]?.trim();
                const ciphertext = parts[3]?.trim();
                const authTag = parts[4]?.trim();

                return html`
                <span class='token-headers jwt-tooltip-cursor' title='Header'>${headers}</span>.<span class='token-encryption jwt-tooltip-cursor' title='Encrypted Key'
                >${encryptedKey}.<span class='token-encryption jwt-tooltip-cursor' title='Init Vector'
                >${initVector}</span>.<span class='token-payload jwt-tooltip-cursor' title='Ciphertext'
                >${ciphertext}</span>.<span class='token-encryption jwt-tooltip-cursor' title='Authentication Tag'>${authTag}</span>
            `;
            } else {
                return html`${token}`;
            }
        }
        return html``;
    }

    static _decodeBase64(encoded){
        function base64ToBytes(base64) {
            const binString = window.atob(base64);
            return Uint8Array.from(binString, (m) => m.codePointAt(0));
        }
        return new TextDecoder().decode(base64ToBytes(encoded));
    }

    static _formatJson(jwt) {
        const tooltips = {
            "iss": "Issuer",
            "sub": "Subject",
            "aud": "Audience",
            "exp": "Expiration Time",
            "iat": "Issued At",
            "auth_time": "End-User Authentication Time",
            "nonce": "Cryptographic Nonce",
            "acr": "Authentication Context Class Reference",
            "amr": "Authentication Methods References",
            "azp": "Authorized Party",
            "nbf": "Not Before",
            "jti": "JWT ID",
            "sid": "Session ID",
            "scope": "Scope",
            "upn": "User Principal Name",
            "groups": "Groups",
            "kid": "Key ID",
            "alg": "Algorithm",
            "typ": "Token Type"
        };
        const spaces = 4;
        var ret = "{";
        var once = false;
        for(let k in jwt){
            if (Object.prototype.hasOwnProperty.call(jwt, k)) {
                const val = jwt[k];
                if(once){
                    ret += ",";
                }
                ret += "\n" + " ".repeat(spaces);
                // decorate key
                var tooltip = tooltips[k];
                if(tooltip) {
                    ret += "<span class='jwt-tooltip-bg jwt-tooltip-cursor' title='"+tooltip+"'>\"" + k + "\"</span>";
                } else {
                    ret += "\"" + k + "\"";
                }
                // on to values
                ret += ": ";
                // decorate values
                if(k == 'iat' || k == 'nbf' || k == 'exp'){
                    ret += "<span class='jwt-tooltip-bg jwt-tooltip-cursor' title='" + new Date(val * 1000).toString() + "'>" + val + "</span>";
                } else {
                    ret += JSON.stringify(val);
                }
                
            }
            once = true;
        }
        if(once){
            ret += "\n";
        }
        ret += "}";
        return ret;
    }
    
    static _decodeToken(token) {
        if (token) {
            const parts = token.split(".");
            if (parts.length === 3) {
                const headers = QwcOidcProvider._decodeBase64(parts[0]);
                const headersJsonObj = JSON.parse(headers);
                const headersHtml = QwcOidcProvider._formatJson(headersJsonObj);
                const payload = QwcOidcProvider._decodeBase64(parts[1]);
                const signature = parts[2];
                const jsonPayload = JSON.parse(payload);
                const json = QwcOidcProvider._formatJson(jsonPayload);
                return html`
                <pre class='token-headers'>${unsafeHTML(headersHtml?.trim())}</pre>
                <pre class='token-payload'>${unsafeHTML(json?.trim())}</pre>
                <span class='token-signature jwt-tooltip-cursor' title='Signature'>${signature?.trim()}</span>
                `;
            } else if (parts.length === 5) {
                const headers = window.atob(parts[0]?.trim());
                const encryptedKey = parts[1]?.trim();
                const initVector = parts[2]?.trim();
                const ciphertext = parts[3]?.trim();
                const authTag = parts[4]?.trim();

                return html`
                <pre class='token-headers jwt-tooltip-cursor' title='Header'>${JSON.stringify(JSON.parse(headers), null, 4)?.trim()}</pre>
                <pre class='token-encryption jwt-tooltip-cursor' title='Encrypted Key'>${encryptedKey}</pre>
                <pre class='token-encryption jwt-tooltip-cursor' title='Init Vector'>${initVector}</pre>
                <pre class='token-payload jwt-tooltip-cursor' title='Ciphertext'>${ciphertext}</pre>
                <span class='token-encryption jwt-tooltip-cursor' title='Authentication Tag'>${authTag}</span>
            `;
            } else {
                return html`${token}`;
            }
        }
        return html``;
    }

    static _parseUserName(accessToken, idToken) {
        const getUserName = (token) => {
            if (token) {
                const parts = token.split(".");
                if (parts.length === 3) {
                    const payload = QwcOidcProvider._decodeBase64(parts[1]);
                    const jsonPayload = JSON.parse(payload);
                    if (jsonPayload?.upn) {
                        return jsonPayload.upn;
                    } else if (jsonPayload?.preferred_username) {
                        return jsonPayload.preferred_username;
                    }  else if (jsonPayload?.name) {
                        return jsonPayload.name;
                    }
                }
            }
            return null;
        };
        return getUserName(accessToken) || getUserName(idToken);
    }

    static _getFontAwesomeFailureSvgAsImgSrc() {
        return 'data:image/svg+xml;utf-8,<svg xmlns=\'http://www.w3.org/2000/svg\' viewBox=\'0 0 512 512\'>' +
            '<path d=\'M256 48a208 208 0 1 1 0 416 208 208 0 1 1 0-416zm0 464A256 256 0 1 0 256 0a256 256 0 1 0 0 ' +
            '512zM175 175c-9.4 9.4-9.4 24.6 0 33.9l47 47-47 47c-9.4 9.4-9.4 24.6 0 33.9s24.6 9.4 33.9 0l47-47 ' +
            '47 47c9.4 9.4 24.6 9.4 33.9 0s9.4-24.6 0-33.9l-47-47 47-47c9.4-9.4 9.4-24.6 0-33.9s-24.6-9.4-33.9 ' +
            '0l-47 47-47-47c-9.4-9.4-24.6-9.4-33.9 0z\' style=\'fill: rgb(220,53,69)\'/></svg>'
    }

    static _getFontAwesomeSuccessSvgAsImgSrc() {
        return 'data:image/svg+xml;utf-8,<svg xmlns=\'http://www.w3.org/2000/svg\' viewBox=\'0 0 512 512\'>' +
            '<path d=\'M256 48a208 208 0 1 1 0 416 208 208 0 1 1 0-416zm0 464A256 256 0 1 0 256 0a256 256 0 1 0' +
            ' 0 512zM369 209c9.4-9.4 9.4-24.6 0-33.9s-24.6-9.4-33.9 0l-111 111-47-47c-9.4-9.4-24.6-9.4-33.9 ' +
            '0s-9.4 24.6 0 33.9l64 64c9.4 9.4 24.6 9.4 33.9 0L369 209z\' style=\'fill: rgb(40,167,69)\'/></svg>'
    }

    _getTokenUrl() {
        if (propertiesState.keycloakAdminUrl && propertiesState.keycloakRealms?.length > 0) {
            return this._getKeycloakTokenUrl();
        } else {
            return propertiesState.tokenUrl;
        }
    }

    _getKeycloakTokenUrl() {
        return propertiesState.keycloakAdminUrl + "/realms/" + this._selectedRealm + "/protocol/openid-connect/token";
    }
    
    _getKeycloakAuthorizationUrl() {
        return propertiesState.keycloakAdminUrl + "/realms/" + this._selectedRealm + "/protocol/openid-connect/auth";
    }
}
customElements.define('qwc-oidc-provider', QwcOidcProvider);
