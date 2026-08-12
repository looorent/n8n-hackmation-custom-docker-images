# syntax=docker/dockerfile:1

FROM rust:1-alpine AS build
RUN apk add --no-cache musl-dev
WORKDIR /src
COPY Cargo.toml ./
COPY src ./src
# The alpine host target (*-unknown-linux-musl) links statically by default,
# so the release binary runs on scratch with no libc.
RUN cargo build --release

FROM build AS test
RUN cargo test

FROM scratch
COPY --from=build /src/target/release/bf /bf
ENTRYPOINT ["/bf"]
