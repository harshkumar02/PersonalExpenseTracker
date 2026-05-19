Add-Type -AssemblyName System.Drawing

$densities = @{
    "mdpi" = 48
    "hdpi" = 72
    "xhdpi" = 96
    "xxhdpi" = 144
    "xxxhdpi" = 192
}

$greenColor = [System.Drawing.Color]::FromArgb(255, 76, 175, 80)
$redColor = [System.Drawing.Color]::FromArgb(255, 244, 67, 54)
$whiteColor = [System.Drawing.Color]::White

foreach ($density in $densities.Keys) {
    $size = $densities[$density]
    $basePath = "F:\repos\PersonalExpenseTracker\app\src\main\res\mipmap-$density"

    foreach ($iconName in @("ic_launcher", "ic_launcher_round")) {
        $bitmap = New-Object System.Drawing.Bitmap($size, $size)
        $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
        $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias

        $center = $size / 2
        $outerRadius = [int]($size * 0.4)
        $innerRadius = [int]($size * 0.22)

        $outerRect = New-Object System.Drawing.Rectangle([int]($center - $outerRadius), [int]($center - $outerRadius), ($outerRadius * 2), ($outerRadius * 2))
        $innerRect = New-Object System.Drawing.Rectangle([int]($center - $innerRadius), [int]($center - $innerRadius), ($innerRadius * 2), ($innerRadius * 2))

        # Draw green background circle
        $greenBrush = New-Object System.Drawing.SolidBrush($greenColor)
        $graphics.FillEllipse($greenBrush, $outerRect)

        # Draw red slice (30%) starting from 2 o'clock position (60 degrees)
        # 2 o'clock is 60 degrees from 12 o'clock (going clockwise)
        # So the slice goes from 60 to 60+108 = 168 degrees
        $redBrush = New-Object System.Drawing.SolidBrush($redColor)
        $graphics.FillPie($redBrush, $outerRect, 60, 108)

        # Draw white inner circle
        $whiteBrush = New-Object System.Drawing.SolidBrush($whiteColor)
        $graphics.FillEllipse($whiteBrush, $innerRect)

        # Draw R letter
        $rFont = New-Object System.Drawing.Font("Arial", [int]($size * 0.18), [System.Drawing.FontStyle]::Bold)
        $darkGreenBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 46, 125, 50))
        $stringFormat = New-Object System.Drawing.StringFormat
        $stringFormat.Alignment = [System.Drawing.StringAlignment]::Center
        $stringFormat.LineAlignment = [System.Drawing.StringAlignment]::Center
        $rRect = New-Object System.Drawing.RectangleF(0, 0, $size, $size)
        $graphics.DrawString("R", $rFont, $darkGreenBrush, $rRect, $stringFormat)

        $bitmap.Save("$basePath\$iconName.png", [System.Drawing.Imaging.ImageFormat]::Png)

        $graphics.Dispose()
        $bitmap.Dispose()
        $greenBrush.Dispose()
        $redBrush.Dispose()
        $whiteBrush.Dispose()
        $darkGreenBrush.Dispose()
        $rFont.Dispose()
    }
}

Write-Host "Icons generated successfully!"