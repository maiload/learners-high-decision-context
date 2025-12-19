package com.example.opa.policydecisionlog.command.filter;

import com.example.opa.policydecisionlog.shared.config.GzipProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

class GzipDecompressionFilterTest {

    private GzipDecompressionFilter filter;
    private GzipProperties gzipProperties;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        gzipProperties = new GzipProperties("/logs", 64 * 1024, 1024 * 1024);
        filter = new GzipDecompressionFilter(gzipProperties);
        filterChain = mock(FilterChain.class);
    }

    @Nested
    @DisplayName("shouldNotFilter")
    class ShouldNotFilter {

        @Test
        @DisplayName("설정된 경로가 아닌 요청이 주어지면 필터링하지 않는다")
        void givenOtherPath_whenShouldNotFilter_thenReturnsTrue() throws ServletException {
            // given
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/other/path");

            // when
            boolean result = filter.shouldNotFilter(request);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("설정된 경로와 일치하는 요청이 주어지면 필터링한다")
        void givenExactPath_whenShouldNotFilter_thenReturnsFalse() throws ServletException {
            // given
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/logs");

            // when
            boolean result = filter.shouldNotFilter(request);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("설정된 경로의 하위 경로 요청이 주어지면 필터링한다")
        void givenSubPath_whenShouldNotFilter_thenReturnsFalse() throws ServletException {
            // given
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/logs/sub");

            // when
            boolean result = filter.shouldNotFilter(request);

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("doFilterInternal")
    class DoFilterInternal {

        @Test
        @DisplayName("gzip 인코딩이 없는 요청이 주어지면 원본을 그대로 전달한다")
        void givenNonGzipRequest_whenDoFilter_thenPassesUnchanged() throws ServletException, IOException {
            // given
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/logs");
            request.setContent("plain text".getBytes(StandardCharsets.UTF_8));
            MockHttpServletResponse response = new MockHttpServletResponse();

            // when
            filter.doFilterInternal(request, response, filterChain);

            // then
            then(filterChain).should().doFilter(request, response);
        }

        @Test
        @DisplayName("gzip 인코딩된 요청이 주어지면 압축을 해제한다")
        void givenGzipRequest_whenDoFilter_thenDecompresses() throws ServletException, IOException {
            // given
            String originalContent = "Hello, World!";
            byte[] gzippedContent = gzip(originalContent);

            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/logs");
            request.addHeader(HttpHeaders.CONTENT_ENCODING, "gzip");
            request.setContent(gzippedContent);
            MockHttpServletResponse response = new MockHttpServletResponse();

            // when
            filter.doFilterInternal(request, response, filterChain);

            // then
            then(filterChain).should().doFilter(argThat(req -> {
                try {
                    HttpServletRequest wrappedRequest = (HttpServletRequest) req;
                    byte[] body = wrappedRequest.getInputStream().readAllBytes();
                    return new String(body, StandardCharsets.UTF_8).equals(originalContent);
                } catch (IOException e) {
                    return false;
                }
            }), eq(response));
        }

        @Test
        @DisplayName("압축된 크기가 제한을 초과하면 413 에러를 반환한다")
        void givenOversizedCompressedContent_whenDoFilter_thenReturns413() throws ServletException, IOException {
            // given
            GzipProperties smallLimitProps = new GzipProperties("/logs", 10, 1024 * 1024);
            GzipDecompressionFilter smallLimitFilter = new GzipDecompressionFilter(smallLimitProps);

            byte[] gzippedContent = gzip("This content is larger than 10 bytes when compressed");

            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/logs");
            request.addHeader(HttpHeaders.CONTENT_ENCODING, "gzip");
            request.setContent(gzippedContent);
            MockHttpServletResponse response = new MockHttpServletResponse();

            // when
            smallLimitFilter.doFilterInternal(request, response, filterChain);

            // then
            assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
            then(filterChain).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("gzip 요청이 주어지면 Content-Encoding 헤더가 제거된다")
        void givenGzipRequest_whenDoFilter_thenRemovesContentEncodingHeader() throws ServletException, IOException {
            // given
            String originalContent = "test";
            byte[] gzippedContent = gzip(originalContent);

            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/logs");
            request.addHeader(HttpHeaders.CONTENT_ENCODING, "gzip");
            request.setContent(gzippedContent);
            MockHttpServletResponse response = new MockHttpServletResponse();

            // when
            filter.doFilterInternal(request, response, filterChain);

            // then
            then(filterChain).should().doFilter(argThat(req -> {
                HttpServletRequest wrappedRequest = (HttpServletRequest) req;
                return wrappedRequest.getHeader(HttpHeaders.CONTENT_ENCODING) == null;
            }), eq(response));
        }

        @Test
        @DisplayName("gzip 요청이 주어지면 Content-Length가 압축 해제된 크기로 업데이트된다")
        void givenGzipRequest_whenDoFilter_thenUpdatesContentLength() throws ServletException, IOException {
            // given
            String originalContent = "Hello, World!";
            byte[] gzippedContent = gzip(originalContent);

            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/logs");
            request.addHeader(HttpHeaders.CONTENT_ENCODING, "gzip");
            request.setContent(gzippedContent);
            MockHttpServletResponse response = new MockHttpServletResponse();

            // when
            filter.doFilterInternal(request, response, filterChain);

            // then
            then(filterChain).should().doFilter(argThat(req -> {
                HttpServletRequest wrappedRequest = (HttpServletRequest) req;
                return wrappedRequest.getContentLength() == originalContent.length();
            }), eq(response));
        }
    }

    private byte[] gzip(String content) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOut = new GZIPOutputStream(baos)) {
            gzipOut.write(content.getBytes(StandardCharsets.UTF_8));
        }
        return baos.toByteArray();
    }
}
