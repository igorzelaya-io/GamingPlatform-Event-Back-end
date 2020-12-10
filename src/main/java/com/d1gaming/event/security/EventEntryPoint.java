package com.d1gaming.event.security;

import java.io.IOException;
import java.util.Collections;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class EventEntryPoint implements AuthenticationEntryPoint{

	private static final Logger logger = LoggerFactory.getLogger(EventEntryPoint.class);

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
							AuthenticationException authException) throws IOException {
		
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		
		//response.sendError(HttpServletResponse.SC_UNAUTHORIZED,"Error: Unauthorized");
		logger.error("Unauthorized error: {}", authException.getMessage());
		String message = "";
		if(authException.getCause() != null) {
			message = authException.getCause().toString() + " " + authException.getMessage();
		}
		else {
			message = authException.getMessage();
		}
		
		byte[] body = new ObjectMapper().writeValueAsBytes(Collections.singletonMap("error", message));
		response.getOutputStream().write(body);
	}

}
