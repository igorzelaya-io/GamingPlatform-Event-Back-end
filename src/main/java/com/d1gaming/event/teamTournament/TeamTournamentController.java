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

	@GetMapping(value = "/teamTournaments")
	public ResponseEntity<List<Tournament>> getAllTournamentsFromTeam(@RequestParam(required = true)String teamId) throws InterruptedException, ExecutionException{
		List<Tournament> teamTournaments = teamTournamentService.getAllTournamentsFromTeam(teamId);
		if(teamTournaments.isEmpty()) {
			return new ResponseEntity<List<Tournament>>(teamTournaments, HttpStatus.NO_CONTENT);
		}
		return new ResponseEntity<List<Tournament>>(teamTournaments, HttpStatus.OK);
	
	}
	
	@GetMapping(value = "/teamTournaments/search")
	public ResponseEntity<Tournament> getTournamentFromTeamById(@RequestParam(required = true)String teamId, 
													            @RequestParam(required = true)String tournamentId) throws InterruptedException, ExecutionException{
		Optional<Tournament> tournament = teamTournamentService.getTournamentFromTeamById(teamId, tournamentId);
		if(tournament == null) {
			return new ResponseEntity<>(null, HttpStatus.NOT_FOUND );
		}
		return new ResponseEntity<Tournament>(tournament.get(), HttpStatus.OK);
	}
	
	@PostMapping(value = "/teamTournaments/add")
	@PreAuthorize("hasRole('ADMIN') or hasRole('TOURNEY_ADMIN') or hasRole('PLAYER')")
	public ResponseEntity<MessageResponse> addTeamToTournament(@RequestBody(required = true)TeamTournamentRequest teamTournamentRequest) throws InterruptedException, ExecutionException{
		String response = teamTournamentService.addTeamToTournament(teamTournamentRequest.getTeam(), teamTournamentRequest.getTournament());
		if(response.equals("Not found.")) {
			return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.OK);
	}
	
	@DeleteMapping(value = "/teamTournaments/remove")
	@PreAuthorize("hasRole('ADMIN') or hasRole('TOURNEY_ADMIN') or hasRole('PLAYER')")
	public ResponseEntity<MessageResponse> removeTeamFromTournament(@RequestBody(required = true)TeamTournamentRequest teamTournamentRequest) throws InterruptedException, ExecutionException{
		String response = teamTournamentService.removeTeamFromTournament(teamTournamentRequest.getTeam(), teamTournamentRequest.getTournament());
		if(response.equals("Not found.")) {
			return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.OK);
	}

}
