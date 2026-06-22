const fs = require('fs');
const path = require('path');

const realmPath = path.join(__dirname, 'realm-export.json');
console.log('Reading realm file from:', realmPath);

if (!fs.existsSync(realmPath)) {
    console.error('Error: realm-export.json not found!');
    process.exit(1);
}

const realm = JSON.parse(fs.readFileSync(realmPath, 'utf8'));

// Clear any previously generated users except test.user
realm.users = realm.users.filter(u => u.username === 'test.user');

console.log('Generating 1000 users...');
for (let i = 1; i <= 1000; i++) {
    realm.users.push({
        username: `user${i}`,
        enabled: true,
        emailVerified: true,
        firstName: 'User',
        lastName: `${i}`,
        email: `user${i}@cookmate.local`,
        credentials: [
            {
                type: 'password',
                value: 'password123',
                temporary: false
            }
        ],
        realmRoles: [
            'ROLE_USER'
        ]
    });
}

fs.writeFileSync(realmPath, JSON.stringify(realm, null, 2), 'utf8');
console.log('Successfully added 1000 users to realm-export.json!');
