package com.example.opa.policydecisionlog.filter;

import com.example.opa.policydecisionlog.config.GzipProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.zip.GZIPInputStream;

@Component
@RequiredArgsConstructor
public class GzipDecompressionFilter extends OncePerRequestFilter {

    private final GzipProperties gzipProperties;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String uri = request.getRequestURI();
        String path = gzipProperties.path();
        return !(uri.equals(path) || uri.startsWith(path + "/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String contentEncoding = request.getHeader(HttpHeaders.CONTENT_ENCODING);

        if (contentEncoding != null && contentEncoding.contains("gzip")) {
            long compressedSize = request.getContentLengthLong();
            if (compressedSize > gzipProperties.maxCompressedSize()) {
                response.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE,
                        "Compressed size exceeds limit: " + gzipProperties.maxCompressedSize() + " bytes");
                return;
            }
            filterChain.doFilter(new GzipRequestWrapper(request, gzipProperties.maxDecompressedSize()), response);
        } else {
            filterChain.doFilter(request, response);
        }
    }

    private static class GzipRequestWrapper extends HttpServletRequestWrapper {
        private static final int DEFAULT_BUFFER_SIZE = 8192;
        private final byte[] decompressedData;

        public GzipRequestWrapper(HttpServletRequest request, long maxDecompressedSize) throws IOException {
            super(request);
            this.decompressedData = decompress(request.getInputStream(), maxDecompressedSize);
        }

        private byte[] decompress(ServletInputStream inputStream, long maxDecompressedSize) throws IOException {
            try (GZIPInputStream gzipInputStream = new GZIPInputStream(inputStream);
                 ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

                byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
                int bytesRead;
                long totalRead = 0;

                while ((bytesRead = gzipInputStream.read(buffer)) != -1) {
                    totalRead += bytesRead;
                    if (totalRead > maxDecompressedSize) {
                        throw new IOException("Decompressed size exceeds limit: " + maxDecompressedSize + " bytes");
                    }
                    outputStream.write(buffer, 0, bytesRead);
                }
                return outputStream.toByteArray();
            }
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(decompressedData);
            return new ServletInputStream() {

                private boolean finished = false;

                @Override
                public int read() {
                    int data = byteArrayInputStream.read();
                    if (data == -1) {
                        this.finished = true;
                    }

                    return data;
                }

                @Override
                public boolean isFinished() {
                    return finished;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener listener) {
                    throw new UnsupportedOperationException();
                }
            };
        }

        @Override
        public BufferedReader getReader() throws IOException {
            Charset cs = Optional.ofNullable(getCharacterEncoding())
                    .map(Charset::forName)
                    .orElse(StandardCharsets.UTF_8);
            return new BufferedReader(new InputStreamReader(getInputStream(), cs));
        }

        @Override
        public String getHeader(String name) {
            if (HttpHeaders.CONTENT_ENCODING.equalsIgnoreCase(name)) {
                return null;
            }
            return super.getHeader(name);
        }

        @Override
        public int getContentLength() {
            return decompressedData.length;
        }

        @Override
        public long getContentLengthLong() {
            return decompressedData.length;
        }
    }
}
