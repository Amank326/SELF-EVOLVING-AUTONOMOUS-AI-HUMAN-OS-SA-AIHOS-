# Download and extract gradle wrapper
$jarUrl = 'https://services.gradle.org/distributions/gradle-8.5-bin.zip'
$tempZip = 'temp-gradle.zip'

Write-Host 'Downloading Gradle 8.5 binary distribution...'
(New-Object System.Net.WebClient).DownloadFile($jarUrl, $tempZip)

Write-Host 'Extracting gradle-wrapper.jar from distribution...'
Add-Type -AssemblyName System.IO.Compression.FileSystem

$zip = [System.IO.Compression.ZipFile]::OpenRead($tempZip)
$wrapperEntry = $zip.Entries | Where-Object { $_.Name -eq 'gradle-wrapper.jar' } | Select-Object -First 1

if ($wrapperEntry) {
    [System.IO.Compression.ZipFileExtensions]::ExtractToFile($wrapperEntry, 'gradle/wrapper/gradle-wrapper.jar', $true)
    Write-Host 'gradle-wrapper.jar extracted successfully'
} else {
    Write-Host 'ERROR: Could not find gradle-wrapper.jar in distribution'
}

$zip.Dispose()
Remove-Item $tempZip -Force -ErrorAction SilentlyContinue

Write-Host 'Gradle wrapper restoration complete'
