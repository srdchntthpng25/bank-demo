param(
    [string]$BaseUrl = "http://localhost:8080"
)

$ErrorActionPreference = "Stop"
$api = "$BaseUrl/api/v1"

function Invoke-CurlJson {
    param(
        [Parameter(Mandatory)] [string[]]$Arguments
    )

    $output = & curl.exe @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "curl failed with exit code $LASTEXITCODE"
    }
    if ([string]::IsNullOrWhiteSpace(($output -join ""))) {
        return $null
    }
    return (($output -join "") | ConvertFrom-Json)
}

Write-Host "Checking API: $BaseUrl"
$health = Invoke-CurlJson @("-sS", "$BaseUrl/actuator/health")
if ($health.status -ne "UP") {
    throw "API health is not UP: $($health | ConvertTo-Json -Compress)"
}
Write-Host "PASS health"

$suffix = [Guid]::NewGuid().ToString("N").Substring(0, 8)
$headers = @("-H", "Content-Type: application/json")

$from = Invoke-CurlJson @(
    "-sS", "-X", "POST", "$api/accounts",
    $headers,
    "-d", (@{
        ownerName = "Curl Alice $suffix"
        currency = "THB"
        initialBalance = 1000
    } | ConvertTo-Json -Compress)
)
$to = Invoke-CurlJson @(
    "-sS", "-X", "POST", "$api/accounts",
    $headers,
    "-d", (@{
        ownerName = "Curl Bob $suffix"
        currency = "THB"
        initialBalance = 500
    } | ConvertTo-Json -Compress)
)
Write-Host "PASS create accounts: from=$($from.id), to=$($to.id)"

$deposit = Invoke-CurlJson @(
    "-sS", "-X", "POST", "$api/accounts/$($from.id)/deposit",
    $headers,
    "-d", '{"amount":250}'
)
if ([decimal]$deposit.balance -ne 1250) {
    throw "Unexpected balance after deposit: $($deposit.balance)"
}
Write-Host "PASS deposit"

$withdraw = Invoke-CurlJson @(
    "-sS", "-X", "POST", "$api/accounts/$($from.id)/withdraw",
    $headers,
    "-d", '{"amount":100}'
)
if ([decimal]$withdraw.balance -ne 1150) {
    throw "Unexpected balance after withdraw: $($withdraw.balance)"
}
Write-Host "PASS withdraw"

$account = Invoke-CurlJson @("-sS", "$api/accounts/$($from.id)")
if ($account.id -ne $from.id -or $account.currency -ne "THB") {
    throw "Unexpected account response"
}
Write-Host "PASS get account"

$balance = Invoke-CurlJson @("-sS", "$api/accounts/$($from.id)/balance")
if ([decimal]$balance.balance -ne 1150) {
    throw "Unexpected account balance: $($balance.balance)"
}
Write-Host "PASS balance"

$idempotencyKey = "curl-transfer-$suffix"
$transferBody = @{
    fromAccountId = $from.id
    toAccountId = $to.id
    amount = 250
    currency = "THB"
} | ConvertTo-Json -Compress

$transfer = Invoke-CurlJson @(
    "-sS", "-X", "POST", "$api/transfers",
    $headers,
    "-H", "Idempotency-Key: $idempotencyKey",
    "-d", $transferBody
)
$sameTransfer = Invoke-CurlJson @(
    "-sS", "-X", "POST", "$api/transfers",
    $headers,
    "-H", "Idempotency-Key: $idempotencyKey",
    "-d", $transferBody
)
if ($transfer.transferId -ne $sameTransfer.transferId) {
    throw "Idempotency failed: transfer IDs differ"
}
Write-Host "PASS transfer and idempotency: transfer=$($transfer.transferId)"

$fromBalance = Invoke-CurlJson @("-sS", "$api/accounts/$($from.id)/balance")
$toBalance = Invoke-CurlJson @("-sS", "$api/accounts/$($to.id)/balance")
if ([decimal]$fromBalance.balance -ne 900 -or [decimal]$toBalance.balance -ne 750) {
    throw "Unexpected balances after transfer: from=$($fromBalance.balance), to=$($toBalance.balance)"
}
Write-Host "PASS transfer balances"

$transactions = Invoke-CurlJson @("-sS", "$api/accounts/$($from.id)/transactions?page=0&size=20")
if ($transactions.totalElements -lt 3) {
    throw "Expected at least three transactions"
}
Write-Host "PASS statement: $($transactions.totalElements) entries"

$updated = Invoke-CurlJson @(
    "-sS", "-X", "PATCH", "$api/accounts/$($to.id)/status",
    $headers,
    "-d", '{"status":"FROZEN"}'
)
if ($updated.status -ne "FROZEN") {
    throw "Unexpected status response: $($updated.status)"
}
Write-Host "PASS status update"

$transferLookup = Invoke-CurlJson @("-sS", "$api/transfers/$($transfer.transferId)")
if ($transferLookup.transferId -ne $transfer.transferId) {
    throw "Unexpected transfer lookup response"
}
Write-Host "PASS transfer lookup"
Write-Host "All curl API tests passed."
