# Extract gradle-wrapper-8.5.jar and rename to gradle-wrapper.jar
Add-Type -AssemblyName System.IO.Compression.FileSystem

Write-Host 'Opening gradle85.zip...'
$zip = [System.IO.Compression.ZipFile]::OpenRead('gradle85.zip')

Write-Host 'Extracting gradle-wrapper-8.5.jar...'
$wrapperEntry = $zip.Entries | Where-Object { $_.Name -eq 'gradle-wrapper-8.5.jar' }
if ($wrapperEntry) {
    [System.IO.Compression.ZipFileExtensions]::ExtractToFile($wrapperEntry, 'gradle/wrapper/gradle-wrapper.jar', $true)
    Write-Host 'Successfully extracted gradle-wrapper.jar'
    $size = (Get-Item 'gradle/wrapper/gradle-wrapper.jar').Length
    Write-Host "File size: $size bytes"
} else {
    Write-Host 'ERROR: gradle-wrapper-8.5.jar not found'
}

$zip.Dispose()
Remove-Item 'gradle85.zip' -Force
Write-Host 'Extraction complete!'
