package com.d1gaming.event.teamChallenge;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.d1gaming.library.challenge.Challenge;
import com.d1gaming.library.match.Match;
import com.d1gaming.library.request.MatchChallengeRequest;
import com.d1gaming.library.request.TeamChallengeRequest;
import com.d1gaming.library.response.MessageResponse;

@RestController
@PreAuthorize("permitAll()")
@RequestMapping(value = "/teamchallengesapi")
public class TeamChallengeController {

	@Autowired
	private TeamChallengeService teamChallengeService;
	
	
	@GetMapping(value = "/teamChallenges/matches/search")
	public ResponseEntity<Match> getTeamMatchFromChallenge(@RequestParam(required = true)String matchId,
														   @RequestParam(required = true)String challengeId) throws InterruptedException, ExecutionException{
		Optional<Match> match = teamChallengeService.getTeamMatchFromChallenge(matchId, challengeId);
		if(match.isPresent()) {
			return new ResponseEntity<Match>(match.get(), HttpStatus.OK);
		}
		return new ResponseEntity<Match>(match.get(), HttpStatus.NOT_FOUND);
	}
	
	@GetMapping(value = "/teamChallenges/cod/all")
	public ResponseEntity<List<Challenge>> getAllCodChallengesFromTeam(@RequestParam(required = true)String teamId) throws InterruptedException, ExecutionException{
		List<Challenge> teamChallengeList = teamChallengeService.getAllCodChallengesFromTeam(teamId);
		if(teamChallengeList.isEmpty()) {
			return new ResponseEntity<List<Challenge>>(teamChallengeList, HttpStatus.NO_CONTENT);
		}
		return new ResponseEntity<List<Challenge>>(teamChallengeList, HttpStatus.OK);
	}
	
	@GetMapping(value = "/teamChallenges/cod/search")
	public ResponseEntity<Challenge> getCodChallengeFromTeam(@RequestParam(required = true)String teamId,
															  @RequestParam(required = true)String challengeId) throws InterruptedException, ExecutionException{
		Optional<Challenge> teamChallenge = teamChallengeService.getCodChallengeFromTeamById(teamId, challengeId);
		if(teamChallenge.isEmpty()) {
			return new ResponseEntity<Challenge>(teamChallenge.get(), HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<Challenge>(teamChallenge.get(), HttpStatus.OK);
	}
	
	@GetMapping(value = "/teamChallenges/matches/disputed")
	public ResponseEntity<List<Match>> getDisputedMatchesFromChallenge(@RequestParam(required = true)String challengeId) throws InterruptedException, ExecutionException{
		List<Match> matchesList = teamChallengeService.getAllDisputedMatchesFromChallenge(challengeId);
		return new ResponseEntity<List<Match>>(matchesList, HttpStatus.OK);
	}
	
	@PostMapping(value = "/teamChallenges/matches/cod/save")
	@PreAuthorize("hasRole('ADMIN') or hasRole('TEAM_ADMIN')")
	public ResponseEntity<MessageResponse> uploadCodMatchResult(@RequestBody(required = true)MatchChallengeRequest matchChallengeRequest) throws InterruptedException, ExecutionException{
		String response = teamChallengeService.uploadCodMatchResult(matchChallengeRequest.getMatchChallengeMatch(), matchChallengeRequest.getMatchChallengeChallengeId(), matchChallengeRequest.getMatchChallengeTeam().getTeamId());
		if(response.equals("Not found.")) {
			return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.OK);
	}
	
	@PostMapping(value = "/teamChallenges/cod/add")
	@PreAuthorize("hasRole('ADMIN') or hasRole('PLAYER')")
	public ResponseEntity<MessageResponse> addTeamToCodChallenge(@RequestBody(required = true)TeamChallengeRequest teamChallengeRequest) throws InterruptedException, ExecutionException{
		String response = teamChallengeService.addTeamToChallenge(teamChallengeRequest.getTeam(), teamChallengeRequest.getChallenge());
		if(response.equals("Not found.")) {
			return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.OK);
	}
	
	//POST, add match to teams
	
	@DeleteMapping(value="/teamChallenges/cod/remove")
	@PreAuthorize("hasRole('PLAYER') or hasRole('PLAYER')")
	public ResponseEntity<MessageResponse> removeTeamFromCodChallenge(@RequestBody(required = true)TeamChallengeRequest teamChallengeRequest) throws InterruptedException, ExecutionException{
		String response = teamChallengeService.removeTeamFromChallenge(teamChallengeRequest.getTeam(), teamChallengeRequest.getChallenge());
		if(response.equals("Not found.")) {
			return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.OK);
	}
	
}
