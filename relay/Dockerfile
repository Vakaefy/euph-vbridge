FROM golang:1.23-alpine AS build
WORKDIR /src
COPY go.mod main.go ./
RUN go mod tidy && CGO_ENABLED=0 go build -trimpath -ldflags "-s -w" -o /out/voice-relay .

FROM alpine:3.20
COPY --from=build /out/voice-relay /usr/local/bin/voice-relay
EXPOSE 8790
ENTRYPOINT ["/usr/local/bin/voice-relay"]