# hackmation-java-qr-code-cli

A tiny command-line tool that takes a string as argument and prints a QR code
of it, as a Base64-encoded PNG, to stdout.

Built with:

- **Java 21** / **Quarkus 3.38** (Picocli extension)
- **ZXing** for QR code generation (plus a hand-rolled PNG encoder, so the
  binary stays free of `java.awt`)
- **GraalVM native image** (Mandrel JDK 25), compiled *inside a container* —
  you do **not** need GraalVM installed
- **Docker** runtime image built `FROM scratch`: just the UPX-compressed
  native binary and the three shared libraries it needs (`ld-linux`, `libc`,
  `libz`) — no JVM, no shell, no package manager, no distro

## Usage

```bash
docker run --rm looorent/hackmation-java-qr-code-cli "https://n8n.io"
# -> iVBORw0KGgoAAAANSUhEUgAAAQAAAA... (Base64 PNG on stdout)

# Decode it into an image:
docker run --rm looorent/hackmation-java-qr-code-cli "https://n8n.io" | base64 -d > qr.png

# Options:
docker run --rm looorent/hackmation-java-qr-code-cli --size 512 "bigger please"
docker run --rm looorent/hackmation-java-qr-code-cli --help
```

## Prerequisites

- A JDK 21+ (to run Gradle — **GraalVM is not required**)
- Docker (or a compatible container runtime)

That's it. The native compilation runs inside the
`quay.io/quarkus/ubi9-quarkus-mandrel-builder-image` container, pulled
automatically by the build.

No Gradle installation is needed either — the Gradle wrapper (`./gradlew`) is
committed. If you don't even have a JDK, you can run Gradle itself in Docker:

```bash
docker run --rm -v /var/run/docker.sock:/var/run/docker.sock \
  -v "$PWD":"$PWD" -w "$PWD" gradle:9-jdk21 \
  bash -c 'curl -fsSL "https://download.docker.com/linux/static/stable/$(uname -m)/docker-27.5.1.tgz" \
    | tar xz -C /usr/local/bin --strip-components=1 docker/docker \
    && gradle build -Dquarkus.native.enabled=true -Dquarkus.package.jar.enabled=false -Dquarkus.container-image.build=true'
```

## Build the Docker image

```bash
./gradlew build \
  -Dquarkus.native.enabled=true \
  -Dquarkus.package.jar.enabled=false \
  -Dquarkus.container-image.build=true
```

What happens:

1. Quarkus compiles the app to a **native executable** inside the
   `quay.io/quarkus/ubi9-quarkus-mandrel-builder-image` container
   (`quarkus.native.container-build=true` is set in `application.properties`,
   so no local GraalVM is used — only Docker).
2. The `quarkus-container-image-docker` extension builds the runtime image
   from [`src/main/docker/Dockerfile.native`](src/main/docker/Dockerfile.native):
   `FROM scratch`, plus the binary and the three libraries it links against,
   copied from the builder image.

Result:

```bash
docker images looorent/hackmation-java-qr-code-cli
# IMAGE                                         DISK USAGE   CONTENT SIZE
# looorent/hackmation-java-qr-code-cli:latest   ~28MB        ~13MB
```

## Publish the image

Publishing is also driven by Gradle, via the same Quarkus container-image
extension. The image group is set to `looorent` in `application.properties`,
so the default target is Docker Hub:

```bash
docker login   # once, as looorent

./gradlew build \
  -Dquarkus.native.enabled=true \
  -Dquarkus.package.jar.enabled=false \
  -Dquarkus.container-image.build=true \
  -Dquarkus.container-image.push=true
```

This pushes `looorent/hackmation-java-qr-code-cli:latest`. To publish to
another registry or namespace, override with
`-Dquarkus.container-image.registry=...` / `-Dquarkus.container-image.group=...`
(credentials via `-Dquarkus.container-image.username/password` if you don't
want to `docker login`).

## Development (JVM mode, no Docker)

```bash
./gradlew test                                  # unit tests (QR round-trip)
./gradlew build                                 # fast-jar
java -jar build/quarkus-app/quarkus-run.jar "hello"
```

## How the image stays small

- No JVM in the image: ahead-of-time compiled native executable.
- The executable is UPX-compressed by the build
  (`quarkus.native.compression.level=5`, ~37 MB → ~11.5 MB). Costs
  ~100-300 ms at startup and the unpacked code is not shared between
  containers via the page cache — both irrelevant for a one-shot CLI.
- Base image is `scratch`: `ldd` shows the binary needs exactly three
  files (`ld-linux`, `libc.so.6`, `libz.so.1`, ~3 MB total), so the
  Dockerfile copies only those from the builder image instead of shipping
  a distro rootfs.
- No `java.awt`/`javax.imageio`: the PNG is written by a ~80-line encoder
  (`PngEncoder`), keeping the native image closed-world friendly and lean.
  (ZXing's AWT-based `javase` module is used in tests only.)

### Alternative: fully static (amd64 only)

GraalVM can also link **fully statically** against musl, collapsing the
image to a single file. This requires an x86_64 builder with a musl
toolchain — GraalVM does not support `--libc=musl` on aarch64, and the
stock Mandrel builder image does not ship musl. On an amd64 CI runner with
a musl-enabled builder image, add:

```properties
quarkus.native.additional-build-args=--static,--libc=musl
```

and drop the library copies from `Dockerfile.native`.
