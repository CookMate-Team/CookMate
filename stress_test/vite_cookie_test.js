const crypto = require('crypto');

async function testAuthFlow() {
  console.log('Starting OAuth2 Authorization Code flow...');

  // 1. Hit Gateway protected endpoint to initiate login
  const loginRes = await fetch('http://localhost:5173/oauth2/authorization/keycloak', { redirect: 'manual' });
  const location = loginRes.headers.get('location');
  const gwInitCookie = loginRes.headers.get('set-cookie');
  let gwSessionCookie = gwInitCookie ? gwInitCookie.split(';')[0] : '';
  console.log('Gateway init cookie:', gwSessionCookie);

  console.log('Redirecting to:', location);
  let kcCookies = [];

  // 2. Follow redirect to Keycloak auth endpoint
  const authRes = await fetch(location, { redirect: 'manual' });
  const authHtml = await authRes.text();
  const setCookieHeader = authRes.headers.get('set-cookie');
  if (setCookieHeader) {
      kcCookies.push(setCookieHeader.split(';')[0]);
  }
  
  // Extract form action URL
  const actionMatch = authHtml.match(/action="([^"]+)"/);
  if (!actionMatch) {
    console.log('Could not find login form action URL. AuthHtml:', authHtml);
    return;
  }
  const actionUrl = actionMatch[1].replace(/&amp;/g, '&');
  console.log('Login form action URL:', actionUrl);

  // 3. Post credentials to Keycloak
  const loginBody = new URLSearchParams();
  loginBody.append('username', 'test.user');
  loginBody.append('password', 'test12345');

  const postRes = await fetch(actionUrl, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
      'Cookie': kcCookies.join('; ')
    },
    body: loginBody.toString(),
    redirect: 'manual'
  });

  const postLocation = postRes.headers.get('location');
  console.log('Post login redirect to:', postLocation);
  
  if (!postLocation) {
      console.log('Login failed?', await postRes.text());
      return;
  }

  // 4. Follow redirect back to Gateway
  const callbackRes = await fetch(postLocation, { 
    headers: { 'Cookie': gwSessionCookie },
    redirect: 'manual' 
  });
  const sessionCookieHeader = callbackRes.headers.get('set-cookie');
  console.log('Gateway callback response status:', callbackRes.status);
  console.log('Gateway Set-Cookie:', sessionCookieHeader);

  let sessionCookie = '';
  if (sessionCookieHeader) {
      sessionCookie = sessionCookieHeader.split(';')[0];
  }

  // 5. Test APIs with the session cookie
  console.log('\nTesting /api/v1/users/me with session...');
  const meRes = await fetch('http://localhost:5173/api/v1/users/me', {
    headers: { 'Cookie': sessionCookie }
  });
  console.log('Status:', meRes.status);
  console.log('Body:', await meRes.text());

  console.log('\nTesting /api/recipes/52956/steps with session...');
  const stepsRes = await fetch('http://localhost:5173/api/recipes/52956/steps', {
    headers: { 'Cookie': sessionCookie }
  });
  console.log('\nTesting concurrent requests...');
  const promises = [];
  for(let i = 0; i < 5; i++) {
    promises.push(fetch('http://localhost:5173/api/cooking-sessions/active', { headers: { 'Cookie': sessionCookie } }));
    promises.push(fetch('http://localhost:5173/api/recipes/52772/steps', { headers: { 'Cookie': sessionCookie } }));
  }
  const results = await Promise.all(promises);
  for(let i = 0; i < results.length; i++) {
    console.log(`Req ${i} Status:`, results[i].status);
  }
}

testAuthFlow().catch(console.error);
