package main

import (
	"image"
)

const defaultRamp = " .:-=+*#%@"

// heightFor derives the output row count from the source dimensions and the
// requested column count. Terminal cells are roughly twice as tall as wide,
// so the aspect ratio is corrected by 0.5 to keep proportions on screen.
func heightFor(srcW, srcH, width int) int {
	h := int(float64(srcH) / float64(srcW) * float64(width) * 0.5)
	if h < 1 {
		h = 1
	}
	return h
}

// luminance returns perceived brightness in [0, 255] using Rec. 709 weights.
func luminance(r, g, b uint32) float64 {
	// RGBA() returns 16-bit channels; scale to 8-bit.
	return (0.2126*float64(r>>8) + 0.7152*float64(g>>8) + 0.0722*float64(b>>8))
}

// render converts img to ASCII art lines, width characters wide. The ramp is
// ordered lightest to densest: bright pixels map to its first rune, dark
// pixels to its last.
func render(img image.Image, width int, ramp string) []string {
	runes := []rune(ramp)
	bounds := img.Bounds()
	srcW, srcH := bounds.Dx(), bounds.Dy()
	height := heightFor(srcW, srcH, width)

	lines := make([]string, height)
	row := make([]rune, width)
	for y := range height {
		srcY := bounds.Min.Y + y*srcH/height
		for x := range width {
			srcX := bounds.Min.X + x*srcW/width
			r, g, b, _ := img.At(srcX, srcY).RGBA()
			lum := luminance(r, g, b)
			idx := int((255 - lum) / 255 * float64(len(runes)-1))
			row[x] = runes[idx]
		}
		lines[y] = string(row)
	}
	return lines
}
