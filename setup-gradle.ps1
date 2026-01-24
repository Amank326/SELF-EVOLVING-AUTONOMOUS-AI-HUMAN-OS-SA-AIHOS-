# Download and extract gradle wrapper
Add-Type -AssemblyName System.IO.Compression.FileSystem

$jarUrl = 'https://services.gradle.org/distributions/gradle-8.5-bin.zip'
$tempZip = 'gradle85-temp.zip'

Write-Host 'Downloading Gradle 8.5...'
(New-Object System.Net.WebClient).DownloadFile($jarUrl, $tempZip)
Write-Host 'Download complete!'

Write-Host 'Extracting gradle-wrapper-8.5.jar...'
$zip = [System.IO.Compression.ZipFile]::OpenRead($tempZip)
$wrapperEntry = $zip.Entries | Where-Object { $_.Name -eq 'gradle-wrapper-8.5.jar' } | Select-Object -First 1

if ($wrapperEntry) {
    [System.IO.Compression.ZipFileExtensions]::ExtractToFile($wrapperEntry, 'gradle/wrapper/gradle-wrapper.jar', $true)
    Write-Host 'gradle-wrapper.jar extracted successfully'
    $size = (Get-Item 'gradle/wrapper/gradle-wrapper.jar').Length
    Write-Host "File size: $size bytes"
} else {
    Write-Host 'ERROR: gradle-wrapper-8.5.jar not found in archive'
}

$zip.Dispose()
Remove-Item $tempZip -Force -ErrorAction SilentlyContinue
Write-Host 'Setup complete!'
