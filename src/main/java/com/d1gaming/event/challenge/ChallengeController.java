package com.d1gaming.event.challenge;

import java.util.List;
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
import com.d1gaming.library.match.Match;
import com.d1gaming.library.response.MessageResponse;


@RestController
@RequestMapping(value  = "/challengesapi")
@CrossOrigin(origins="localhost:4200")
@PreAuthorize("permitAll()")
public class ChallengeController {

	@Autowired
	private ChallengeService challengeServ;
	
	@GetMapping(value = "/challenges/search", params="challengeId")
	public ResponseEntity<Object> getChallengeById(@RequestParam(required = true)final String challengeId) throws InterruptedException, ExecutionException{
		Challenge challenge = challengeServ.getChallengeById(challengeId);
		if(challenge == null) {
			return new ResponseEntity<>(new MessageResponse("Challenge not found."),HttpStatus.NOT_FOUND);
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
	
	@GetMapping(value="/challenges/upcoming")
	public ResponseEntity<List<Challenge>> getAllChallengesAfterOneWeek() throws InterruptedException, ExecutionException{
		List<Challenge> challenges = challengeServ.getAllChallengesAfterOneWeek();
		if(challenges.isEmpty()) {
			return new ResponseEntity<List<Challenge>>(challenges, HttpStatus.NO_CONTENT);
		}
		return new ResponseEntity<List<Challenge>>(challenges, HttpStatus.OK);
	}
	
	@GetMapping(value="/challenges/now")
	public ResponseEntity<List<Challenge>> getAllChallengesBeforeOneWeek() throws InterruptedException, ExecutionException{
		List<Challenge> challenges = challengeServ.getAllChallengesBeforeOneWeek();
		if(challenges.isEmpty()) {
			return new ResponseEntity<List<Challenge>>(challenges, HttpStatus.NO_CONTENT);
		}
		return new ResponseEntity<List<Challenge>>(challenges, HttpStatus.OK);
	}
	
	@GetMapping(value="/challenges/matches/active")
	public ResponseEntity<List<Match>> getAllActiveMatches(@RequestParam(required = true)String challengeId) throws InterruptedException, ExecutionException{
		List<Match> matches = challengeServ.getAllChallengeMatches(challengeId);
		if(matches.isEmpty()) {
			return new ResponseEntity<List<Match>>(matches, HttpStatus.NO_CONTENT);
		}
		return new ResponseEntity<List<Match>>(matches, HttpStatus.OK);
	}
	
	@GetMapping(value="/challenges/matches/inactive")
	public ResponseEntity<List<Match>> getAllInactiveMatches(@RequestParam(required = true)String challengeId) throws InterruptedException, ExecutionException{
		List<Match> matches = challengeServ.getAllChallengeInactiveMatches(challengeId);
		if(matches.isEmpty()) {
			return new ResponseEntity<List<Match>>(matches, HttpStatus.NO_CONTENT);
		}
		return new ResponseEntity<List<Match>>(matches, HttpStatus.OK);
	}
	
	@GetMapping(value="/challenges/users/matches/active")
	public ResponseEntity<List<Match>> getAllUserMatches(@RequestParam(required = true)String userId,
					 									 @RequestParam(required = true)String challengeId) throws InterruptedException, ExecutionException{
		List<Match> matches = challengeServ.getAllUserActiveMatches(userId, challengeId);
		if(matches.isEmpty()) {
			return new ResponseEntity<List<Match>>(matches, HttpStatus.NO_CONTENT);
		}
		return new ResponseEntity<List<Match>>(matches, HttpStatus.OK);
	}
	
	@PostMapping(value="/challenges/add")
	@PreAuthorize("hasRole('PLAYER') or hasRole('ADMIN')")
	public ResponseEntity<Challenge> postChallenge(@RequestBody(required = true)Challenge challenge) throws InterruptedException, ExecutionException{
		Challenge postedChallenge = challengeServ.postChallenge(challenge.getChallengeModerator(), challenge);
		if(postedChallenge == null) {
			return new ResponseEntity<Challenge>(postedChallenge, HttpStatus.EXPECTATION_FAILED);
		}
		return new ResponseEntity<Challenge>(postedChallenge, HttpStatus.OK);
	}
	
	@PostMapping(value = "/challenges/start")
	@PreAuthorize("hasRole('CHALLENGE_ADMIN') or hasRole('ADMIN')")
	public ResponseEntity<Challenge> activateChallenge(@RequestParam(required = true)String challengeId) throws InterruptedException, ExecutionException{
		Challenge response = challengeServ.activateChallenge(challengeId);
		if(response == null) {
			return new ResponseEntity<Challenge>(response, HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<Challenge>(response, HttpStatus.OK);
	}
	
	@DeleteMapping("/challenges")
	@PreAuthorize("hasRole('CHALLENGE_ADMIN') or hasRole('ADMIN')")
	public ResponseEntity<MessageResponse> deleteChallengeById(@RequestParam(required = true)String challengeId, 
													  		   @RequestParam(required = false)String challengeField) throws InterruptedException, ExecutionException{
		if(challengeField != null) {
			String response = challengeServ.deleteChallengeField(challengeId, challengeField);
			if(response.equals("Challenge not found.")) {
				return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.NOT_FOUND);
			}
			return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.OK);
		}
		String response = challengeServ.deleteChallengeById(challengeId);
		if(response.equals("Challenge not found.")) {
			return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.OK);
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
