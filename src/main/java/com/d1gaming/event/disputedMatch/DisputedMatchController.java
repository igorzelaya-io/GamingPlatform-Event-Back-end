package com.d1gaming.event.disputedMatch;

import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.d1gaming.library.match.DisputedMatch;
import com.d1gaming.library.response.MessageResponse;

@RestController
@RequestMapping(value = "/disputedMatches")
@PreAuthorize("permitAll()")
public class DisputedMatchController {

	@Autowired
	private DisputedMatchService disputedMatchService;
	
	@PostMapping
	public ResponseEntity<MessageResponse> postDisputedMatch(@RequestBody(required = true)DisputedMatch disputedMatch) throws InterruptedException, ExecutionException{
		String response = disputedMatchService.disputeMatch(disputedMatch);
		return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.OK);
	}
	
	@GetMapping
	public ResponseEntity<?> getDisputedMatchFromChallenge(@RequestParam(required = false)String challengeId,
																	   @RequestParam(required = false)String tournamentId,
																	   @RequestParam(required = false)String matchId) throws InterruptedException, ExecutionException{
		if(matchId == null) {
			return new ResponseEntity<>(disputedMatchService.getAllDisputedMatches(), HttpStatus.OK);
		}
		if(tournamentId != null) {
			DisputedMatch match = disputedMatchService.getDisputedMatchFromTournament(tournamentId, matchId);
			if(match == null) {
				return new ResponseEntity<DisputedMatch>(match, HttpStatus.NOT_FOUND);
			}
			return new ResponseEntity<DisputedMatch>(match, HttpStatus.OK);
		}
		DisputedMatch match = disputedMatchService.getDisputedMatchFromChallenge(challengeId, matchId);
		if(match == null) {
			return new ResponseEntity<DisputedMatch>(match, HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<DisputedMatch>(match, HttpStatus.OK);
	}
	
}
