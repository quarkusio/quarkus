export const api = () => {
    let path = window.location.pathname
    return path.replace('/kafka-dev-ui', '/kafka-admin')
}
export const ui = 'kafka-ui';