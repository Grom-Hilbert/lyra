# Multi-stage build for Lyra Document Management System
FROM gradle:8-jdk21-alpine AS builder

# Set working directory
WORKDIR /app

# Copy gradle files
COPY build.gradle settings.gradle ./
COPY gradle/ gradle/

# Copy source code
COPY src/ src/

# Build the application
RUN gradle build -x test --no-daemon

# Runtime stage
FROM openjdk:21-jdk-slim

# Install NFS client tools and other dependencies
RUN apt-get update && \
    apt-get install -y \
        nfs-common \
        curl \
        wget \
        ca-certificates \
        && rm -rf /var/lib/apt/lists/*

# Create application user
RUN groupadd -r lyra && useradd -r -g lyra lyra

# Create directories
RUN mkdir -p /app /data /mnt/nfs-storage /tmp/lyra && \
    chown -R lyra:lyra /app /data /mnt/nfs-storage /tmp/lyra

# Set working directory
WORKDIR /app

# Copy built application
COPY --from=builder /app/build/libs/*.jar app.jar

# Copy configuration files
COPY src/main/resources/application-default.yml application.yml

# Create startup script
RUN echo '#!/bin/bash\n\
set -e\n\
\n\
# Function to check if NFS is enabled\n\
is_nfs_enabled() {\n\
    [ "$STORAGE_TYPE" = "nfs" ] && [ "$NFS_ENABLED" = "true" ]\n\
}\n\
\n\
# Function to wait for NFS server\n\
wait_for_nfs() {\n\
    if is_nfs_enabled; then\n\
        echo "Waiting for NFS server $NFS_SERVER..."\n\
        timeout=60\n\
        while [ $timeout -gt 0 ]; do\n\
            if showmount -e "$NFS_SERVER" >/dev/null 2>&1; then\n\
                echo "NFS server is available"\n\
                return 0\n\
            fi\n\
            echo "NFS server not ready, waiting..."\n\
            sleep 2\n\
            timeout=$((timeout - 2))\n\
        done\n\
        echo "Warning: NFS server not available after 60 seconds"\n\
    fi\n\
}\n\
\n\
# Function to create mount point\n\
create_mount_point() {\n\
    if is_nfs_enabled; then\n\
        mkdir -p "$NFS_MOUNT_POINT"\n\
        chown lyra:lyra "$NFS_MOUNT_POINT"\n\
    fi\n\
}\n\
\n\
# Main execution\n\
echo "Starting Lyra Document Management System..."\n\
echo "Storage Type: ${STORAGE_TYPE:-local}"\n\
\n\
if is_nfs_enabled; then\n\
    echo "NFS Configuration:"\n\
    echo "  Server: $NFS_SERVER"\n\
    echo "  Export Path: $NFS_EXPORT_PATH"\n\
    echo "  Mount Point: $NFS_MOUNT_POINT"\n\
    echo "  Mount Options: $NFS_MOUNT_OPTIONS"\n\
    \n\
    wait_for_nfs\n\
    create_mount_point\n\
fi\n\
\n\
# Switch to application user and start the application\n\
exec su-exec lyra java $JAVA_OPTS -jar app.jar "$@"\n\
' > /app/start.sh && chmod +x /app/start.sh

# Install su-exec for user switching
RUN apt-get update && \
    apt-get install -y su-exec && \
    rm -rf /var/lib/apt/lists/*

# Set environment variables
ENV JAVA_OPTS="-Xmx1g -Xms512m" \
    STORAGE_TYPE=local \
    NFS_ENABLED=false \
    NFS_MOUNT_POINT=/mnt/nfs-storage \
    NFS_MOUNT_OPTIONS="rw,sync,hard,intr" \
    DB_TYPE=sqlite \
    DB_PATH=/data/lyra.db

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Use startup script
ENTRYPOINT ["/app/start.sh"]

# Default command
CMD []

# Labels
LABEL maintainer="Lyra Team" \
      version="1.0.0" \
      description="Lyra Document Management System with NFS support" \
      org.opencontainers.image.title="Lyra" \
      org.opencontainers.image.description="Enterprise-grade cloud-native document management system" \
      org.opencontainers.image.vendor="TSLC BeiHaiYun" \
      org.opencontainers.image.version="1.0.0"