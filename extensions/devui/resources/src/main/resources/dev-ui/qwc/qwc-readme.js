import { LitElement, html, css} from 'lit';
import MarkdownIt from 'markdown-it';
import { unsafeHTML } from 'lit/directives/unsafe-html.js';
import { JsonRpc } from 'jsonrpc';
import { readme } from 'devui-data';

/**
 * This component shows the Readme page
 */
export class QwcReadme extends LitElement {

  jsonRpc = new JsonRpc("devui-readme", true);
  
  static styles = css`
  .readme {
        padding: 15px;
    }
    a {
        color:var(--quarkus-blue);
    }
  `;

  static properties = {
      _readme: {state:true},
  };

  constructor() {
    super();
    this.md = new MarkdownIt();
    this._readme = readme;
  }

  connectedCallback() {
    super.connectedCallback();
    this._observer = this.jsonRpc.streamReadme().onNext(jsonRpcResponse => { 
        this._readme = jsonRpcResponse.result;
    }); 
  }  

  render() {
    if(this._readme){
        const htmlContent = this.md.render(this._readme);
        return html`<div class="readme">${unsafeHTML(htmlContent)}</div>`;
    }
  }

}
customElements.define('qwc-readme', QwcReadme);