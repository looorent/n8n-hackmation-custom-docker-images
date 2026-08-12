# hackmation: tiny CLI images

Four small command-line tools, each written in a different language and
packaged as the smallest practical Docker image. Every project builds,
tests, and packages entirely inside Docker: the only prerequisite on your
machine is Docker itself (the Java project optionally uses a local JDK to
drive Gradle).

| Image | Size (compressed) | Technology | Purpose |
|---|---|---|---|
| [looorent/hackmation-java-qr-code-cli](https://hub.docker.com/r/looorent/hackmation-java-qr-code-cli) | ~13 MB | Java 21, Quarkus, GraalVM native image, UPX, `FROM scratch` | Encode a string as a QR code, printed as a Base64 PNG |
| [looorent/hackmation-lisette-base64-to-image-cli](https://hub.docker.com/r/looorent/hackmation-lisette-base64-to-image-cli) | ~0.7 MB | [Lisette](https://lisette.run) (compiles to Go), static binary, `FROM scratch` | Decode a Base64 string into an image file |
| [looorent/hackmation-rust-brainfuck-cli](https://hub.docker.com/r/looorent/hackmation-rust-brainfuck-cli) | ~0.2 MB | Rust (edition 2024), static musl binary, `FROM scratch` | Interpret a Brainfuck program |
| [looorent/hackmation-go-ascii-art-cli](https://hub.docker.com/r/looorent/hackmation-go-ascii-art-cli) | ~0.8 MB | Go (stdlib only), static binary, `FROM scratch` | Convert an image to ASCII art |

Each project folder has its own README with usage, build, publish, and test
instructions.

## The tools compose

```bash
# String -> QR code -> PNG file, in one pipeline:
docker run --rm looorent/hackmation-java-qr-code-cli "https://n8n.io" \
  | docker run --rm -i looorent/hackmation-lisette-base64-to-image-cli > qr.png

# ...and back to the terminal as ASCII art:
docker run --rm -i looorent/hackmation-go-ascii-art-cli -w 60 < qr.png
```

## Building

Every image builds with a single `docker build` (Rust, Go, Lisette) or a
Gradle command that itself runs the native compilation in a container
(Java — no GraalVM needed). Tests run inside Docker too, via a dedicated
`test` build stage:

```bash
docker build --target test hackmation-rust-brainfuck-cli   # example
```

See each project's README for details.

## CI

[`.github/workflows/ci.yml`](.github/workflows/ci.yml) builds and tests all
four images on every push and pull request. On pushes to `main` it also
publishes them to Docker Hub. That publish step needs two repository
secrets:

- `DOCKERHUB_USERNAME` — the Docker Hub account (e.g. `looorent`)
- `DOCKERHUB_TOKEN` — a Docker Hub access token with write scope

The Rust, Go, and Lisette images are published multi-arch
(linux/amd64 + linux/arm64). The Java image is published for the
architecture of the CI runner (amd64), because GraalVM native compilation
under QEMU emulation is impractically slow.

## License

[MIT](LICENSE)
