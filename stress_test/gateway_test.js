const http = require('http');

async function check() {
  console.log('Testing /api/v1/users/me without session...');
  const res1 = await fetch('http://localhost:8085/api/v1/users/me');
  console.log('Status:', res1.status);
  const text1 = await res1.text();
  console.log('Body:', text1);

  console.log('\nTesting /api/recipes/52772/steps without session...');
  const res2 = await fetch('http://localhost:8085/api/recipes/52772/steps');
  console.log('Status:', res2.status);
  const text2 = await res2.text();
  console.log('Body:', text2);
}
check();
