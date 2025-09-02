import { LitElement, html, css } from 'lit';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/combo-box';
import '@vaadin/progress-bar';
import '@vaadin/button';
import '@vaadin/icon';
import 'qui-themed-code-block';
import { notifier } from 'notifier';

export class QwcGraphqlGenerateClient extends LitElement {
    jsonRpc = new JsonRpc(this);

    static styles = css`
    :host {
        display: flex;
        flex-direction: column;
        padding-right: 10px;
        padding-left: 10px;
    }

    .generatedcode {
        display: flex;
        flex-direction: column;
        gap: 10px;
        margin-top: 1em;
    }

    .progress {
        margin-top: 1em;
    }

    .heading {
        display: flex;
        align-items: center;
        justify-content: space-between;
    }
    .top {
        display: flex;
        justify-content: space-between;
        align-items: baseline;
    }
    .blurb {
        font-size: 0.9em;
        color: var(--lumo-secondary-text-color);
        margin-bottom: 1em;
    }
    
  `;

    static properties = {
        _codemap: {state: true},
        _selectedLanguage: {state: true},
        _loading: {state: true},
    };

    constructor() {
        super();
        this._codemap = new Map();
        this._selectedLanguage = null;
        this._loading = false;

        this.languages = [
            {label: 'Java (Quarkus)', value: 'Java', mode: 'java', context: 'This code should be valid Quarkus Java code that use the quarkus-smallrye-graphql-client extension and the dynamic client, not the typesafe one'},
            {label: 'Kotlin (Quarkus)', value: 'Kotlin', mode: 'java', context: 'This code should be valid Quarkus Kotlin code that use the quarkus-smallrye-graphql-client extension and the dynamic client, not the typesafe one'},
            {label: 'Javascript', value: 'Javascript', mode: 'js', context: ''},
            {label: 'TypeScript', value: 'Typecript', mode: 'ts', context: ''},
            {label: 'C#', value: 'C#', mode: 'cs', context: ''},
            {label: 'C++', value: 'C++', mode: 'cpp', context: ''},
            {label: 'PHP', value: 'PHP', mode: 'php', context: ''},
            {label: 'Python', value: 'Python', mode: 'py', context: ''},
            {label: 'Rust', value: 'Rust', mode: 'rust', context: ''},
            {label: 'Go', value: 'Golang', mode: 'go', context: ''}
        ];
    }

    render() {
        return html`
        <div class="top">
            <vaadin-combo-box
                label="Technology / Language"
                .items="${this.languages}"
                item-label-path="label"
                item-value-path="value"
                @value-changed="${this._languageSelected}">
            </vaadin-combo-box>
            <p class="blurb">
                Generate client code based on the GraphQL schema document produced by your Quarkus application.
            </p>
        </div>
        
        ${this._loading ? html`
            <div class="progress">
                <label class="text-secondary" id="pblbl">Talking to AI...</label>
                <vaadin-progress-bar indeterminate aria-labelledby="pblbl" aria-describedby="sublbl"></vaadin-progress-bar>
                <span class="text-secondary text-xs" id="sublbl">This can take a while</span>
            </div>
        ` : ''}

        ${this._selectedLanguage && this._codemap.has(this._selectedLanguage.value) ? this._renderClientResult(this._selectedLanguage) : ''}
    `;
    }

    async _languageSelected(event) {
        const selectedValue = event.detail.value;
        const lang = this.languages.find(l => l.value === selectedValue);
        if (!lang)
            return;

        this._selectedLanguage = lang;

        if (!this._codemap.has(lang.value)) {
            this._loading = true;
            try {
                const res = await this.jsonRpc.generateClient({language: lang.value, extraContext: lang.context});
                if(res.result.code){
                    this._codemap.set(lang.value, res.result.code);
                }else {
                    console.warn("code field not populated");
                    this._codemap.set(lang.value, JSON.stringify(res.result)); // fallback
                }
            } catch (e) {
                console.error('Failed to generate code:', e);
                notifier.showErrorMessage("Failed to generate code: " + e);
            } finally {
                this._loading = false;
            }
        }
    }

    _renderClientResult(lang) {
        const code = this._codemap.get(lang.value);
        return html`
      <div class="generatedcode">
        <div class="heading">${lang.label} code generated from the GraphQL Schema with Quarkus assistant:
            <vaadin-button theme="secondary" @click="${() => this._copyGeneratedContent(lang.value)}">
                <vaadin-icon icon="font-awesome-solid:copy"></vaadin-icon>
                Copy
            </vaadin-button>
        </div>
        <qui-themed-code-block
            mode="${lang.mode}"
            content="${code}"
            showLineNumbers>
        </qui-themed-code-block>
      </div>
    `;
    }

    _copyGeneratedContent(langName) {
        if (this._codemap.has(langName)) {
            const content = this._codemap.get(langName);
            navigator.clipboard.writeText(content)
                    .then(() => notifier.showInfoMessage("Content copied to clipboard"))
                    .catch(err => notifier.showErrorMessage("Failed to copy content: " + err));
        } else {
            notifier.showWarningMessage("No content");
        }
    }
}

customElements.define('qwc-graphql-generate-client', QwcGraphqlGenerateClient);
