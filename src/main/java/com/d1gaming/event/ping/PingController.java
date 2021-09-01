package com.d1gaming.event.ping;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("permitAll()")
public class PingController {

	@GetMapping("/ping")
	public ResponseEntity<String> ping(){
		return new ResponseEntity<String>("pong", HttpStatus.OK);
	}
	
}
