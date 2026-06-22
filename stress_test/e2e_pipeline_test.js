const fetch = globalThis.fetch;

async function runE2ETest() {
    console.log('--- Rozpoczynanie testu E2E potoku CookMate ---');
    const gatewayUrl = 'http://127.0.0.1:8085';
    const keycloakUrl = 'http://localhost:8080/realms/cookmate/protocol/openid-connect/token';
    
    // 1. Logowanie do Keycloak
    console.log('[1/6] Pobieranie tokenu z Keycloak dla test.user...');
    const loginBody = new URLSearchParams({
        client_id: 'cookmate-client',
        client_secret: 'cookmate-secret',
        grant_type: 'password',
        username: 'test.user',
        password: 'test12345'
    });
    
    const loginRes = await fetch(keycloakUrl, {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: loginBody.toString()
    });
    
    if (!loginRes.ok) {
        throw new Error(`Login failed: ${loginRes.status} ${await loginRes.text()}`);
    }
    const token = (await loginRes.json()).access_token;
    console.log('Token uzyskany pomyślnie!');
    const payload = JSON.parse(Buffer.from(token.split('.')[1], 'base64').toString());
    console.log('Token issuer:', payload.iss);
    
    const authHeaders = {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
    };

    const mainUrl = 'http://localhost:8081';
    const simulatorUrl = 'http://localhost:8082';
    const sessionUrl = 'http://localhost:8083';
    
    // 2. Upewnij się, że są kroki dla przepisu 52772 (wygeneruj, jeśli nie ma)
    console.log('[2/6] Sprawdzanie kroków dla przepisu 52772...');
    const recipeId = '52772';
    const stepsCheckRes = await fetch(`${mainUrl}/api/recipes/${recipeId}/steps`, { headers: authHeaders });
    if (!stepsCheckRes.ok) throw new Error(`Steps check failed: ${stepsCheckRes.status}`);
    const steps = await stepsCheckRes.json();
    if (!Array.isArray(steps) || steps.length === 0) {
        console.log('Brak kroków. Generowanie przez Main Service (Groq)...');
        const genRes = await fetch(`${mainUrl}/api/steps/generate`, {
            method: 'POST',
            headers: authHeaders,
            body: JSON.stringify({ mealId: recipeId })
        });
        if (!genRes.ok) throw new Error(`Generate steps failed: ${genRes.status}`);
        console.log('Kroki wygenerowane.');
    } else {
        console.log(`Kroki przepisu istnieją (${steps.length} kroków).`);
    }

    // 3. Rozpoczęcie sesji symulacyjnej
    console.log('[3/6] Rozpoczynanie sesji symulacyjnej (Simulator Service)...');
    const startRes = await fetch(`${simulatorUrl}/api/simulator/sessions/start`, {
        method: 'POST',
        headers: authHeaders,
        body: JSON.stringify({ recipeId })
    });
    
    if (!startRes.ok) {
        const body = await startRes.text();
        throw new Error(`Failed to start simulation: ${startRes.status} - ${body}`);
    }
    const startData = await startRes.json();
    const sessionId = startData.sessionId;
    console.log(`Sesja symulacyjna utworzona: ${sessionId}`);
    
    // 4. Odczytanie aktywnej sesji (Session Service)
    console.log('[4/6] Pobieranie globalnej aktywnej sesji z Cooking Session Service...');
    const activeSessionRes = await fetch(`${sessionUrl}/api/cooking-sessions/active`, { headers: authHeaders });
    if (!activeSessionRes.ok) {
        const body = await activeSessionRes.text();
        throw new Error(`Failed to get active session: ${activeSessionRes.status} - ${body}`);
    }
    const activeSessionData = await activeSessionRes.json();
    console.log(`Aktywna sesja: ${JSON.stringify(activeSessionData)}`);

    // 5. Wykonanie pierwszego kroku
    console.log('[5/6] Wykonywanie kroku 1 w symulatorze...');
    const execRes = await fetch(`${simulatorUrl}/api/simulator/sessions/${sessionId}/steps/execute`, {
        method: 'POST',
        headers: authHeaders
    });
    if (!execRes.ok) {
        const body = await execRes.text();
        throw new Error(`Failed to execute step: ${execRes.status} - ${body}`);
    }
    const execData = await execRes.json();
    console.log(`Krok wykonany pomyślnie. Postęp w symulatorze: krok ${execData.stepNumber}`);
    
    // 6. Weryfikacja synchronizacji (czy Simulator wysłał postęp do Session Service)
    console.log('[6/6] Sprawdzanie czy postęp został zarejestrowany w Session Service...');
    
    await new Promise(r => setTimeout(r, 1000));
    
    const historyRes = await fetch(`${sessionUrl}/api/cooking-sessions/recipes/${recipeId}/history`, { headers: authHeaders });
    if (!historyRes.ok) {
        const body = await historyRes.text();
        throw new Error(`Failed to get session history: ${historyRes.status} - ${body}`);
    }
    const historyData = await historyRes.json();
    console.log(`Historia sesji gotowania:`, JSON.stringify(historyData, null, 2));
    
    const hasExecutedStep = historyData.some(h => h.status === 'EXECUTED' && h.stepNumber === 1);
    if (!hasExecutedStep) {
        throw new Error('TEST FAILED: Session Service nie zarejestrował wykonanego kroku! Prawdopodobnie brak autoryzacji z Simulator -> Session Service.');
    }
    
    console.log('--- TEST E2E ZAKOŃCZONY SUKCESEM! ---');
}

runE2ETest().catch(e => {
    console.error('BŁĄD PODCZAS TESTU E2E:', e);
    process.exit(1);
});
