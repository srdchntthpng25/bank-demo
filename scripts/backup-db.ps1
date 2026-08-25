param(
    [string]$OutputDirectory = ".\backups",
    [string]$Database = "bank",
    [string]$Username = "sa",
    [string]$Password = $env:DB_PASSWORD
)

$ErrorActionPreference = "Stop"
if ([string]::IsNullOrWhiteSpace($Password)) {
    throw "Set DB_PASSWORD before running the backup"
}
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$fileName = "$Database-$timestamp.bak"
$containerPath = "/var/opt/mssql/data/$fileName"
$hostDirectory = [System.IO.Path]::GetFullPath($OutputDirectory)
$hostPath = Join-Path $hostDirectory $fileName

New-Item -ItemType Directory -Force -Path $hostDirectory | Out-Null

Write-Host "Creating SQL Server backup: $fileName"
docker compose exec -T sqlserver /opt/mssql-tools18/bin/sqlcmd `
    -S localhost -U $Username -P $Password -C `
    -Q "BACKUP DATABASE [$Database] TO DISK = N'$containerPath' WITH COPY_ONLY, INIT"
if ($LASTEXITCODE -ne 0) {
    throw "SQL Server backup failed"
}

Write-Host "Copying backup to: $hostPath"
docker compose cp "sqlserver:$containerPath" $hostPath
if ($LASTEXITCODE -ne 0) {
    throw "Could not copy backup out of the container"
}

Write-Host "Backup completed: $hostPath"