package com.d1gaming.event.tournament;

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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.d1gaming.library.team.Team;
import com.d1gaming.library.tournament.Tournament;
import com.d1gaming.library.tournament.TournamentCreationRequest;

@RestController
@CrossOrigin(origins = "localhost:4200")
@RequestMapping( value = "/tournamentsapi")
@PreAuthorize("permitAll()")
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
	
	@GetMapping(value="/tournaments/upcoming")
	public ResponseEntity<?> getAllTournamentsAfterOneWeek() throws InterruptedException, ExecutionException{
		List<Tournament> tournamentsInOneWeek = tournamentService.getAllTournamentsAfterOneWeek();
		if(tournamentsInOneWeek.isEmpty()) {
			return new ResponseEntity<>(tournamentsInOneWeek, HttpStatus.NO_CONTENT);
		}
		return new ResponseEntity<>(tournamentsInOneWeek, HttpStatus.OK);
	}
	
	@GetMapping(value = "/tournaments/now")
	public ResponseEntity<?> getAllTournamentsNow() throws InterruptedException, ExecutionException{
		List<Tournament> tournamentsNow = tournamentService.getAllTournamentsBeforeOneWeek();
		if(tournamentsNow.isEmpty()) {
			return new ResponseEntity<>(tournamentsNow, HttpStatus.NO_CONTENT);
		}
		return new ResponseEntity<>(tournamentsNow, HttpStatus.OK);
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
	@PreAuthorize("hasRole('PLAYER') or hasRole('ADMIN')")
	public ResponseEntity<?> saveTournament(@RequestBody(required = true)TournamentCreationRequest tournament) throws InterruptedException, ExecutionException{
		String response = tournamentService.postTournament(tournament.getTournamentUserModerator(), tournament.getTournamentToBeCreated());	
		if(response.equals("Not found.")) {
			return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<>(response, HttpStatus.OK);
	} 
	
	@DeleteMapping(value = "/tournaments/delete")
	@PreAuthorize("hasRole('TOURNEY_ADMIN') or hasRole('ADMIN')")
	public ResponseEntity<?> deleteTournament(@RequestBody(required = true) Tournament tournament) throws InterruptedException, ExecutionException{
		String response = tournamentService.deleteTournament(tournament);
		if(response.equals("Tournament not found.")) {
			return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	@DeleteMapping(value = "/tournaments/delete/field")
	@PreAuthorize("hasRole('PLAYER') or hasRole('TOURNEY_ADMIN')")
	public ResponseEntity<?> deleteTournamentField(@RequestParam(required = true)String tournamentId,
												   @RequestParam(required = true)String tournamentField) throws InterruptedException, ExecutionException{
		String response = tournamentService.deleteTournamentField(tournamentId, tournamentField);
		if(response.equals("Not found.")) {
			return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	@PutMapping(value = "tournaments/update")
	@PreAuthorize("hasRole('PLAYER') or hasRole('TOURNEY_ADMIN')")
	public ResponseEntity<?> updateTournament(@RequestBody Tournament tournament) throws InterruptedException, ExecutionException{
		String response = tournamentService.updateTournament(tournament);
		if(response.equals("Tournament not found.")) {
			return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
}