# hackmation-go-ascii-art-cli

A small Go CLI that converts an image (PNG, JPEG, or GIF) to ASCII art.
It reads the image from stdin by default, maps each sampled pixel to a
character by luminance (0.2126 R + 0.7152 G + 0.0722 B), and prints the
result to stdout, one line per row. Errors go to stderr with a non-zero
exit code. The tool uses only the Go standard library and ships as a
minimal `FROM scratch` Docker image.

## Usage

Pipe an image via stdin (the default and the most Docker-friendly mode):

```sh
docker run --rm -i looorent/hackmation-go-ascii-art-cli < photo.png
```

Read from a file path passed as the first positional argument. Inside
Docker the file must be visible to the container, so mount it as a volume:

```sh
docker run --rm -v "$PWD:/data" looorent/hackmation-go-ascii-art-cli /data/photo.png
```

Flags:

- `-w` / `--width`: output width in characters (default 80). The height is
  derived from the image aspect ratio with a 0.5 correction factor, because
  terminal character cells are roughly twice as tall as they are wide.
- `-c` / `--charset`: custom character ramp ordered lightest to densest
  (default `" .:-=+*#%@"`).

```sh
docker run --rm -i looorent/hackmation-go-ascii-art-cli -w 120 -c " .oO@" < photo.jpg
```

## Prerequisites

Docker only. No Go toolchain is needed on the host: compiling, testing,
and packaging all happen inside Docker.

## Build the Docker image

```sh
docker build -t looorent/hackmation-go-ascii-art-cli .
```

## Publish the image

The image name already carries the Docker Hub namespace, so pushing is:

```sh
docker login   # once, as looorent
docker push looorent/hackmation-go-ascii-art-cli:latest
```

For another registry, retag first, e.g.
`docker tag looorent/hackmation-go-ascii-art-cli ghcr.io/<your-org>/hackmation-go-ascii-art-cli:latest`.

## Why the image is small

- The final stage is `FROM scratch` and contains a single file: the binary.
- `CGO_ENABLED=0` produces a fully static binary with no libc dependency.
- `-ldflags="-s -w"` strips the symbol table and DWARF debug info.
- `-trimpath` removes local build paths for reproducible output.

The result is a few megabytes, essentially the size of the Go binary.

## Run the tests

Unit tests run inside Docker via the dedicated `test` stage:

```sh
docker build --target test .
```

The build fails if any test fails. Add `--no-cache` to force a re-run
when nothing changed.
