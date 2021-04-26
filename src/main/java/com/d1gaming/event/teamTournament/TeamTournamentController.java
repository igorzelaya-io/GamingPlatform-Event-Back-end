package com.d1gaming.event.teamTournament;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.d1gaming.library.match.Match;
import com.d1gaming.library.request.TeamTournamentRequest;
import com.d1gaming.library.response.MessageResponse;
import com.d1gaming.library.team.Team;
import com.d1gaming.library.tournament.Tournament;

@RestController
@CrossOrigin(origins = "localhost:4200")
@PreAuthorize("permitAll()")
@RequestMapping(value = "/teamtournamentsapi")
public class TeamTournamentController {

	@Autowired
	private TeamTournamentService teamTournamentService;
	
	@GetMapping(value = "/teamTournaments/matches/search")
	public ResponseEntity<Object> getTeamMatchFromTournament(@RequestParam(required = true)String matchId,
															 @RequestParam(required = true)String tournamentId) throws InterruptedException, ExecutionException{
		Optional<Match> tournamentMatch = teamTournamentService.getTeamMatchFromTournament(matchId, tournamentId);
		if(tournamentMatch == null) {
			return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<Object>(tournamentMatch.get(), HttpStatus.OK);
	}
	
	@GetMapping(value = "/teamTournaments/cod/all")
	public ResponseEntity<List<Tournament>> getAllCodTournamentsFromTeam(@RequestParam(required = true)String teamId) throws InterruptedException, ExecutionException{
		List<Tournament> teamTournaments = teamTournamentService.getAllCodTournamentsFromTeam(teamId);
		if(teamTournaments.isEmpty()) {
			return new ResponseEntity<List<Tournament>>(teamTournaments, HttpStatus.NO_CONTENT);
		}
		return new ResponseEntity<List<Tournament>>(teamTournaments, HttpStatus.OK);
	}
	
	@GetMapping(value = "/teamTournaments/fifa/all")
	public ResponseEntity<List<Tournament>> getAllFifaTournamentsFromTeam(@RequestParam(required = true)String teamId) throws InterruptedException, ExecutionException{
		List<Tournament> teamTournaments = teamTournamentService.getAllFifaTournamentsFromTeam(teamId);
		if(teamTournaments.isEmpty()) {
			return new ResponseEntity<List<Tournament>>(teamTournaments, HttpStatus.NO_CONTENT);
		}
		return new ResponseEntity<List<Tournament>>(teamTournaments, HttpStatus.OK);
	}
	
	@GetMapping(value = "/teamTournaments/fifa/search")
	public ResponseEntity<Tournament> getFifaTournamentFromTeamById(@RequestParam(required = true)String teamId, 
													                @RequestParam(required = true)String tournamentId) throws InterruptedException, ExecutionException{
		Optional<Tournament> tournament = teamTournamentService.getFifaTournamentFromTeamById(teamId, tournamentId);
		if(tournament == null) {
			return new ResponseEntity<>(null, HttpStatus.NOT_FOUND );
		}
		return new ResponseEntity<Tournament>(tournament.get(), HttpStatus.OK);
	}
	
	@GetMapping(value = "/teamTournaments/cod/search")
	public ResponseEntity<Tournament> getCodTournamentFromTeamById(@RequestParam(required = true)String teamId, 
													               @RequestParam(required = true)String tournamentId) throws InterruptedException, ExecutionException{
		Optional<Tournament> tournament = teamTournamentService.getCodTournamentFromTeamById(teamId, tournamentId);
		if(tournament == null) {
			return new ResponseEntity<>(null, HttpStatus.NOT_FOUND );
		}
		return new ResponseEntity<Tournament>(tournament.get(), HttpStatus.OK);
	}
	
	@GetMapping(value ="/teamTournaments/bestTeams")
	public ResponseEntity<List<Team>> getTeamsWithMostWins(@RequestBody(required = true)Tournament tournament) throws InterruptedException, ExecutionException{
		List<Team> teamsWithMostWins = teamTournamentService.getAllTeamsWithHighestWins(tournament);
		if(teamsWithMostWins.isEmpty()) {
			return new ResponseEntity<List<Team>>(teamsWithMostWins, HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<List<Team>>(teamsWithMostWins, HttpStatus.OK);
		
	}
	
	@PostMapping(value = "/teamTournaments/fifa/add")
	@PreAuthorize("hasRole('ADMIN') or hasRole('TOURNEY_ADMIN') or hasRole('PLAYER')")
	public ResponseEntity<MessageResponse> addTeamToFifaTournament(@RequestBody(required = true)TeamTournamentRequest teamTournamentRequest) throws InterruptedException, ExecutionException{
		String response = teamTournamentService.addTeamToFifaTournament(teamTournamentRequest.getTeam(), teamTournamentRequest.getTournament());
		if(response.equals("Not found.")) {
			return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.NOT_FOUND);
		}
		else if(response.equals("Tournament is already full.")) {
			return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.BAD_REQUEST);
		}
		else if(response.equals("Team is already part of tournament.")) {
			return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.BAD_REQUEST);
		}
		return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.OK);
	}
	
	@PostMapping(value = "/teamTournaments/cod/add")
	@PreAuthorize("hasRole('ADMIN') or hasRole('TOURNEY_ADMIN') or hasRole('PLAYER')")
	public ResponseEntity<MessageResponse> addTeamToCodTournament(@RequestBody(required = true)TeamTournamentRequest teamTournamentRequest) throws InterruptedException, ExecutionException {
		String response = teamTournamentService.addTeamToCodTournament(teamTournamentRequest.getTeam(), teamTournamentRequest.getTournament());
		if(response.equals("Not found.")) {
			return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.NOT_FOUND);
		}
		else if(response.equals("Tournament is already full.")) {
			return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.BAD_REQUEST);
		}		
		else if(response.equals("Team is already part of tournament.")) {
			return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.BAD_REQUEST);
		}
		
		return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.OK);
	}
	
	
	@DeleteMapping(value = "/teamTournaments/cod/remove")
	@PreAuthorize("hasRole('ADMIN') or hasRole('TOURNEY_ADMIN') or hasRole('PLAYER')")
	public ResponseEntity<MessageResponse> removeTeamFromCodTournament(@RequestBody(required = true)TeamTournamentRequest teamTournamentRequest) throws InterruptedException, ExecutionException{
		String response = teamTournamentService.removeTeamFromCodTournament(teamTournamentRequest.getTeam(), teamTournamentRequest.getTournament());
		if(response.equals("Not found.")) {
			return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.OK);
	}
	
	@DeleteMapping(value = "/teamTournaments/fifa/remove")
	@PreAuthorize("hasRole('ADMIN') or hasRole('TOURNEY_ADMIN') or hasRole('PLAYER')")
	public ResponseEntity<MessageResponse> removeTeamFromFifaTournament(@RequestBody(required = true)TeamTournamentRequest teamTournamentRequest) throws InterruptedException, ExecutionException{
		String response = teamTournamentService.removeTeamFromFifaTournament(teamTournamentRequest.getTeam(), teamTournamentRequest.getTournament());
		if(response.equals("Not found.")) {
			return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.OK);
	}

}
