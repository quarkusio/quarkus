function addTrailingZero(data) {
    if (data < 10) {
        return "0" + data;
    }
    return data;
}

export default function timestampToFormattedString(UNIX_timestamp) {
    const a = new Date(UNIX_timestamp);
    const year = a.getFullYear();
    const month = addTrailingZero(a.getMonth());
    const date = addTrailingZero(a.getDate());
    const hour = addTrailingZero(a.getHours());
    const min = addTrailingZero(a.getMinutes());
    const sec = addTrailingZero(a.getSeconds());
    return date + '/' + month + '/' + year + ' ' + hour + ':' + min + ':' + sec;
}