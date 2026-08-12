# hackmation-rust-brainfuck-cli

A Brainfuck interpreter written in Rust (zero dependencies, pure std), packaged
as a minimal Docker image built entirely inside Docker. No Rust toolchain is
needed on the host.

Semantics:

- 30000-cell tape of wrapping `u8` cells.
- Moving the pointer outside the tape is an error (no undefined behavior).
- `,` reads one byte from stdin; on EOF it sets the current cell to 0.
- `.` writes one byte to stdout. Stdout only ever contains program output.
- Unbalanced brackets are rejected before execution: message on stderr,
  non-zero exit code.

## Usage

The first positional argument is the Brainfuck source:

```bash
docker run --rm looorent/hackmation-rust-brainfuck-cli \
  '++++++++[>++++[>++>+++>+++>+<<<<-]>+>+>->>+[<]<-]>>.>---.+++++++..+++.>>.<-.<.+++.------.--------.>>+.>++.'
# Hello World!
```

Program input for `,` comes from stdin (note `-i`):

```bash
echo -n 'hi' | docker run --rm -i looorent/hackmation-rust-brainfuck-cli ',[.,]'
# hi
```

Reading the program from a file instead of argv:

```bash
docker run --rm -v "$PWD:/w" looorent/hackmation-rust-brainfuck-cli -f /w/program.bf
```

## Prerequisites

Docker only. Compiling, testing, and packaging all happen inside the build.
No rustup, no cargo, nothing installed on the host.

## Building the image

```bash
docker build -t looorent/hackmation-rust-brainfuck-cli .
```

## Publishing

The image name already carries the Docker Hub namespace, so pushing is:

```bash
docker login   # once, as looorent
docker push looorent/hackmation-rust-brainfuck-cli:latest
```

For another registry, retag first, e.g.
`docker tag looorent/hackmation-rust-brainfuck-cli ghcr.io/<owner>/hackmation-rust-brainfuck-cli:latest`.

## How the image stays small

- The final stage is `FROM scratch`: it contains a single file, the binary.
- The build stage runs on `rust:1-alpine`, where the host target is
  `*-unknown-linux-musl`, so the binary is statically linked and needs no libc
  at runtime. The target is never hardcoded, so the build works on amd64 and
  arm64 alike.
- The release profile in `Cargo.toml` minimizes binary size:
  `opt-level = "z"`, `lto = true`, `codegen-units = 1`, `strip = true`,
  `panic = "abort"`.

Result: an image well under 1 MB.

## Running the tests

Unit tests live in `src/interpreter.rs` and run inside Docker via the `test`
build stage:

```bash
docker build --target test --progress=plain .
```
