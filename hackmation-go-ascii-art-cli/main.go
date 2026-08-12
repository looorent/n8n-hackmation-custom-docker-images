package main

import (
	"bufio"
	"errors"
	"flag"
	"fmt"
	"image"
	_ "image/gif"
	_ "image/jpeg"
	_ "image/png"
	"io"
	"os"
)

func main() {
	if err := run(os.Args[1:], os.Stdin, os.Stdout); err != nil {
		fmt.Fprintln(os.Stderr, "error:", err)
		os.Exit(1)
	}
}

func run(args []string, stdin io.Reader, stdout io.Writer) error {
	fs := flag.NewFlagSet("ascii-art-cli", flag.ContinueOnError)
	width := fs.Int("width", 80, "output width in characters")
	fs.IntVar(width, "w", 80, "shorthand for -width")
	ramp := fs.String("charset", defaultRamp, "character ramp, lightest to densest")
	fs.StringVar(ramp, "c", defaultRamp, "shorthand for -charset")
	if err := fs.Parse(args); err != nil {
		return err
	}
	if *width < 1 {
		return errors.New("width must be at least 1")
	}
	if len(*ramp) == 0 {
		return errors.New("charset must not be empty")
	}

	input := stdin
	if fs.NArg() > 0 {
		f, err := os.Open(fs.Arg(0))
		if err != nil {
			return err
		}
		defer f.Close()
		input = f
	}

	img, _, err := image.Decode(input)
	if err != nil {
		return fmt.Errorf("decode image: %w", err)
	}

	out := bufio.NewWriter(stdout)
	for _, line := range render(img, *width, *ramp) {
		fmt.Fprintln(out, line)
	}
	return out.Flush()
}
