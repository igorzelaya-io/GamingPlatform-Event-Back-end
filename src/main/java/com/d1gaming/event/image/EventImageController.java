package com.d1gaming.event.image;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.d1gaming.library.image.ImageModel;

@RestController
@CrossOrigin( origins = "localhost:4200")
@RequestMapping( value = "/eventimagesapi")
public class EventImageController {
	
	@Autowired
	private EventImageService eventImagesService;
	
	@PostMapping(value = "/images/save", params = "teamId")
	public ResponseEntity<?> saveTeamImage(@RequestParam(required = true)String teamId,
										   @RequestBody(required = true)MultipartFile file) throws InterruptedException, ExecutionException, IOException{
		ImageModel image = new ImageModel(file.getOriginalFilename(), file.getContentType(),
											file.getBytes());
		String response = eventImagesService.saveTeamImage(teamId, image);
		if(response == "Team not found.") {
			return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	@GetMapping( value = "/images/search", params = "teamId")
	public ResponseEntity<?> getTeamImage(@RequestParam(required = true) String teamId) throws InterruptedException, ExecutionException{
		Optional<ImageModel> image = eventImagesService.getTeamImage(teamId);
		if(image == null) {
			return new ResponseEntity<>(image, HttpStatus.NO_CONTENT);
		}
		return new ResponseEntity<>(image.get(), HttpStatus.OK);
	}

	
}
