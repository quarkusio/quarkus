import { LitElement, html, css} from 'lit';
import { unsafeHTML } from 'lit/directives/unsafe-html.js';
import { RouterController } from 'router-controller';
import { devuiState } from 'devui-state';
import { observeState } from 'lit-element-state';
import 'qwc/qwc-extension.js';
import 'qwc/qwc-extension-link.js';

/**
 * This component create cards of all the extensions
 */
export class QwcExtensions extends observeState(LitElement) {

    static styles = css`
        .grid {
            display: flex;
            flex-wrap: wrap;
            gap: 20px;
            padding-left: 5px;
            padding-right: 10px;
        }
        
        .description {
            padding-bottom: 10px;
        }
    
        .card-content {
            color: var(--lumo-contrast-90pct);
            display: flex;
            flex-direction: column;
            justify-content: flex-start;
            padding: 10px 10px;
            height: 100%;
        }

        .card-content slot {
            display: flex;
            flex-flow: column wrap;
            padding-top: 5px;
        }
    `;

    constructor() {
        super();
        // TODO: Change this to use state
        window.addEventListener('vaadin-router-location-changed', (event) => {
            var pageDetails = RouterController.parseLocationChangedEvent(event);
        });
    }

    render() {
        return html`<div class="grid">
            ${devuiState.cards.active.map(extension => this._renderActive(extension))}            
            ${devuiState.cards.inactive.map(extension => this._renderInactive(extension))}
          </div>`;
    }
    
    _renderActive(extension){
        extension.cardPages.forEach(page => {   
            if(page.embed){ // we need to register with the router
                import(page.componentRef);
                RouterController.addExtensionRoute(page);
            }
        });
        
        return html`
                <qwc-extension 
                    clazz="active"
                    name="${extension.name}" 
                    description="${extension.description}"
                    guide="${extension.guide}"
                    namespace="${extension.namespace}"
                    artifact="${extension.artifact}"
                    shortName="${extension.shortName}"
                    keywords="${extension.keywords}"
                    status="${extension.status}"
                    configFilter="${extension.configFilter}"
                    categories="${extension.categories}"
                    unlisted="${extension.unlisted}"
                    builtWith="${extension.builtWith}"
                    providesCapabilities="${extension.providesCapabilities}"
                    extensionDependencies="${extension.extensionDependencies}">

                    ${this._renderCardContent(extension)}

                </qwc-extension>

            `;
    }

    _renderCardContent(extension){
        if(extension.card){
            return this._renderCustomCardContent(extension);
        } else {
            return this._renderDefaultCardContent(extension);
        }
    }

    _renderCustomCardContent(extension){
        import(extension.card.componentRef);
        
        // TODO: Pass description and links along ${this._renderCardLinks(extension)}
        let customCardCode = `<${extension.card.componentName} 
                                class="card-content"
                                slot="content"
                                description="${extension.description}">

                             </${extension.card.componentName}>`;
        
        return html`${unsafeHTML(customCardCode)}`;
        
    }

    _renderDefaultCardContent(extension){
        
        return html`<div class="card-content" slot="content">
                        <span class="description">${extension.description}</span>
                            ${this._renderCardLinks(extension)}
                    </div>`;
    }

    _renderCardLinks(extension){
        
        return html`${extension.cardPages.map(page => html`
                            <qwc-extension-link slot="link"
                                extensionName="${extension.name}"
                                iconName="${page.icon}"
                                displayName="${page.title}"
                                staticLabel="${page.staticLabel}"
                                dynamicLabel="${page.dynamicLabel}"
                                streamingLabel="${page.streamingLabel}"
                                path="${page.id}" 
                                webcomponent="${page.componentLink}" >
                            </qwc-extension-link>
                        `)}`;
    }

    _renderInactive(extension){
        if(extension.unlisted === "false"){
            return html`
                <qwc-extension
                    clazz="inactive"
                    name="${extension.name}" 
                    description="${extension.description}" 
                    guide="${extension.guide}"
                    namespace="${extension.namespace}"
                    artifact="${extension.artifact}"
                    shortName="${extension.shortName}"
                    keywords="${extension.keywords}"
                    status="${extension.status}"
                    configFilter="${extension.configFilter}"
                    categories="${extension.categories}"
                    unlisted="${extension.unlisted}"
                    builtWith="${extension.builtWith}"
                    providesCapabilities="${extension.providesCapabilities}"
                    extensionDependencies="${extension.extensionDependencies}">
                    <div class="card-content" slot="content">
                        ${extension.description}
                    </div>    
                </qwc-extension>        
            `;
        }
    }

}
customElements.define('qwc-extensions', QwcExtensions);
