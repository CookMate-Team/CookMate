import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    setupTimeout: '180s', // Allow plenty of time to fetch 1000 tokens
    stages: [
        { duration: '30s', target: 100 },  // Ramp-up to 100 users
        { duration: '1m', target: 1000 },  // Ramp-up to 1000 users
        { duration: '2m', target: 1000 },  // Stay at 1000 users
        { duration: '30s', target: 0 },    // Ramp-down
    ],
    thresholds: {
        http_req_failed: ['rate<0.05'], // HTTP error rate must be < 5%
        http_req_duration: ['p(95)<2000'], // 95% of requests must complete under 2s
    },
};

// Setup phase: pre-fetch OIDC tokens for all 1000 users and ensure recipe steps exist
export function setup() {
    const tokens = [];
    const loginUrl = 'http://localhost:8080/realms/cookmate/protocol/openid-connect/token';
    
    console.log('Fetching tokens for 1000 users...');
    
    // Use http.batch to fetch tokens in parallel batches of 50
    const batchSize = 50;
    for (let i = 1; i <= 1000; i += batchSize) {
        const batch = [];
        const limit = Math.min(i + batchSize - 1, 1000);
        for (let j = i; j <= limit; j++) {
            batch.push({
                method: 'POST',
                url: loginUrl,
                body: {
                    client_id: 'cookmate-client',
                    client_secret: 'cookmate-secret',
                    grant_type: 'password',
                    username: `user${j}`,
                    password: 'password123',
                },
                params: {
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                },
            });
        }
        
        const responses = http.batch(batch);
        for (let r = 0; r < responses.length; r++) {
            const res = responses[r];
            if (res.status === 200) {
                const token = JSON.parse(res.body).access_token;
                tokens.push(token);
            } else {
                console.error(`Failed to login user: status=${res.status}, body=${res.body}`);
            }
        }
    }
    
    console.log(`Successfully fetched ${tokens.length} tokens.`);

    // Ensure recipe steps exist BEFORE load test starts (generate via Groq if missing)
    if (tokens.length > 0) {
        const setupHeaders = {
            'Authorization': `Bearer ${tokens[0]}`,
            'Content-Type': 'application/json',
        };
        const recipesToCheck = ['52772', '52773', '52774'];
        for (const recipeId of recipesToCheck) {
            const stepsCheckRes = http.get(`http://localhost:8081/api/recipes/${recipeId}/steps`, { headers: setupHeaders });
            let stepsExist = false;
            if (stepsCheckRes.status === 200) {
                const steps = JSON.parse(stepsCheckRes.body);
                stepsExist = Array.isArray(steps) && steps.length > 0;
            }

            if (!stepsExist) {
                console.log(`Recipe steps not found for ${recipeId}, generating via Groq AI (this may take ~20s)...`);
                const genRes = http.post(
                    'http://localhost:8081/api/steps/generate',
                    JSON.stringify({ mealId: recipeId }),
                    { headers: setupHeaders, timeout: '60s' }
                );
                if (genRes.status === 200) {
                    const count = JSON.parse(genRes.body).steps?.length || 0;
                    console.log(`Generated ${count} steps for recipe ${recipeId}.`);
                } else {
                    console.error(`Failed to generate steps for ${recipeId}: ${genRes.status} ${genRes.body}`);
                }
            } else {
                console.log(`Recipe ${recipeId} steps already exist in DB, skipping generation.`);
            }
        }
    }
    
    return { tokens };
}

export default function (data) {
    const vu = __VU;
    // Get the pre-fetched token for this virtual user (use modulo if VU exceeds 1000)
    const token = data.tokens[(vu - 1) % data.tokens.length];
    
    if (!token) {
        return;
    }
    
    const headers = {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json',
    };
    
    const gatewayUrl = 'http://localhost:8085';
    
    // Distribute VUs evenly across the three generated recipes
    const recipes = ['52772', '52773', '52774'];
    const recipeId = recipes[(vu - 1) % recipes.length];
    
    // 1. Start simulation session for chosen recipe
    const startPayload = JSON.stringify({ recipeId: recipeId });
    const startRes = http.post(`${gatewayUrl}/api/simulator/sessions/start`, startPayload, { headers });
    
    if (!check(startRes, {
        'start session status is 201': (r) => r.status === 201,
        'has sessionId': (r) => r.json().sessionId !== undefined,
    })) {
        console.error(`Start failed for recipe ${recipeId}: ${startRes.status} ${startRes.body}`);
        return;
    }
    
    const sessionId = startRes.json().sessionId;
    const totalSteps = startRes.json().totalSteps || 2;
    
    // Simulate reading instructions before execution
    sleep(1);
    
    // 2. Execute steps one by one
    for (let step = 1; step <= totalSteps; step++) {
        const execRes = http.post(`${gatewayUrl}/api/simulator/sessions/${sessionId}/steps/execute`, null, { headers });
        
        check(execRes, {
            'execute step status is 200': (r) => r.status === 200,
            'step success': (r) => r.json().success === true,
        });
        
        // Simulate preparation time between steps
        sleep(0.5 + Math.random() * 0.5);
    }
}
