package com.hdfclife.smartauth.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hdfclife.smartauth.dto.response.ErrorResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

        private final ObjectMapper objectMapper;

        public CustomAccessDeniedHandler(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }


        @Override
        public void handle(
                HttpServletRequest request,
                HttpServletResponse response,
                AccessDeniedException accessDeniedException
        ) throws IOException, ServletException {

            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.setCharacterEncoding(java.nio.charset.StandardCharsets.UTF_8.name());
            objectMapper.writeValue(response.getWriter(), ErrorResponse.of(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "You do not have permission to access this resource",
                    request));
        }
    }

