# hackmation-lisette-base64-to-image-cli

A tiny command-line tool that converts a Base64 string into an image: it
decodes the Base64, validates by magic bytes that the payload really is an
image (PNG, JPEG, GIF, WebP or BMP), and writes the raw image bytes to stdout.
A short `detected: <FORMAT>` note goes to stderr, so stdout stays a pure image
stream you can redirect or pipe.

Built with **[Lisette](https://lisette.run)**, a Rust-syntax language that
compiles to readable Go. The `lis` toolchain emits a plain Go module, which is
then compiled by the standard Go toolchain into a single static binary.

It is the inverse companion of the sibling
`looorent/hackmation-java-qr-code-cli`, which prints a QR code as Base64 PNG.

## Usage

Base64 is read from stdin by default:

```bash
echo "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==" \
  | docker run --rm -i looorent/hackmation-lisette-base64-to-image-cli > pixel.png
# stderr: detected: PNG
```

Or passed as the first argument:

```bash
docker run --rm looorent/hackmation-lisette-base64-to-image-cli "iVBORw0KGgo...SUVORK5CYII=" > pixel.png
```

Full pipeline with the QR code generator:

```bash
docker run --rm looorent/hackmation-java-qr-code-cli "https://n8n.io" \
  | docker run --rm -i looorent/hackmation-lisette-base64-to-image-cli > qr.png
# stderr: detected: PNG
file qr.png
# qr.png: PNG image data, 256 x 256, 8-bit grayscale, non-interlaced
```

Surrounding whitespace and newlines in the input are ignored. On invalid
Base64, or when the decoded bytes are not a recognized image format, the tool
prints an error to stderr, writes nothing to stdout, and exits with code 1.

## Prerequisites

- Docker (or a compatible container runtime)

That's it. No Lisette, no Go, no Rust on your machine: the Lisette compiler
and the Go toolchain are downloaded inside the builder stage of the
Dockerfile.

## Build the Docker image

```bash
docker build -t looorent/hackmation-lisette-base64-to-image-cli .
```

The builder stage installs the `lis` binary from the Lisette GitHub releases
(pinned version, arch-aware: arm64 and amd64), runs `lis check`, emits Go code
with `lis emit`, and compiles it with the Go toolchain. The final stage is
`FROM scratch` with just the binary.

## Publish the image

```bash
docker login
docker push looorent/hackmation-lisette-base64-to-image-cli:latest
```

Prefer also tagging a version so consumers can pin:

```bash
docker tag looorent/hackmation-lisette-base64-to-image-cli \
  looorent/hackmation-lisette-base64-to-image-cli:0.1.0
docker push looorent/hackmation-lisette-base64-to-image-cli:0.1.0
```

## How the image stays small

Around 2 MB on disk (661 kB compressed content):

- `FROM scratch` runtime stage: no OS, no shell, no libc, just the binary.
- The Lisette to Go to binary chain produces a single static executable,
  built with `CGO_ENABLED=0` so nothing is dynamically linked.
- `go build -trimpath -ldflags="-s -w"` strips symbol tables and debug info.
- Only the Go standard library is used (`encoding/base64`, `bytes`, `io`,
  `os`, `strings`), so no third-party dependencies are compiled in.

## Run the tests

```bash
docker build --target test .
```

The `test` stage runs:

- the Lisette unit tests (`lis test`, see `src/img/img.test.lis`): Base64
  decoding, whitespace tolerance, and magic-byte detection for every
  supported format, plus rejection cases;
- end-to-end shell assertions against the compiled binary: a known 1x1 PNG
  round-trips byte-for-byte (sha256 compared against a reference decode),
  `detected: PNG` appears on stderr, argv mode matches stdin mode, and the
  two failure modes (invalid Base64, non-image bytes) exit non-zero with an
  empty stdout.
