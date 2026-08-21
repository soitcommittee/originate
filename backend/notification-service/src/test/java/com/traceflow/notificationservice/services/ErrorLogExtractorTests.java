package com.traceflow.notificationservice.services;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorLogExtractorTests {
    private final ErrorLogExtractor extractor = new ErrorLogExtractor();

    @Test
    void extractsOnlyTheLatestExceptionBlock() {
        String logs = """
                2026-08-21T10:00:00.000Z 2026-08-21T10:00:00.000Z level=INFO message=healthy
                2026-08-21T10:00:01.000Z 2026-08-21T10:00:01.000Z level=ERROR message=request_failed java.lang.IllegalStateException: boom
                2026-08-21T10:00:01.001Z java.lang.IllegalStateException: boom
                2026-08-21T10:00:01.002Z     at com.traceflow.LoanService.disburse(LoanService.java:42)
                2026-08-21T10:00:01.003Z Caused by: java.net.SocketTimeoutException: timed out
                2026-08-21T10:00:02.000Z 2026-08-21T10:00:02.000Z level=INFO message=request_complete
                """;

        String excerpt = extractor.extract(logs);

        assertThat(excerpt).contains("level=ERROR", "IllegalStateException", "Caused by:");
        assertThat(excerpt).doesNotContain("message=healthy", "message=request_complete");
    }

    @Test
    void supportsPythonTracebacks() {
        String logs = """
                normal line
                Traceback (most recent call last):
                  File \"app.py\", line 7, in run
                    raise ValueError("bad input")
                ValueError: bad input
                """;

        assertThat(extractor.extract(logs))
                .startsWith("Traceback")
                .contains("ValueError: bad input")
                .doesNotContain("normal line");
    }

    @Test
    void returnsEmptyWhenNoErrorPatternExists() {
        assertThat(extractor.extract("level=INFO message=healthy\nlevel=WARN message=slow")).isEmpty();
    }
}
