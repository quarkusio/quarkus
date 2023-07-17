import { LitElement, html, css} from 'lit';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import { infoUrl } from 'build-time-data';
import '@vaadin/progress-bar';
import 'qui-card';
import '@vaadin/icon';

/**
 * This component shows the Info Screen
 */
export class QwcInfo extends LitElement {

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
            align-items: center;
            padding: 10px;
            gap: 10px;
        }
        vaadin-icon {
            font-size: xx-large;
        }
    `;

    static properties = {
        _infoUrl: {state: false},
        _info: {state: true},
    };

    constructor() {
        super();
        this._infoUrl = infoUrl;
        this._info = null;
    }

    async connectedCallback() {
        super.connectedCallback();
        await this.load();
    }
        
    async load() {
        const response = await fetch(this._infoUrl)
        const data = await response.json();
        this._info = data;
    }

    render() {
        if (this._info) {
            return html`
                ${this._renderOsInfo(this._info)}
                ${this._renderJavaInfo(this._info)}
                ${this._renderGitInfo(this._info)}
                ${this._renderBuildInfo(this._info)}
            `;
        }else{
            return html`
            <div style="color: var(--lumo-secondary-text-color);width: 95%;" >
                <div>Fetching infomation...</div>
                <vaadin-progress-bar indeterminate></vaadin-progress-bar>
            </div>
            `;
        }
    }
    
    _renderOsInfo(info){
        if(info.os){
            let os = info.os;
            return html`<qui-card title="Operating System">
                    <div class="cardContent" slot="content">
                        ${this._renderOsIcon(os.name)}    
                        <table class="table">
                            <tr><td>Name</td><td>${os.name}</td></tr>
                            <tr><td>Version</td><td>${os.version}</td></tr>
                            <tr><td>Arch</td><td>${os.arch}</td></tr>
                        </table>
                    </div>
                </qui-card>`;
        }
    }
    
    _renderJavaInfo(info){
        if(info.java){
            let java = info.java;
            return html`<qui-card title="Java">
                    <div class="cardContent" slot="content">
                        <vaadin-icon icon="font-awesome-brands:java"></vaadin-icon>
                        <table class="table">
                            <tr><td>Version</td><td>${java.version}</td></tr>
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
            return html`<qui-card title="Git">
                    <div class="cardContent" slot="content">
                        <vaadin-icon icon="font-awesome-brands:git"></vaadin-icon>
                        <table class="table">
                            <tr><td>Branch</td><td>${git.branch}</td></tr>
                            <tr><td>Commit</td><td>${git.commit.id}</td></tr>
                            <tr><td>Time</td><td>${git.commit.time}</td></tr>
                        </table>
                    </div>
                </qui-card>`;
        }
    }
    
    _renderBuildInfo(info){
        if(info.build){
            let build = info.build;
            return html`<qui-card title="Build">
                    <div class="cardContent" slot="content">
                        <table class="table">
                            <tr><td>Group</td><td>${build.group}</td></tr>
                            <tr><td>Artifact</td><td>${build.artifact}</td></tr>
                            <tr><td>Version</td><td>${build.version}</td></tr>
                            <tr><td>Time</td><td>${build.time}</td></tr>
                        </table>
                    </div>
                </qui-card>`;
        }
    }
}
customElements.define('qwc-info', QwcInfo);