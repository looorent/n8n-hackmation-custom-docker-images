# syntax=docker/dockerfile:1

# Lisette compiles to Go, so the builder is the Go toolchain plus the `lis` binary.
FROM golang:1.25-alpine AS toolchain
ARG TARGETARCH
ARG LISETTE_VERSION=lisette-v0.11.2
RUN apk add --no-cache curl xz tar
RUN case "$TARGETARCH" in \
      arm64) lis_arch=aarch64 ;; \
      amd64) lis_arch=x86_64 ;; \
      *) echo "unsupported TARGETARCH: $TARGETARCH" >&2; exit 1 ;; \
    esac && \
    curl -fsSL "https://github.com/ivov/lisette/releases/download/${LISETTE_VERSION}/lisette-${lis_arch}-unknown-linux-musl.tar.xz" \
      | tar -xJ --strip-components=1 -C /usr/local/bin "lisette-${lis_arch}-unknown-linux-musl/lis" && \
    lis version

FROM toolchain AS build
WORKDIR /app
COPY lisette.toml ./
COPY src ./src
RUN lis check && lis emit
RUN cd target && CGO_ENABLED=0 go build -trimpath -ldflags="-s -w" -o /out/base64-to-image-cli .

# `docker build --target test .` runs the Lisette unit tests plus
# end-to-end assertions against the compiled binary.
FROM build AS test
RUN lis test
RUN <<'EOF'
set -eu
BIN=/out/base64-to-image-cli
# 1x1 PNG
PNG_B64="iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg=="

# stdin mode: decoded bytes must match a reference decode exactly
want=$(printf '%s' "$PNG_B64" | base64 -d | sha256sum | cut -d' ' -f1)
got=$(printf '%s\n' "$PNG_B64" | "$BIN" | sha256sum | cut -d' ' -f1)
[ "$got" = "$want" ] || { echo "FAIL: stdin decode mismatch"; exit 1; }

# format detection is reported on stderr
err=$(printf '%s' "$PNG_B64" | "$BIN" 2>&1 >/dev/null)
[ "$err" = "detected: PNG" ] || { echo "FAIL: expected 'detected: PNG', got '$err'"; exit 1; }

# argv mode must equal stdin mode
got_argv=$("$BIN" "$PNG_B64" | sha256sum | cut -d' ' -f1)
[ "$got_argv" = "$want" ] || { echo "FAIL: argv decode mismatch"; exit 1; }

# invalid Base64: exit 1, empty stdout
if out=$(printf 'not base64!!!\n' | "$BIN" 2>/dev/null); then
  echo "FAIL: invalid Base64 should exit non-zero"; exit 1
fi
[ -z "$out" ] || { echo "FAIL: invalid Base64 should produce no stdout"; exit 1; }

# valid Base64 of non-image bytes: exit 1
if printf 'aGVsbG8=' | "$BIN" >/dev/null 2>&1; then
  echo "FAIL: non-image bytes should exit non-zero"; exit 1
fi

echo "all end-to-end assertions passed"
EOF

FROM scratch
COPY --from=build /out/base64-to-image-cli /base64-to-image-cli
ENTRYPOINT ["/base64-to-image-cli"]
