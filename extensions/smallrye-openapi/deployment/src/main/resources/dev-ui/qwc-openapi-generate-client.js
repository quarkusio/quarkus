import { LitElement, html, css } from 'lit';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/combo-box';
import '@vaadin/progress-bar';
import '@vaadin/button';
import '@vaadin/icon';
import 'qui-themed-code-block';
import { notifier } from 'notifier';
import { msg, str, updateWhenLocaleChanges } from 'localization';

export class QwcOpenapiGenerateClient extends LitElement {
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
        updateWhenLocaleChanges(this);
        this._codemap = new Map();
        this._selectedLanguage = null;
        this._loading = false;

        this.languages = [
            {label: msg('Java (Quarkus)', { id: 'quarkus-smallrye-openapi-java-quarkus' }), value: 'Java', mode: 'java', context: msg('This code should be valid Quarkus Java code that use the quarkus-rest-client-jackson extension. It is very important to use the jakarta.ws namaspace when importing classes. Do NOT use the old javax.ws namespace. Use the org.eclipse.microprofile.rest.client.inject.RegisterRestClient annotation', { id: 'quarkus-smallrye-openapi-java-context' })},
            {label: msg('Kotlin (Quarkus)', { id: 'quarkus-smallrye-openapi-kotlin-quarkus' }), value: 'Kotlin', mode: 'java', context: msg('This code should be valid Quarkus Kotlin code that use the quarkus-rest-client-jackson extension. It is very important to use the jakarta.ws namaspace when importing classes. Do NOT use the old javax.ws namespace. Use the org.eclipse.microprofile.rest.client.inject.RegisterRestClient annotation', { id: 'quarkus-smallrye-openapi-kotlin-context' })},
            {label: msg('Javascript', { id: 'quarkus-smallrye-openapi-javascript' }), value: 'Javascript', mode: 'js', context: ''},
            {label: msg('TypeScript', { id: 'quarkus-smallrye-openapi-typescript' }), value: 'Typecript', mode: 'ts', context: ''},
            {label: msg('C#', { id: 'quarkus-smallrye-openapi-csharp' }), value: 'C#', mode: 'cs', context: ''},
            {label: msg('C++', { id: 'quarkus-smallrye-openapi-cpp' }), value: 'C++', mode: 'cpp', context: ''},
            {label: msg('PHP', { id: 'quarkus-smallrye-openapi-php' }), value: 'PHP', mode: 'php', context: ''},
            {label: msg('Python', { id: 'quarkus-smallrye-openapi-python' }), value: 'Python', mode: 'py', context: ''},
            {label: msg('Rust', { id: 'quarkus-smallrye-openapi-rust' }), value: 'Rust', mode: 'rust', context: ''},
            {label: msg('Go', { id: 'quarkus-smallrye-openapi-go' }), value: 'Golang', mode: 'go', context: ''}
        ];
    }

    render() {
        return html`
        <div class=top">
            <vaadin-combo-box
                label=${msg('Technology / Language', { id: 'quarkus-smallrye-openapi-technology-language' })}
                .items="${this.languages}"
                item-label-path="label"
                item-value-path="value"
                @value-changed="${this._languageSelected}">
            </vaadin-combo-box>
            <p class="blurb">
                ${msg('Generate client code based on the OpenAPI schema document produced by your Quarkus application at build time.', { id: 'quarkus-smallrye-openapi-description' })}
            </p>
        </div>
        
        ${this._loading ? html`
            <div class="progress">
                <label class="text-secondary" id="pblbl">${msg('Talking to AI...', { id: 'quarkus-smallrye-openapi-talking-to-ai' })}</label>
                <vaadin-progress-bar indeterminate aria-labelledby="pblbl" aria-describedby="sublbl"></vaadin-progress-bar>
                <span class="text-secondary text-xs" id="sublbl">${msg('This can take a while', { id: 'quarkus-smallrye-openapi-can-take-while' })}</span>
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
                notifier.showErrorMessage(msg(str`Failed to generate code: ${e}`, { id: 'quarkus-smallrye-openapi-failed-generate' }));
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
        <div class="heading">${msg(str`${ll} code generated from the OpenAPI Schema with AI:`, { id: 'quarkus-smallrye-openapi-code-generated' })}
            <vaadin-button theme="secondary" @click="${() => this._copyGeneratedContent(lang.value)}">
                <vaadin-icon icon="font-awesome-solid:copy"></vaadin-icon>
                ${msg('Copy', { id: 'quarkus-smallrye-openapi-copy' })}
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
                    .then(() => notifier.showInfoMessage(msg('Content copied to clipboard', { id: 'quarkus-smallrye-openapi-copied' })))
                    .catch(err => notifier.showErrorMessage(msg(str`Failed to copy content: ${err}`, { id: 'quarkus-smallrye-openapi-failed-copy' })));
        } else {
            notifier.showWarningMessage(msg('No content', { id: 'quarkus-smallrye-openapi-no-content' }));
        }
    }
}

customElements.define('qwc-openapi-generate-client', QwcOpenapiGenerateClient);
