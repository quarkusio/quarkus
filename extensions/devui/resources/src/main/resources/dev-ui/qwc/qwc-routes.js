import { LitElement, html, css} from 'lit';
import { basepath } from 'devui-data';
import '@vaadin/progress-bar';
import '@vaadin/grid';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import '@vaadin/grid/vaadin-grid-sort-column.js';

/**
 * This component show all available routes
 */
export class QwcRoutes extends LitElement {
    
    static styles = css`
        .infogrid {
            width: 99%;
            height: 99%;
        }
        a {
            cursor: pointer;
            color: var(--lumo-body-text-color);
        }
        a:link { 
            text-decoration: none;
            color: var(--lumo-body-text-color); 
        }
        a:visited { 
            text-decoration: none;
            color: var(--lumo-body-text-color); 
        }
        a:active { 
            text-decoration: none; 
            color: var(--lumo-body-text-color);
        }
        a:hover {
            color: var(--quarkus-red);
        }
        .contextHandler {
            color: var(--lumo-success-text-color);
        }
        .failureHandler{
            color: var(--lumo-error-text-color);
        }
    `;

    static properties = {
        _routes: {state: true},
    }

    constructor() {
        super();
        this._routes = null;
    }

    async connectedCallback() {
        super.connectedCallback();
        await this.load();
    }
        
    async load() {
        const response = await fetch(basepath + "/endpoints/routes.json");
        const data = await response.json();
        this._routes = data.filter(item => item.contextHandlers);;
    }

    render() {
        if (this._routes) {
            return html`<vaadin-grid .items="${this._routes}" class="infogrid">
                        <vaadin-grid-sort-column resizable
                                            header='Handler'
                                            path="contextHandlers"
                                            auto-width
                                            ${columnBodyRenderer(this._contextHandlersRenderer, [])}>>
                        </vaadin-grid-sort-column>

                        <vaadin-grid-sort-column resizable
                                            header="Order"     
                                            path="order"
                                            auto-width>
                        </vaadin-grid-sort-column>
            
                        <vaadin-grid-sort-column resizable
                                            header="Path"
                                            path="path"
                                            auto-width
                                            ${columnBodyRenderer(this._pathRenderer, [])}>>
                        </vaadin-grid-sort-column>
            
                    </vaadin-grid>`;
        }else{
            return html`
            <div style="color: var(--lumo-secondary-text-color);width: 95%;" >
                <div>Fetching routes...</div>
                <vaadin-progress-bar indeterminate></vaadin-progress-bar>
            </div>
            `;
        }
    }
    
    _pathRenderer(route){
        let path = route.path.replace(/^'|'$/g, '');
        
        if(path === "null"){
            return "";
        }
        return path;
    }
    
    _contextHandlersRenderer(route){
        const contextHandler = this._extractClassName(route.contextHandlers);
        if(contextHandler){
            return html`<code title="Context Handler" class="contextHandler">${contextHandler}</code>`;
        }else{
            const failureHandler = this._extractClassName(route.failureHandlers);
            if(failureHandler){
                return html`<code title="Failure Handler" class="failureHandler">${failureHandler}</code>`;
            }
        }
        return html`<code>Unknown (could not detect)</code>`;
    }
    
    _extractClassName(handler){
        const regex = /([^$]+)*/;
        const match = handler.match(regex);
        let className = match ? match[1] : handler;
        if(className.startsWith("[")){
            className = className.substring(1);
        }
        if(className.endsWith("]")){
            className = className.substring(0, className.length-1);
        }
        if(className === "null"){
            return null;
        }
        
        return className;
    }
    
}
customElements.define('qwc-routes', QwcRoutes);
