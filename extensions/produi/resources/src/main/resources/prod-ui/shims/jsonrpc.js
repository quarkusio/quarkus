// JsonRPC shim for Prod UI
// Re-exports the Prod UI JsonRpc class as a bare 'jsonrpc' module
// so Dev UI extension components can import { JsonRpc } from 'jsonrpc'

export { JsonRpc } from '../controller/jsonrpc.js';
