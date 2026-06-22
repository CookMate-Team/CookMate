# Generate steps for recipes 52772, 52773, 52774 and verify persistence

# -------------------------------------------------------------------------
# 1. Obtain JWT token (Keycloak)
# -------------------------------------------------------------------------
$tokenResponse = Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/realms/cookmate/protocol/openid-connect/token" `
  -Body @{
    client_id="cookmate-client";
    client_secret="cookmate-secret";
    grant_type="password";
    username="user1";
    password="password123"
  } -ContentType "application/x-www-form-urlencoded"

$token = $tokenResponse.access_token
Write-Host "[OK] Token obtained"

# -------------------------------------------------------------------------
# 2. List of recipe IDs we care about
# -------------------------------------------------------------------------
$recipeIds = @("52772","52773","52774")

# -------------------------------------------------------------------------
# 3. For each recipe: generate steps, then verify count in DB
# -------------------------------------------------------------------------
foreach ($id in $recipeIds) {
    # ---- generate
    $response = Invoke-RestMethod -Method Post `
      -Uri "http://localhost:8081/api/steps/generate" `
      -Headers @{ Authorization = "Bearer $token"; "Content-Type"="application/json" } `
      -Body (ConvertTo-Json @{ mealId = $id }) `
      -ErrorAction SilentlyContinue

    if ($response) {
        $count = if ($null -ne $response.steps) { $response.steps.Count } else { 0 }
        Write-Host "[SUCCESS] Generated $count steps for recipe $id (Status 200)"
    } else {
        Write-Host "[ERROR] Failed to generate steps for recipe $id"
        continue
    }

    # ---- verify persistence (SQL query inside PowerShell)
    $verifyCmd = "SELECT COUNT(*) FROM steps WHERE recipe_id = '$id';"
    $verifyResult = docker exec -i cookmate-postgres psql -U cookmate -d cookmate -t -c $verifyCmd
    $persisted = $verifyResult.Trim()
    Write-Host "   -> DB now holds $persisted step(s) for recipe $id"
}
