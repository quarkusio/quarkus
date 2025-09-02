import {LitElement, html, css} from 'lit';
import { JsonRpc } from 'jsonrpc';
import { notifier } from 'notifier';
import { observeState } from 'lit-element-state';
import { devuiState } from 'devui-state';

export class QuiIdeLink extends observeState(LitElement) {

    jsonRpc = new JsonRpc("devui-ide-interaction");

    static styles = css`
        span {
            cursor: pointer;
        }
        span:hover {
            text-decoration: underline;
        }
    `;

    static properties = {
        fileName: {type: String},
        lang: {type: String},
        lineNumber: {type: String},
        stackTraceLine: {type: String},
        _fontWeight: {type: String},
        noCheck: {type: Boolean},
    };

    constructor() {
        super();
        this.stackTraceLine = null;
        this.fileName = null;
        this.lang = "java";
        this.lineNumber = "0";
        this._fontWeight = "normal";
        this.noCheck = false;
    }
    
    connectedCallback() {
        super.connectedCallback();

        // Detect the value from a stack trace line
        if(this.stackTraceLine && devuiState.ideInfo.ideName && !this.stackTraceLine.includes(".zig") && this.stackTraceLine.includes(" ")){
            var parts = this.stackTraceLine.split(" ");
            // Make it clickable
            let classMethodFileNumber = parts[1];
            let classMethodFileNumberSplit = classMethodFileNumber.split("(");
            let classMethod = classMethodFileNumberSplit[0];
            let fileNameAndLineNumber = classMethodFileNumberSplit[1];
            if(fileNameAndLineNumber && fileNameAndLineNumber!==""){
                let p = fileNameAndLineNumber.split(":");
                if(p.length===2){
                    let lang = p[0].substring(p[0].lastIndexOf('.') + 1);
                    let lineNumber = p[1].substring(0, p[1].lastIndexOf(')'));
                    if(lineNumber && lineNumber!==""){
                        let givenClassName = classMethod.substring(0, classMethod.lastIndexOf('.'));
                        if(givenClassName && givenClassName!== "" && this._checkIfStringStartsWith(givenClassName, devuiState.ideInfo.idePackages)){
                            this.fileName = givenClassName;
                            this.lang = lang;
                            this.lineNumber = lineNumber;
                            this._fontWeight = "bold";
                        }
                    }
                }
            }
        }
    }

    _checkIfStringStartsWith(str, substrs) {
        return substrs.some(substr => {
            if(substr && substr.trim !== ""){
                return str.startsWith(substr);
            }
            return false;
        });
    }

    render() {
        if(this.fileName){
            if(this.noCheck || this._checkIfStringStartsWith(this.fileName, devuiState.ideInfo.idePackages)){
                return html`<span style="font-weight: ${this._fontWeight};" @click=${() => this._openInIde()}><slot></slot></span>`;
            }else{
                return html`<slot></slot>`;
            }
        }else if(this.stackTraceLine){
            return html`${this.stackTraceLine}`;
        }
    }

    _openInIde(){
        this.jsonRpc.open({
            'fileName':this.fileName,
            'lang':this.lang,
            'lineNumber': this.lineNumber
        }).then(jsonRpcResponse => {
            if(!jsonRpcResponse.result){
                notifier.showErrorMessage("Could not open your IDE");
            }
        });
    }
}

customElements.define('qui-ide-link', QuiIdeLink);
