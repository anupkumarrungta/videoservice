# Set JAVA_HOME
$env:JAVA_HOME = "C:\Program Files\Java\jdk-23"
Write-Host "JAVA_HOME set to: $env:JAVA_HOME"

# Load .env variables into the current session
if (Test-Path .env) {
    Get-Content .env | ForEach-Object {
        if ($_ -match "^(.*?)=(.*)$") {
            [System.Environment]::SetEnvironmentVariable($matches[1], $matches[2])
            Write-Host "Loaded environment variable: $($matches[1])"
        }
    }
} else {
    Write-Host "Warning: .env file not found"
}

# Run the Spring Boot application
Write-Host "Starting Spring Boot application..."
./mvnw spring-boot:run 