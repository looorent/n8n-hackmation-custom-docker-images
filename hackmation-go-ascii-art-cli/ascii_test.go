package main

import (
	"bytes"
	"image"
	"image/color"
	"image/png"
	"strings"
	"testing"
)

// gradientPNG encodes a horizontal black-to-white gradient as PNG bytes.
func gradientPNG(t *testing.T, w, h int) []byte {
	t.Helper()
	img := image.NewGray(image.Rect(0, 0, w, h))
	for y := range h {
		for x := range w {
			img.SetGray(x, y, color.Gray{Y: uint8(x * 255 / (w - 1))})
		}
	}
	var buf bytes.Buffer
	if err := png.Encode(&buf, img); err != nil {
		t.Fatal(err)
	}
	return buf.Bytes()
}

func TestGradientMapsDarkToDenseAndLightToSpace(t *testing.T) {
	var out bytes.Buffer
	err := run([]string{"-w", "10"}, bytes.NewReader(gradientPNG(t, 100, 20)), &out)
	if err != nil {
		t.Fatal(err)
	}
	lines := strings.Split(strings.TrimRight(out.String(), "\n"), "\n")
	if len(lines) != 1 {
		t.Fatalf("expected 1 line for 100x20 at width 10, got %d", len(lines))
	}
	line := []rune(lines[0])
	if len(line) != 10 {
		t.Fatalf("expected width 10, got %d", len(line))
	}
	if line[0] != '@' {
		t.Errorf("darkest pixel should map to densest char '@', got %q", line[0])
	}
	if line[len(line)-1] != ' ' {
		t.Errorf("lightest pixel should map to space, got %q", line[len(line)-1])
	}
}

func TestCustomCharset(t *testing.T) {
	var out bytes.Buffer
	err := run([]string{"-w", "4", "-c", ".#"}, bytes.NewReader(gradientPNG(t, 40, 8)), &out)
	if err != nil {
		t.Fatal(err)
	}
	for _, r := range strings.TrimRight(out.String(), "\n") {
		if r != '.' && r != '#' {
			t.Fatalf("output contains rune %q outside custom charset", r)
		}
	}
}

func TestHeightFor(t *testing.T) {
	tests := []struct {
		srcW, srcH, width, want int
	}{
		{100, 100, 80, 40}, // square image: half the width
		{200, 100, 80, 20},
		{100, 200, 80, 80},
		{1000, 1, 80, 1}, // never below one row
	}
	for _, tt := range tests {
		if got := heightFor(tt.srcW, tt.srcH, tt.width); got != tt.want {
			t.Errorf("heightFor(%d, %d, %d) = %d, want %d", tt.srcW, tt.srcH, tt.width, got, tt.want)
		}
	}
}

func TestUnsupportedInputFails(t *testing.T) {
	var out bytes.Buffer
	err := run(nil, strings.NewReader("this is not an image"), &out)
	if err == nil {
		t.Fatal("expected an error for non-image input")
	}
}

func TestInvalidWidthFails(t *testing.T) {
	var out bytes.Buffer
	if err := run([]string{"-w", "0"}, bytes.NewReader(gradientPNG(t, 10, 10)), &out); err == nil {
		t.Fatal("expected an error for width 0")
	}
}
