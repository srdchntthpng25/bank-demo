param(
    [string]$BaseUrl = "http://localhost:8080"
)

$ErrorActionPreference = "Stop"
$api = "$BaseUrl/api/v1"

function Invoke-Api {
    param(
        [ValidateSet("GET", "POST", "PATCH")]
        [string]$Method,
        [string]$Path,
        [object]$Body,
        [hashtable]$Headers = @{}
    )

    $request = @{
        Method = $Method
        Uri = "$api$Path"
        Headers = $Headers
        ContentType = "application/json"
    }
    if ($null -ne $Body) {
        $request.Body = ($Body | ConvertTo-Json -Compress)
    }

    try {
        return Invoke-RestMethod @request
    } catch {
        $response = $_.Exception.Response
        if ($null -ne $response) {
            $reader = New-Object System.IO.StreamReader($response.GetResponseStream())
            $detail = $reader.ReadToEnd()
            throw "[$Method $Path] HTTP $([int]$response.StatusCode): $detail"
        }
        throw
    }
}

Write-Host "Checking API: $BaseUrl"
for ($attempt = 1; $attempt -le 30; $attempt++) {
    try {
        $health = Invoke-RestMethod "$BaseUrl/actuator/health"
        if ($health.status -eq "UP") {
            break
        }
        Write-Host "Waiting for API health (attempt $attempt/30): $($health.status)"
    } catch {
        if ($attempt -eq 30) {
            $response = $_.Exception.Response
            if ($null -ne $response) {
                $reader = New-Object System.IO.StreamReader($response.GetResponseStream())
                $detail = $reader.ReadToEnd()
                throw "Health check failed with HTTP $([int]$response.StatusCode): $detail"
            }
            throw
        }
        Write-Host "Waiting for API to start (attempt $attempt/30)"
    }
    Start-Sleep -Seconds 2
}
if ($health.status -ne "UP") {
    throw "API health is not UP: $($health | ConvertTo-Json -Compress)"
}
Write-Host "PASS health"

$suffix = [Guid]::NewGuid().ToString("N").Substring(0, 8)
$from = Invoke-Api POST "/accounts" @{
    ownerName = "Alice $suffix"
    currency = "THB"
    initialBalance = 1000
}
$to = Invoke-Api POST "/accounts" @{
    ownerName = "Bob $suffix"
    currency = "THB"
    initialBalance = 100
}
Write-Host "PASS create accounts: from=$($from.id), to=$($to.id)"

$deposit = Invoke-Api POST "/accounts/$($from.id)/deposit" @{ amount = 500 }
if ([decimal]$deposit.balance -ne 1500) {
    throw "Unexpected balance after deposit: $($deposit.balance)"
}
Write-Host "PASS deposit"

$balance = Invoke-Api GET "/accounts/$($from.id)/balance"
if ([decimal]$balance.balance -ne 1500) {
    throw "Unexpected account balance: $($balance.balance)"
}
Write-Host "PASS balance"

$idempotencyKey = "test-$suffix"
$transferBody = @{
    fromAccountId = $from.id
    toAccountId = $to.id
    amount = 250
    currency = "THB"
}
$transfer = Invoke-Api POST "/transfers" $transferBody @{ "Idempotency-Key" = $idempotencyKey }
$sameTransfer = Invoke-Api POST "/transfers" $transferBody @{ "Idempotency-Key" = $idempotencyKey }
if ($transfer.transferId -ne $sameTransfer.transferId) {
    throw "Idempotency failed: transfer IDs differ"
}
Write-Host "PASS transfer and idempotency: transfer=$($transfer.transferId)"

$fromBalance = Invoke-Api GET "/accounts/$($from.id)/balance"
$toBalance = Invoke-Api GET "/accounts/$($to.id)/balance"
if ([decimal]$fromBalance.balance -ne 1250 -or [decimal]$toBalance.balance -ne 350) {
    throw "Unexpected balances after transfer: from=$($fromBalance.balance), to=$($toBalance.balance)"
}
Write-Host "PASS balances after transfer"

$transactions = Invoke-Api GET "/accounts/$($from.id)/transactions?page=0&size=20"
if ($transactions.totalElements -lt 2) {
    throw "Expected at least two transactions"
}
Write-Host "PASS transactions: $($transactions.totalElements) entries"
Write-Host "All API tests passed."