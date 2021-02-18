package com.d1gaming.event.challenge;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.d1gaming.library.challenge.Challenge;


@RestController
@RequestMapping(value  = "/challengesapi")
@CrossOrigin(origins="localhost:4200")
@PreAuthorize("permitAll()")
public class ChallengeController {

	@Autowired
	private ChallengeService challengeServ;
	
	@GetMapping(value = "challenges/search", params="challengeId")
	public ResponseEntity<Object> getChallengeById(@RequestParam(required = true)final String challengeId) throws InterruptedException, ExecutionException{
		if(challengeId == null) {
			return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
		}
		Challenge challenge = challengeServ.getChallengeById(challengeId);
		if(challenge == null) {
			return new ResponseEntity<>("Challenge not found.",HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<>(challenge, HttpStatus.FOUND);
	}
	
	@GetMapping(value = "/challenges")
	public ResponseEntity<Object> getAllChallenges() throws InterruptedException, ExecutionException {
		List<Challenge> ls = challengeServ.getAllChallenges();
		if(ls.isEmpty()) {
			return new ResponseEntity<>("No content",HttpStatus.NO_CONTENT);
		}
		return new ResponseEntity<>(ls,HttpStatus.OK);
	}
	
	@PostMapping(value = "challenges/save", params = "userId")
	@PreAuthorize("hasRole('PLAYER') or hasRole('ADMIN')")
	public ResponseEntity<String> postOneVrsOneChallenge(@RequestParam(required = true)String userId, 
														 @RequestBody Challenge challenge) throws InterruptedException, ExecutionException{
		String response = challengeServ.postOneVOneChallenge(userId, challenge);
		if(response.equals("Could not create challenge.")) {
			return new ResponseEntity<>(response, HttpStatus.EXPECTATION_FAILED);
		}
		return new ResponseEntity<>(response, HttpStatus.OK);
	
	}
	
	@PostMapping(value = "/challenges/save")
	@PreAuthorize("hasRole('PLAYER') or hasRole('ADMIN')")
	public ResponseEntity<Object> postChallenge(@RequestParam(required = true) Map<String,Object> userMap , 
												@RequestParam(required = true) String userAdminId, 
												@RequestBody Challenge challenge) throws InterruptedException, ExecutionException{
		String response = challengeServ.postChallenge(userMap, userAdminId, challenge);
		if(response.equals("Could not create challenge.")) {
			return new ResponseEntity<>(response, HttpStatus.EXPECTATION_FAILED);
		}
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@DeleteMapping("/challenges")
	@PreAuthorize("hasRole('CHALLENGE_ADMIN') or hasRole('ADMIN')")
	public ResponseEntity<Object> deleteChallengeById(@RequestParam(required = true)String challengeId, 
													  @RequestParam(required = false)String challengeField) throws InterruptedException, ExecutionException{
		if(challengeField != null) {
			String response = challengeServ.deleteChallengeField(challengeId, challengeField);
			if(response.equals("Challenge not found.")) {
				return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
			}
			return new ResponseEntity<>(response, HttpStatus.OK);
		}
		String response = challengeServ.deleteChallengeById(challengeId);
		if(response.equals("Challenge not found.")) {
			return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	@PutMapping("/challenges/update")
	@PreAuthorize("hasRole('ADMIN') or hasRole('CHALLENGE_ADMIN')")
	public ResponseEntity<Object> updateChallenge(@RequestBody Challenge challenge) throws InterruptedException, ExecutionException{
		String response = challengeServ.updateChallenge(challenge);
		if(response.equals("Challenge not found.")) {
			return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	@PutMapping(value = "/challenges/update",  params = "challengeId, challengeField, replaceValue")
	@PreAuthorize("hasRole('ADMIN') or hasRole('CHALLENGE_ADMIN')")
	public ResponseEntity<Object> upadateChallengeField(@RequestParam(required = true)String challengeId, 
														@RequestParam(required = true)String challengeField, 
														@RequestParam(required = true)String replaceValue) throws InterruptedException, ExecutionException{
		String response = challengeServ.updateField(challengeId, challengeField, replaceValue);
		if(response.equals("Challenge not found.")) {
			return new ResponseEntity<>(response,HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<>(response,HttpStatus.OK);
	}
}
