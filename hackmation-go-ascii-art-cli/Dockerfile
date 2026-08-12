# syntax=docker/dockerfile:1

FROM golang:1-alpine AS build
WORKDIR /src
COPY go.mod ./
COPY *.go ./
RUN CGO_ENABLED=0 go build -trimpath -ldflags="-s -w" -o /out/ascii-art-cli .

# Test stage: `docker build --target test .` runs the unit tests.
FROM build AS test
RUN go vet ./... && go test -v ./...

FROM scratch
COPY --from=build /out/ascii-art-cli /ascii-art-cli
ENTRYPOINT ["/ascii-art-cli"]
