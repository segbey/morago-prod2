package com.morago.backend.config.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Set;

public class RequestSizeLimitFilter extends OncePerRequestFilter {

    private static final long MAX_BYTES = 16 * 1024;

    private static final Set<String> GUARDED_PATHS = Set.of(
            "/auth/login", "/auth/refresh",
            "/auth/password/reset/start",
            "/auth/password/reset/verify",
            "/auth/password/reset/confirm"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // (опционально) фильтруем только "тело-содержащие" методы
        String m = request.getMethod();
        boolean bodyMethod = "POST".equals(m) || "PUT".equals(m) || "PATCH".equals(m);
        if (!bodyMethod) return true;

        // трогаем только интересующие пути и JSON
        if (!GUARDED_PATHS.contains(request.getRequestURI())) return true;

        String ct = request.getContentType();
        return ct == null || !ct.toLowerCase().startsWith(MediaType.APPLICATION_JSON_VALUE);
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {

        String cl = request.getHeader(HttpHeaders.CONTENT_LENGTH);
        if (cl != null) {
            try {
                long contentLength = Long.parseLong(cl);
                if (contentLength > MAX_BYTES) {
                    reject(response);
                    return;
                }
            } catch (NumberFormatException ignore) {
            }
        }

        HttpServletRequest limited = new BoundedRequestWrapper(request, MAX_BYTES);
        try {
            chain.doFilter(limited, response);
        } catch (PayloadTooLargeException ex) {
            reject(response);
        }
    }

    private void reject(HttpServletResponse response) throws IOException {
        if (!response.isCommitted()) {
            response.setStatus(413);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"status\":413,\"error\":\"Payload Too Large\"}");
        }
    }

    static class BoundedRequestWrapper extends HttpServletRequestWrapper {
        private final long maxBytes;

        BoundedRequestWrapper(HttpServletRequest request, long maxBytes) {
            super(request);
            this.maxBytes = maxBytes;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            return new BoundedServletInputStream(super.getInputStream(), maxBytes);
        }

        @Override
        public BufferedReader getReader() throws IOException {
            return new BufferedReader(new InputStreamReader(
                    getInputStream(),
                    getCharacterEncoding() != null ? getCharacterEncoding() : "UTF-8"
            ));
        }
    }

    static class BoundedServletInputStream extends ServletInputStream {
        private final ServletInputStream delegate;
        private final long maxBytes;
        private long read;

        BoundedServletInputStream(ServletInputStream delegate, long maxBytes) {
            this.delegate = delegate;
            this.maxBytes = maxBytes;
        }

        @Override
        public int read() throws IOException {
            if (read >= maxBytes) throw new PayloadTooLargeException();
            int b = delegate.read();
            if (b != -1) read++;
            return b;
        }

        @Override
        public int read(@NonNull byte[] b) throws IOException {
            return read(b, 0, b.length);
        }

        @Override
        public int read(@NonNull byte[] b, int off, int len) throws IOException {
            if (read >= maxBytes) throw new PayloadTooLargeException();
            long remain = maxBytes - read;
            int toRead = (int) Math.min(len, remain);
            int n = delegate.read(b, off, toRead);
            if (n > 0) read += n;
            return n;
        }

        @Override public boolean isFinished() { return delegate.isFinished(); }
        @Override public boolean isReady() { return delegate.isReady(); }
        @Override public void setReadListener(ReadListener readListener) { delegate.setReadListener(readListener); }
    }

    static class PayloadTooLargeException extends IOException {}
}