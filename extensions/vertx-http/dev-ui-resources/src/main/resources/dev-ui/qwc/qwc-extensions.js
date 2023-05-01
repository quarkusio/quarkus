import { LitElement, html, css} from 'lit';
import { unsafeHTML } from 'lit/directives/unsafe-html.js';
import { RouterController } from 'router-controller';
import { devuiState } from 'devui-state';
import { observeState } from 'lit-element-state';
import 'qwc/qwc-extension.js';
import 'qwc/qwc-extension-link.js';
import 'qui-badge';


/**
 * This component create cards of all the extensions
 */
export class QwcExtensions extends observeState(LitElement) {
    routerController = new RouterController(this);
    
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
            color: var(--lumo-contrast-50pct);
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
         .float-right {
            align-self: flex-end;
        }
       `;

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
                this.routerController.addRouteForExtension(page);
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
        let customCardCode = `<${extension.card.componentName} 
                                class="card-content"
                                slot="content"
                                extensionName="${extension.name}"
                                description="${extension.description}"
                                guide="${extension.guide}"
                                namespace="${extension.namespace}">

                             </${extension.card.componentName}>`;

        return html`${unsafeHTML(customCardCode)}`;

    }

    _renderDefaultCardContent(extension){
        return html`
            <div class="card-content" slot="content">
                <span class="description">
                    ${this._renderExperimentalBadge(extension)}
                    ${extension.description}
                </span>
                ${this._renderCardLinks(extension)}
            </div>`;
    }

    _renderCardLinks(extension){

        return html`${extension.cardPages.map(page => html`
                            <qwc-extension-link slot="link"
                                namespace="${extension.namespace}"
                                extensionName="${extension.name}"
                                iconName="${page.icon}"
                                displayName="${page.title}"
                                staticLabel="${page.staticLabel}"
                                dynamicLabel="${page.dynamicLabel}"
                                streamingLabel="${page.streamingLabel}"
                                path="${page.id}"
                                ?embed=${page.embed}
                                externalUrl="${page.metadata.externalUrl}"
                                webcomponent="${page.componentLink}" >
                            </qwc-extension-link>
                        `)}`;
    }

    _renderInactive(extension){
        if(extension.unlisted === "false"){
            return html`<qwc-extension
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
                    ${this._renderExperimentalBadge(extension)}
                    ${extension.description}
                </div>    
            </qwc-extension>`;
        }
    }

    _renderExperimentalBadge(extension){
        if(extension.status === "experimental") {
            return html`<qui-badge level="warning" class="float-right" small><span>EXPERIMENTAL</span></qui-badge>`;
        }
    }

}
customElements.define('qwc-extensions', QwcExtensions);
