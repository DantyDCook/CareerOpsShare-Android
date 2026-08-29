[CmdletBinding()]
param(
    [string]$Repository = "DantyDCook/CareerOpsShare-Android",
    [string]$Alias = "careerops-share-release",
    [string]$KeystorePath = "$env:USERPROFILE\.careerops\signing\careerops-share-release.p12",
    [switch]$ConfigureGitHubSecrets
)

$ErrorActionPreference = "Stop"

function ConvertFrom-SecureStringPlainText {
    param([Parameter(Mandatory)][Security.SecureString]$SecureValue)

    $ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($SecureValue)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr)
    }
    finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr)
    }
}

function Resolve-Keytool {
    $command = Get-Command keytool -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }

    $candidates = @()
    if ($env:JAVA_HOME) {
        $candidates += (Join-Path $env:JAVA_HOME "bin\keytool.exe")
    }
    if ($env:ProgramFiles) {
        $candidates += (Join-Path $env:ProgramFiles "Android\Android Studio\jbr\bin\keytool.exe")
    }
    if (${env:ProgramFiles(x86)}) {
        $candidates += (Join-Path ${env:ProgramFiles(x86)} "Android\Android Studio\jbr\bin\keytool.exe")
    }
    if ($env:LOCALAPPDATA) {
        $candidates += (Join-Path $env:LOCALAPPDATA "Programs\Android Studio\jbr\bin\keytool.exe")
        $candidates += (Join-Path $env:LOCALAPPDATA "Android\Sdk\jbr\bin\keytool.exe")
    }

    foreach ($candidate in $candidates | Select-Object -Unique) {
        if ($candidate -and (Test-Path $candidate)) {
            return $candidate
        }
    }

    throw "keytool was not found on PATH, JAVA_HOME, or common Android Studio JBR locations. In Android Studio check Help -> About / JDK location, or locate jbr\bin\keytool.exe and add its directory to PATH."
}

$keytoolPath = Resolve-Keytool
Write-Host "Using keytool: $keytoolPath" -ForegroundColor DarkGray

$keystoreFullPath = [IO.Path]::GetFullPath($KeystorePath)
$keystoreDirectory = Split-Path -Parent $keystoreFullPath

if (Test-Path $keystoreFullPath) {
    throw "Refusing to overwrite an existing keystore: $keystoreFullPath`nBack it up and pass a different -KeystorePath only if you intentionally want a different signing identity."
}

New-Item -ItemType Directory -Force -Path $keystoreDirectory | Out-Null

Write-Host ""
Write-Host "Creating permanent CareerOps Share signing key" -ForegroundColor Cyan
Write-Host "Keystore: $keystoreFullPath"
Write-Host "Alias:    $Alias"
Write-Host ""
Write-Host "IMPORTANT:" -ForegroundColor Yellow
Write-Host "- Choose a strong password and store it securely."
Write-Host "- For PKCS12, use the same password for the key and keystore."
Write-Host "- This keystore becomes the permanent app signing identity."
Write-Host ""

& $keytoolPath `
    -genkeypair `
    -v `
    -keystore $keystoreFullPath `
    -storetype PKCS12 `
    -alias $Alias `
    -keyalg RSA `
    -keysize 4096 `
    -validity 10000 `
    -dname "CN=CareerOps Share, OU=Mobile, O=CareerOps, C=US"

if ($LASTEXITCODE -ne 0) {
    throw "keytool failed while creating the keystore."
}

Write-Host ""
Write-Host "Keystore created successfully." -ForegroundColor Green
Write-Host ""
Write-Host "Certificate details / SHA-256 fingerprint:" -ForegroundColor Cyan
& $keytoolPath -list -v -keystore $keystoreFullPath -alias $Alias

if ($LASTEXITCODE -ne 0) {
    throw "keytool could not read the generated keystore."
}

if ($ConfigureGitHubSecrets) {
    $gh = Get-Command gh -ErrorAction SilentlyContinue
    if (-not $gh) {
        throw "GitHub CLI (gh) was not found. The keystore was created successfully, but secrets were not configured. Install/authenticate gh or follow docs/SIGNING.md for manual secret setup."
    }

    Write-Host ""
    Write-Host "Configuring GitHub Actions repository secrets for $Repository" -ForegroundColor Cyan
    Write-Host "Re-enter the keystore password. It will be sent to GitHub through gh stdin and will not be printed."

    $securePassword = Read-Host "Keystore password" -AsSecureString
    $plainPassword = ConvertFrom-SecureStringPlainText -SecureValue $securePassword

    try {
        $keystoreBase64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes($keystoreFullPath))

        $keystoreBase64 | & $gh.Source secret set CAREEROPS_RELEASE_KEYSTORE_B64 --repo $Repository
        if ($LASTEXITCODE -ne 0) { throw "Failed to set CAREEROPS_RELEASE_KEYSTORE_B64." }

        $plainPassword | & $gh.Source secret set CAREEROPS_RELEASE_STORE_PASSWORD --repo $Repository
        if ($LASTEXITCODE -ne 0) { throw "Failed to set CAREEROPS_RELEASE_STORE_PASSWORD." }

        $Alias | & $gh.Source secret set CAREEROPS_RELEASE_KEY_ALIAS --repo $Repository
        if ($LASTEXITCODE -ne 0) { throw "Failed to set CAREEROPS_RELEASE_KEY_ALIAS." }

        $plainPassword | & $gh.Source secret set CAREEROPS_RELEASE_KEY_PASSWORD --repo $Repository
        if ($LASTEXITCODE -ne 0) { throw "Failed to set CAREEROPS_RELEASE_KEY_PASSWORD." }
    }
    finally {
        $plainPassword = $null
        $keystoreBase64 = $null
        $securePassword = $null
    }

    Write-Host ""
    Write-Host "GitHub signing secrets configured." -ForegroundColor Green
}

Write-Host ""
Write-Host "NEXT STEPS" -ForegroundColor Cyan
Write-Host "1. Back up the keystore in at least one second secure location."
Write-Host "2. If secrets are not configured yet, follow docs/SIGNING.md."
Write-Host "3. After the signing infrastructure is on main, run Android Release Candidate from the release-candidate branch."
Write-Host "4. Compare the workflow signer SHA-256 fingerprint with this keystore."
Write-Host ""
Write-Host "Do not commit this keystore to Git." -ForegroundColor Yellow
