param(
    [string]$ConfigPath = ".\scripts\auth-config.yml",
    [string]$ItemCode = "ITEM-001"
)

if (-not (Test-Path $ConfigPath)) {
    Write-Error "Config file not found: $ConfigPath"
    exit 1
}

$config = @{
    apiBaseUrl = $null
    username = $null
    password = $null
}

Get-Content $ConfigPath | ForEach-Object {
    if ($_ -match '^\s*apiBaseUrl:\s*"?(.+?)"?\s*$') { $config.apiBaseUrl = $matches[1] }
    elseif ($_ -match '^\s*username:\s*"?(.+?)"?\s*$') { $config.username = $matches[1] }
    elseif ($_ -match '^\s*password:\s*"?(.+?)"?\s*$') { $config.password = $matches[1] }
}

if (-not $config.apiBaseUrl -or -not $config.username -or -not $config.password) {
    Write-Error "Missing apiBaseUrl/username/password in $ConfigPath"
    exit 1
}

$loginBody = @{
    username = $config.username
    password = $config.password
} | ConvertTo-Json

$login = Invoke-RestMethod -Method Post -Uri "$($config.apiBaseUrl)/api/auth/login" `
    -ContentType "application/json" -Body $loginBody

$token = $login.token
if (-not $token) {
    Write-Error "Login failed: token not returned."
    exit 1
}

Invoke-RestMethod -Method Get -Uri "$($config.apiBaseUrl)/api/inventory/$ItemCode" `
    -Headers @{ Authorization = "Bearer $token" }
