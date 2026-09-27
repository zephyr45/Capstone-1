package com.hdfc.jwtauth.logging;

import com.hdfc.jwtauth.logging.RequestLoggingFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class LoggingBehaviorTest {

    @Test
    void logsRequestAndResponseForACompletedRequest(CapturedOutput output) throws Exception {
        RequestLoggingFilter filter = new RequestLoggingFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/test/circuit-state");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            ((MockHttpServletResponse) servletResponse).setStatus(200);
        });

        assertThat(output.getOut())
                .contains("REQUEST GET /api/v1/test/circuit-state")
                .contains("RESPONSE GET /api/v1/test/circuit-state -> 200");
    }
}
