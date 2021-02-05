package com.d1gaming.event.tournament;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.d1gaming.library.team.Team;
import com.d1gaming.library.tournament.Tournament;
import com.d1gaming.library.user.User;

@RestController
@CrossOrigin(origins = "localhost:4200")
@RequestMapping( value = "/tournamentsapi")
public class TournamentController {

	@Autowired
	private TournamentService tournamentService;
	
	@GetMapping(value="/tournaments")
	public ResponseEntity<?> getAllTournaments() throws InterruptedException, ExecutionException{
		List<Tournament> tournamentLs = tournamentService.getAllTournaments();
		if(tournamentLs.isEmpty()) {
			return new ResponseEntity<>(tournamentLs, HttpStatus.NO_CONTENT);
		}
		return new ResponseEntity<>(tournamentLs, HttpStatus.OK);
	}
	
	@GetMapping(value = "/tournaments/search", params="tournamentId")
	public ResponseEntity<?> getTournamentById(@RequestParam(required = true)String tournamentId) throws InterruptedException, ExecutionException{
		Optional<Tournament> tournament = tournamentService.getTournamentById(tournamentId);
		if(tournament == null) {
			return new ResponseEntity<>(tournament, HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<>(tournament, HttpStatus.OK);
	}
	
	@GetMapping(value = "/tournaments/search", params="tournamentName")
	public ResponseEntity<?> getTournamentByName(@RequestParam(required = true)String tournamentName) throws InterruptedException, ExecutionException{
		Optional<Tournament> tournament = tournamentService.getTournamentByName(tournamentName);
		if(tournament == null) {
			return new ResponseEntity<>(tournament, HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<>(tournament, HttpStatus.OK);
	}
	
	@GetMapping(value = "/tournaments/teams")
	public ResponseEntity<?> getAllTeamsOnTournament(@RequestParam(required = true)String tournamentId) throws InterruptedException, ExecutionException{
		List<Team> teams = tournamentService.getTournamentTeams(tournamentId);
		if(teams.isEmpty()) {
			return new ResponseEntity<>(teams, HttpStatus.NO_CONTENT);
		}
		return new ResponseEntity<>(teams, HttpStatus.OK);
	}
	
	
	@PostMapping(value = "/tournaments/save" )
	public ResponseEntity<?> saveTournament(@RequestBody(required = true)User user, 
											@RequestBody(required = true)Tournament tournament) throws InterruptedException, ExecutionException{
		String response = tournamentService.postTournament(user, tournament);	
		if(response.equals("Not found.")) {
			return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<>(response, HttpStatus.OK);
	} 
	
	@DeleteMapping(value = "/tournaments/delete")
	public ResponseEntity<?> deleteTournament(@RequestParam(required = true) String tournamentId) throws InterruptedException, ExecutionException{
		String response = tournamentService.deleteTournament(tournamentId);
		if(response.equals("Tournament not found.")) {
			return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	@DeleteMapping(value = "/tournaments/delete")
	public ResponseEntity<?> deleteTournamentField(@RequestParam(required = true)String tournamentId,
												   @RequestParam(required = true)String tournamentField) throws InterruptedException, ExecutionException{
		String response = tournamentService.deleteTournamentField(tournamentId, tournamentField);
		if(response.equals("Not found.")) {
			return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	@PutMapping(value = "tournaments/update")
	public ResponseEntity<?> updateTournament(@RequestBody Tournament tournament) throws InterruptedException, ExecutionException{
		String response = tournamentService.updateTournament(tournament);
		if(response.equals("Tournament not found.")) {
			return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	@PostMapping(value = "/tournaments/teams/add")
	public ResponseEntity<?> addTeamToTournament(@RequestBody(required = true)Team team,
												 @RequestBody(required = true)Tournament tournament) throws InterruptedException, ExecutionException{
		String response = tournamentService.addTeamToTournament(team, tournament);
		if(response.equals("Not found.")) {
			return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	@DeleteMapping(value = "/tournaments/teams/remove")
	public ResponseEntity<?> removeTeamFromTournament(@RequestBody(required = true)Team team,
													  @RequestBody(required = true)Tournament tournament) throws InterruptedException, ExecutionException{
		String response = tournamentService.removeTeamFromTournament(team, tournament);
		if(response.equals("Not found.")) {
			return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
}