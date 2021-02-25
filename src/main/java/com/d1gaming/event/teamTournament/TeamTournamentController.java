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
	public ResponseEntity<?> getAllTournamentsFromTeam(@RequestParam(required = true)String teamId) throws InterruptedException, ExecutionException{
		List<Tournament> teamTournaments = teamTournamentService.getAllTournamentsFromTeam(teamId);
		if(teamTournaments.isEmpty()) {
			return new ResponseEntity<>(teamTournaments, HttpStatus.NO_CONTENT);
		}
		return new ResponseEntity<>(teamTournaments, HttpStatus.OK);
	
	}
	
	@GetMapping(value = "/teamTournaments/search")
	public ResponseEntity<?> getTournamentFromTeamById(@RequestParam(required = true)String teamId, 
													   @RequestParam(required = true)String tournamentId) throws InterruptedException, ExecutionException{
		Optional<Tournament> tournament = teamTournamentService.getTournamentFromTeamById(teamId, tournamentId);
		if(tournament == null) {
			return new ResponseEntity<>(tournament, HttpStatus.NOT_FOUND );
		}
		return new ResponseEntity<>(tournament.get(), HttpStatus.OK);
	}
	
	@PostMapping(value = "/tournaments/teams/add")
	@PreAuthorize("hasRole('ADMIN') or hasRole('TOURNEY_ADMIN')")
	public ResponseEntity<?> addTeamToTournament(@RequestBody(required = true)Team team,
												 @RequestBody(required = true)Tournament tournament) throws InterruptedException, ExecutionException{
		String response = teamTournamentService.addTeamToTournament(team, tournament);
		if(response.equals("Not found.")) {
			return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	@DeleteMapping(value = "/tournaments/teams/remove")
	@PreAuthorize("hasRole('ADMIN') or hasRole('TOURNEY_ADMIN')")
	public ResponseEntity<?> removeTeamFromTournament(@RequestBody(required = true)Team team,
													  @RequestBody(required = true)Tournament tournament) throws InterruptedException, ExecutionException{
		String response = teamTournamentService.removeTeamFromTournament(team, tournament);
		if(response.equals("Not found.")) {
			return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

}
