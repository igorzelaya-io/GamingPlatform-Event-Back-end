package com.d1gaming.event.image;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.d1gaming.library.image.ImageModel;

@RestController
@CrossOrigin( origins = "localhost:4200")
@RequestMapping( value = "/eventimagesapi")
@PreAuthorize("permitAll()")
public class EventImageController {
	
	@Autowired
	private EventImageService eventImagesService;
	
	@GetMapping( value = "/images/search", params = "teamId")
	public ResponseEntity<?> getTeamImage(@RequestParam(required = true) String teamId) throws InterruptedException, ExecutionException{
		Optional<ImageModel> image = eventImagesService.getTeamImage(teamId);
		if(image == null) {
			return new ResponseEntity<>(image, HttpStatus.NO_CONTENT);
		}
		return new ResponseEntity<>(image.get(), HttpStatus.OK);
	}

	
}
