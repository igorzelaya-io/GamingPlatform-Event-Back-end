package com.d1gaming.event.tournament;

import java.io.IOException;
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

import com.d1gaming.library.match.Match;
import com.d1gaming.library.response.MessageResponse;
import com.d1gaming.library.team.Team;
import com.d1gaming.library.tournament.Tournament;
import com.d1gaming.library.tournament.TournamentCreationRequest;

@RestController
@CrossOrigin(origins = "http://34.122.97.231")
@RequestMapping( value = "/tournamentsapi")
@PreAuthorize("permitAll()")
public class TournamentController {

	@Autowired
	private TournamentService tournamentService;
	
	@GetMapping(value="/tournaments")
	public ResponseEntity<List<Tournament>> getAllTournaments() throws InterruptedException, ExecutionException{
		List<Tournament> tournamentLs = tournamentService.getAllTournaments();
		if(tournamentLs.isEmpty()) {
			return new ResponseEntity<List<Tournament>>(tournamentLs, HttpStatus.NO_CONTENT);
		}
		return new ResponseEntity<List<Tournament>>(tournamentLs, HttpStatus.OK);
	}
	
	@GetMapping(value="/tournaments/upcoming")
	public ResponseEntity<List<Tournament>> getAllTournamentsAfterOneWeek() throws InterruptedException, ExecutionException{
		List<Tournament> tournamentsInOneWeek = tournamentService.getAllTournamentsAfterOneWeek();
		if(tournamentsInOneWeek.isEmpty()) {
			return new ResponseEntity<List<Tournament>>(tournamentsInOneWeek, HttpStatus.NO_CONTENT);
		}
		return new ResponseEntity<List<Tournament>>(tournamentsInOneWeek, HttpStatus.OK);
	}
	
	@GetMapping(value = "/tournaments/now")
	public ResponseEntity<List<Tournament>> getAllTournamentsNow() throws InterruptedException, ExecutionException{
		List<Tournament> tournamentsNow = tournamentService.getAllTournamentsBeforeOneWeek();
		if(tournamentsNow.isEmpty()) {
			return new ResponseEntity<List<Tournament>>(tournamentsNow, HttpStatus.NO_CONTENT);
		}
		return new ResponseEntity<List<Tournament>>(tournamentsNow, HttpStatus.OK);
	}
	
	@GetMapping(value = "/tournaments/matches/all")
	public ResponseEntity<List<Match>> getAllActiveTournamentMatches(@RequestParam(required = true)String tournamentId) throws InterruptedException, ExecutionException{
		List<Match> tournamentMatches = tournamentService.getAllTournamentMatches(tournamentId);
		return new ResponseEntity<List<Match>>(tournamentMatches, HttpStatus.OK);
	}
	
	@GetMapping(value = "/tournaments/matches/inactive")
	public ResponseEntity<List<Match>> getAllInactiveTournamentMatches(@RequestParam(required = true)String tournamentId) throws InterruptedException, ExecutionException{
		List<Match> tournamentMatches = tournamentService.getAllTournamentInactiveMatches(tournamentId);
		if(tournamentMatches.isEmpty()) {
			return new ResponseEntity<List<Match>>(tournamentMatches ,HttpStatus.NO_CONTENT);
		}
		return new ResponseEntity<List<Match>>(tournamentMatches, HttpStatus.OK);
	}
	
	@GetMapping(value = "/tournaments/user/matches/active")
	public ResponseEntity<List<Match>> getAllUserActiveMatches(@RequestParam(required = true, value="userId")String userId,
															   @RequestParam(required = true, value="tournamentId")String tournamentId) throws InterruptedException, ExecutionException{
		List<Match> userMatches = tournamentService.getAllUserActiveMatches(userId, tournamentId);
		if(userMatches.isEmpty()) {
			return new ResponseEntity<List<Match>>(userMatches, HttpStatus.NO_CONTENT);
		}
		return new ResponseEntity<List<Match>>(userMatches, HttpStatus.OK);
	}
	
	@GetMapping(value = "/tournaments/search", params="tournamentId")
	public ResponseEntity<Tournament> getTournamentById(@RequestParam(required = true)String tournamentId) throws InterruptedException, ExecutionException, ClassNotFoundException, IOException{
		Optional<Tournament> tournament = tournamentService.getTournamentById(tournamentId);
		if(tournament == null) {
			return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<Tournament>(tournament.get(), HttpStatus.OK);
	}
	
	@GetMapping(value = "/tournaments/search", params="tournamentName")
	public ResponseEntity<Tournament> getTournamentByName(@RequestParam(required = true)String tournamentName) throws InterruptedException, ExecutionException{
		Optional<Tournament> tournament = tournamentService.getTournamentByName(tournamentName);
		if(tournament == null) {
			return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<Tournament>(tournament.get(), HttpStatus.OK);
	}
	
	@GetMapping(value = "/tournaments/teams")
	public ResponseEntity<List<Team>> getAllTeamsOnTournament(@RequestParam(required = true)String tournamentId) throws InterruptedException, ExecutionException{
		List<Team> teams = tournamentService.getTournamentTeams(tournamentId);
		if(teams.isEmpty()) {
			return new ResponseEntity<List<Team>>(teams, HttpStatus.NO_CONTENT);
		}
		return new ResponseEntity<List<Team>>(teams, HttpStatus.OK);
	}
	
	
	@PostMapping(value = "/tournaments/save" )
	@PreAuthorize("hasRole('PLAYER') or hasRole('ADMIN')")
	public ResponseEntity<Tournament> saveTournament(@RequestBody(required = true)TournamentCreationRequest tournament) throws InterruptedException, ExecutionException{
		Tournament response = tournamentService.postTournament(tournament.getTournamentUserModeratorId(), tournament.getTournamentToBeCreated());	
		if(response == null) {
			return new ResponseEntity<Tournament>(response, HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<Tournament>(response, HttpStatus.OK);
	}
	
	@DeleteMapping(value = "/tournaments/delete")
	@PreAuthorize("hasRole('TOURNEY_ADMIN') or hasRole('ADMIN')")
	public ResponseEntity<MessageResponse> deleteTournament(@RequestBody(required = true) Tournament tournament) throws InterruptedException, ExecutionException{
		String response = tournamentService.deleteTournament(tournament);
		if(response.equals("Tournament not found.")) {
			return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.OK);
	}
	
	@DeleteMapping(value = "/tournaments/delete/field")
	@PreAuthorize("hasRole('PLAYER') or hasRole('TOURNEY_ADMIN')")
	public ResponseEntity<MessageResponse> deleteTournamentField(@RequestParam(required = true)String tournamentId,
												   				 @RequestParam(required = true)String tournamentField) throws InterruptedException, ExecutionException{
		String response = tournamentService.deleteTournamentField(tournamentId, tournamentField);
		if(response.equals("Not found.")) {
			return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.OK);
	}
	
	@PutMapping(value = "/tournaments/update")
	@PreAuthorize("hasRole('PLAYER') or hasRole('TOURNEY_ADMIN')")
	public ResponseEntity<MessageResponse> updateTournament(@RequestBody Tournament tournament) throws InterruptedException, ExecutionException{
		String response = tournamentService.updateTournament(tournament);
		if(response.equals("Tournament not found.")) {
			return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.OK);
	}
	 
	@PostMapping(value="/tournaments/start")
	@PreAuthorize("hasRole('ADMIN') or hasRole('TOURNEY_ADMIN')")
	public ResponseEntity<Tournament> startTournament(@RequestBody(required = true)Tournament tournament) throws InterruptedException, ExecutionException, IOException{
		Tournament newTournament = tournamentService.activateTournament(tournament);
		if(newTournament != null) {
			return new ResponseEntity<Tournament>(newTournament, HttpStatus.OK);
		}
		return new ResponseEntity<Tournament>(newTournament, HttpStatus.NOT_FOUND);
	}
}