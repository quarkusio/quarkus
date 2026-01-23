import { LitElement, html, css } from 'lit';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/combo-box';
import '@vaadin/progress-bar';
import '@vaadin/button';
import '@vaadin/icon';
import 'qui-themed-code-block';
import 'qui-assistant-chat';
import { notifier } from 'notifier';
import { msg, str, updateWhenLocaleChanges } from 'localization';

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
    .buttons {
        display: flex;
        gap: 5px;
    }
  `;

    static properties = {
        _codemap: {state: true},
        _selectedLanguage: {state: true},
        _loading: {state: true}
    };

    constructor() {
        super();
        updateWhenLocaleChanges(this);
        this._codemap = new Map();
        this._selectedLanguage = null;
        this._loading = false;

        this.languages = [
            {label: msg('Java (Quarkus)', { id: 'quarkus-smallrye-graphql-java-quarkus' }), value: 'Java', mode: 'java', context: 'This code should be valid Quarkus Java code that use the quarkus-smallrye-graphql-client extension and the dynamic client, not the typesafe one'},
            {label: msg('Kotlin (Quarkus)', { id: 'quarkus-smallrye-graphql-kotlin-quarkus' }), value: 'Kotlin', mode: 'java', context: 'This code should be valid Quarkus Kotlin code that use the quarkus-smallrye-graphql-client extension and the dynamic client, not the typesafe one'},
            {label: msg('Javascript', { id: 'quarkus-smallrye-graphql-javascript' }), value: 'Javascript', mode: 'js', context: ''},
            {label: msg('TypeScript', { id: 'quarkus-smallrye-graphql-typescript' }), value: 'Typecript', mode: 'ts', context: ''},
            {label: msg('C#', { id: 'quarkus-smallrye-graphql-csharp' }), value: 'C#', mode: 'cs', context: ''},
            {label: msg('C++', { id: 'quarkus-smallrye-graphql-cpp' }), value: 'C++', mode: 'cpp', context: ''},
            {label: msg('PHP', { id: 'quarkus-smallrye-graphql-php' }), value: 'PHP', mode: 'php', context: ''},
            {label: msg('Python', { id: 'quarkus-smallrye-graphql-python' }), value: 'Python', mode: 'py', context: ''},
            {label: msg('Rust', { id: 'quarkus-smallrye-graphql-rust' }), value: 'Rust', mode: 'rust', context: ''},
            {label: msg('Go', { id: 'quarkus-smallrye-graphql-go' }), value: 'Golang', mode: 'go', context: ''}
        ];
    }

    render() {
        return html`
        <div class="top">
            <vaadin-combo-box
                label=${msg('Technology / Language', { id: 'quarkus-smallrye-graphql-technology-language' })}
                .items="${this.languages}"
                item-label-path="label"
                item-value-path="value"
                @value-changed="${this._languageSelected}">
            </vaadin-combo-box>
            <p class="blurb">
                ${msg('Generate client code based on the GraphQL schema document produced by your Quarkus application.', { id: 'quarkus-smallrye-graphql-description' })}
            </p>
        </div>
        
        ${this._loading ? html`
            <div class="progress">
                <label class="text-secondary" id="pblbl">${msg('Talking to AI...', { id: 'quarkus-smallrye-graphql-talking-to-ai' })}</label>
                <vaadin-progress-bar indeterminate aria-labelledby="pblbl" aria-describedby="sublbl"></vaadin-progress-bar>
                <span class="text-secondary text-xs" id="sublbl">${msg('This can take a while', { id: 'quarkus-smallrye-graphql-can-take-while' })}</span>
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
                notifier.showErrorMessage(msg(str`Failed to generate code: ${e}`, { id: 'quarkus-smallrye-graphql-failed-generate' }));
            } finally {
                this._loading = false;
            }
        }
    }

    _renderClientResult(lang) {
        const code = this._codemap.get(lang.value);
        const ll = lang.label;
        return html`
      <div class="generatedcode">
        <div class="heading">${msg(str`${ll} code generated from the GraphQL Schema with Quarkus assistant:`, { id: 'quarkus-smallrye-graphql-code-generated' })}
            <div class="buttons">
                <vaadin-button theme="secondary" @click="${() => this._copyGeneratedContent(lang.value)}">
                    <vaadin-icon icon="font-awesome-solid:copy"></vaadin-icon>
                    ${msg('Copy', { id: 'quarkus-smallrye-graphql-copy' })}
                </vaadin-button>
                <qui-assistant-chat></qui-assistant-chat>
            </div>
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
                    .then(() => notifier.showInfoMessage(msg('Content copied to clipboard', { id: 'quarkus-smallrye-graphql-copied' })))
                    .catch(err => notifier.showErrorMessage(msg(str`Failed to copy content: ${err}`, { id: 'quarkus-smallrye-graphql-failed-copy' })));
        } else {
            notifier.showWarningMessage(msg('No content', { id: 'quarkus-smallrye-graphql-no-content' }));
        }
    }
}

customElements.define('qwc-graphql-generate-client', QwcGraphqlGenerateClient);
