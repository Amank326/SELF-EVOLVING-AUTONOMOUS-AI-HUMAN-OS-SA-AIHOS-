# Extract gradle-wrapper.jar from gradle 8.5 distribution
Add-Type -AssemblyName System.IO.Compression.FileSystem

Write-Host 'Opening gradle85.zip...'
$zip = [System.IO.Compression.ZipFile]::OpenRead('gradle85.zip')

Write-Host 'Finding gradle-wrapper.jar...'
$wrapperEntry = $zip.Entries | Where-Object { $_.Name -eq 'gradle-wrapper.jar' } | Select-Object -First 1

if ($wrapperEntry) {
    Write-Host "Found gradle-wrapper.jar, extracting..."
    [System.IO.Compression.ZipFileExtensions]::ExtractToFile($wrapperEntry, 'gradle/wrapper/gradle-wrapper.jar', $true)
    Write-Host 'gradle-wrapper.jar extracted successfully'
    Get-Item 'gradle/wrapper/gradle-wrapper.jar' | Select-Object Length
} else {
    Write-Host 'ERROR: gradle-wrapper.jar not found in archive'
    $zip.Entries | Where-Object { $_.Name.Contains('gradle-wrapper') } | Select-Object Name
}

$zip.Dispose()
Write-Host 'Cleanup: removing gradle85.zip'
Remove-Item 'gradle85.zip' -Force
Write-Host 'Done!'
