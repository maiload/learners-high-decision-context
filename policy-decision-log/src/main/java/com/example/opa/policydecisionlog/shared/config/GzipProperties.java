package com.example.opa.policydecisionlog.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "opa.gzip")
public record GzipProperties(
        String path,
        long maxCompressedSize,
        long maxDecompressedSize
) {
    public static final String DEFAULT_PATH = "/logs";
    public static final long DEFAULT_MAX_COMPRESSED = 64L * 1024;
    public static final long DEFAULT_MAX_DECOMPRESSED = 1024L * 1024;

    public GzipProperties {
        if (!StringUtils.hasText(path)) {
            path = DEFAULT_PATH;
        }
        if (maxCompressedSize <= 0) {
            maxCompressedSize = DEFAULT_MAX_COMPRESSED;
        }
        if (maxDecompressedSize <= 0) {
            maxDecompressedSize = DEFAULT_MAX_DECOMPRESSED;
        }
    }
}
