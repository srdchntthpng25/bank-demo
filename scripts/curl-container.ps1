param(
    [string]$Path = "/actuator/health",
    [ValidateSet("GET", "POST", "PATCH")]
    [string]$Method = "GET",
    [string]$Body,
    [string]$IdempotencyKey
)

$ErrorActionPreference = "Stop"
$arguments = @("compose", "--profile", "test", "run", "--rm", "--no-deps", "api-test", "-sS", "-X", $Method, "http://app:8080$Path")

if ($Body) {
    $arguments += @("-H", "Content-Type: application/json", "-d", $Body)
}
if ($IdempotencyKey) {
    $arguments += @("-H", "Idempotency-Key: $IdempotencyKey")
}

& docker @arguments
if ($LASTEXITCODE -ne 0) {
    throw "Container curl failed with exit code $LASTEXITCODE"
}