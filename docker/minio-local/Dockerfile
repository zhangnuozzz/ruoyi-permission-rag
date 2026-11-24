# Build context is the workspace .local-runtime/minio-build directory.
# minio is compiled from the official fixed source release, CGO_ENABLED=0.
FROM scratch
COPY minio /minio
COPY ca-certificates.crt /etc/ssl/certs/ca-certificates.crt
EXPOSE 9000 9001
ENTRYPOINT ["/minio"]
