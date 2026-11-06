# PowerShell script to ensure gradle-wrapper.jar is present
$wrapperDir = Join-Path $PSScriptRoot "..\gradle\wrapper"
$wrapperJar = Join-Path $wrapperDir "gradle-wrapper.jar"

if (-not (Test-Path $wrapperDir)) {
    New-Item -ItemType Directory -Path $wrapperDir -Force | Out-Null
}

if (-not (Test-Path $wrapperJar)) {
    Write-Host "Downloading gradle-wrapper.jar from official Gradle repo..." -ForegroundColor Cyan
    $url = "https://raw.githubusercontent.com/gradle/gradle/v8.6.0/gradle/wrapper/gradle-wrapper.jar"
    try {
        [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
        Invoke-WebRequest -Uri $url -OutFile $wrapperJar -UseBasicParsing
        Write-Host "gradle-wrapper.jar successfully downloaded to $wrapperJar" -ForegroundColor Green
    } catch {
        Write-Warning "Direct raw download failed ($($_.Exception.Message)). Attempting maven fallback..."
        $fallbackUrl = "https://repo.maven.apache.org/maven2/org/gradle/gradle-tooling-api/8.6/gradle-tooling-api-8.6.jar"
        Invoke-WebRequest -Uri $fallbackUrl -OutFile $wrapperJar -UseBasicParsing
    }
} else {
    Write-Host "gradle-wrapper.jar is already present." -ForegroundColor Green
}
