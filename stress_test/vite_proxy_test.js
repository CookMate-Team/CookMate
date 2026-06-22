const http = require('http');

const req = http.request('http://localhost:5173/oauth2/authorization/keycloak', {
    method: 'GET',
}, (res) => {
    console.log('Status:', res.statusCode);
    console.log('Headers:', res.headers);
    res.on('data', () => {});
});

req.end();
