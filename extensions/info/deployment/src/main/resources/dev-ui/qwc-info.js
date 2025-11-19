import { LitElement, html, css} from 'lit';
import { unsafeHTML } from 'lit/directives/unsafe-html.js';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/progress-bar';
import '@qomponent/qui-card';
import '@vaadin/icon';
import { msg, updateWhenLocaleChanges } from 'localization';

/**
 * This component shows the Info Screen
 */
export class QwcInfo extends LitElement {

    jsonRpc = new JsonRpc(this);

    static styles = css`
        :host {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(450px, 1fr));
            gap: 1em;
            padding: 10px;
        }
        qui-card {
            display: flex;
        }
        .cardContent {
            display: flex;
            padding: 10px;
            gap: 10px;
            height: 100%;
        }
        vaadin-icon {
            font-size: xx-large;
        }
        .table {
            height: fit-content;
        }
        .row-header {
            color: var(--lumo-contrast-50pct);
            vertical-align: top;
        }
    `;

    static properties = {
        _infoUrl: {state: false},
        _info: {state: true},
    };

    constructor() {
        super();
        updateWhenLocaleChanges(this);
        this._info = null;
    }

    async connectedCallback() {
        super.connectedCallback();
        this.jsonRpc.getApplicationAndEnvironmentInfo().then(jsonRpcResponse => { 
            this._info = jsonRpcResponse.result;
        });
    }

    render() {
        if (this._info) {
            return html`
                ${this._renderOsInfo(this._info)}
                ${this._renderJavaInfo(this._info)}
                ${this._renderBuildInfo(this._info)}
                ${this._renderGitInfo(this._info)}
                ${this._renderExternalContributedInfo(this._info)}
            `;
        }else{
            return html`
            <div style="color: var(--lumo-secondary-text-color);width: 95%;" >
                <div>${msg('Fetching information...', { id: 'quarkus-info-fetching-information' })}</div>
                <vaadin-progress-bar indeterminate></vaadin-progress-bar>
            </div>
            `;
        }
    }

    _renderOsInfo(info){
        if(info.os){
            let os = info.os;
            return html`<qui-card header=${msg('Operating System', { id: 'quarkus-info-operating-system' })}>
                    <div class="cardContent" slot="content">
                        ${this._renderOsIcon(os.name)}    
                        <table class="table">
                            <tr><td class="row-header">${msg('Name', { id: 'quarkus-info-name' })}</td><td>${os.name}</td></tr>
                            <tr><td class="row-header">${msg('Version', { id: 'quarkus-info-version' })}</td><td>${os.version}</td></tr>
                            <tr><td class="row-header">${msg('Arch', { id: 'quarkus-info-arch' })}</td><td>${os.arch}</td></tr>
                        </table>
                    </div>
                </qui-card>`;
        }
    }

    _renderJavaInfo(info){
        if(info.java){
            let java = info.java;
            return html`<qui-card header=${msg('Java', { id: 'quarkus-info-java' })}>
                    <div class="cardContent" slot="content">
                        <vaadin-icon icon="font-awesome-brands:java"></vaadin-icon>
                        <table class="table">
                            <tr><td class="row-header">${msg('Version', { id: 'quarkus-info-version' })}</td><td>${java.version}</td></tr>
                            <tr><td class="row-header">${msg('Vendor', { id: 'quarkus-info-vendor' })}</td><td>${java.vendor}</td></tr>
                            <tr><td class="row-header">${msg('Vendor Version', { id: 'quarkus-info-vendor-version' })}</td><td>${java.vendorVersion}</td></tr>
                        </table>
                    </div>    
                </qui-card>`;
        }
    }

    _renderOsIcon(osname){

        if(osname){
            if(osname.toLowerCase().startsWith("linux")){
                return html`<vaadin-icon icon="font-awesome-brands:linux"></vaadin-icon>`;
            }else if(osname.toLowerCase().startsWith("mac") || osname.toLowerCase().startsWith("darwin")){
                return html`<vaadin-icon icon="font-awesome-brands:apple"></vaadin-icon>`;
            }else if(osname.toLowerCase().startsWith("win")){
                return html`<vaadin-icon icon="font-awesome-brands:windows"></vaadin-icon>`;
            }
        }
    }

    _renderGitInfo(info){
        if(info.git){
            let git = info.git;
            return html`<qui-card header=${msg('Git', { id: 'quarkus-info-git' })}>
                    <div class="cardContent" slot="content">
                        <vaadin-icon icon="font-awesome-brands:git"></vaadin-icon>
                        <table class="table">
                            <tr><td class="row-header">${msg('Branch', { id: 'quarkus-info-branch' })}</td><td>${git.branch}</td></tr>
                            <tr><td class="row-header">${msg('Commit Id ', { id: 'quarkus-info-commit-id' })}</td><td>${this._renderCommitId(git)}</td></tr>
                            <tr><td class="row-header">${msg('Commit Time', { id: 'quarkus-info-commit-time' })}</td><td>${git.commit.time}</td></tr>
                            ${this._renderOptionalData(git)}
                        </table>
                    </div>
                </qui-card>`;
        }
    }

    _renderCommitId(git){
        if(typeof git.commit.id === "string"){
            return html`${git.commit.id}`;
        }else {
            return html`${git.commit.id.full}`;
        }
    }

    _renderOptionalData(git){
        if(typeof git.commit.id !== "string"){
            return html`<tr><td class="row-header">${msg('Commit User', { id: 'quarkus-info-commit-user' })}</td><td>${git.commit.user.name} &lt;${git.commit.user.email}&gt;</td></tr>
                        <tr><td class="row-header">${msg('Commit Message', { id: 'quarkus-info-commit-message' })}</td><td>${unsafeHTML(this._replaceNewLine(git.commit.id.message.full))}</td></tr>
                        <tr><td class="row-header">${msg('Remote URL', { id: 'quarkus-info-remote-url' })}</td><td>${unsafeHTML(git.remote)}</td></tr>`
        }
    }

    _replaceNewLine(line){
        return line.replace(new RegExp('\r?\n','g'), '<br />');
    }

    _renderBuildInfo(info){
        if(info.build){
            let build = info.build;
            return html`<qui-card header=${msg('Build', { id: 'quarkus-info-build' })}>
                    <div class="cardContent" slot="content">
                        <table class="table">
                            <tr><td class="row-header">${msg('Quarkus', { id: 'quarkus-info-quarkus' })}</td><td>${build.quarkusVersion}</td></tr>
                            <tr><td class="row-header">${msg('App Name', { id: 'quarkus-info-app-name' })}</td><td>${unsafeHTML(build.name)}</td></tr>
                            <tr><td class="row-header">${msg('Group', { id: 'quarkus-info-group' })}</td><td>${build.group}</td></tr>
                            <tr><td class="row-header">${msg('Artifact', { id: 'quarkus-info-artifact' })}</td><td>${build.artifact}</td></tr>
                            <tr><td class="row-header">${msg('Version', { id: 'quarkus-info-version' })}</td><td>${build.version}</td></tr>
                            <tr><td class="row-header">${msg('Time', { id: 'quarkus-info-time' })}</td><td>${build.time}</td></tr>
                        </table>
                    </div>
                </qui-card>`;
        }
    }

    _renderExternalContributedInfo(info){
        const externalConstributors = Object.keys(info)
            .filter(key => key !== 'build')
            .filter(key => key !== 'os')
            .filter(key => key !== 'git')
            .filter(key => key !== 'java')
        if(externalConstributors.length > 0){
            const cards = [];
            externalConstributors.map(key => {
                    const extInfo = info[key];
                    const rows = [];
                    let displayName = key;
                    for (const property of Object.keys(extInfo)){
                        if (property === 'displayName'){
                            displayName = extInfo[property];
                        }else{
                            rows.push(html`<tr><td class="row-header">${property}</td><td>${extInfo[property]}</td></tr>`);
                        }
                    }
                    cards.push(html`<qui-card header=${displayName}>
                        <div class="cardContent" slot="content">
                            <vaadin-icon icon="font-awesome-solid:circle-info"></vaadin-icon>
                            <table class="table">
                                ${rows}
                            </table>
                        </div>
                    </qui-card>`);
                })
            return html`${cards}`;
        }
    }
}
customElements.define('qwc-info', QwcInfo);
