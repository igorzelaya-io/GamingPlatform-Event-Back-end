package com.d1gaming.event.tournament;

import java.util.List;
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
	
	@PostMapping(value = "tournaments/save" )
	public ResponseEntity<?> saveTournament(@RequestParam(required = true)String userId, 
											@RequestBody Tournament tournament, User user) throws InterruptedException, ExecutionException{
		String response = tournamentService.postTournament(userId, tournament);	
		if(response.equals("User not found.")) {
			return new ResponseEntity<>("Faile", HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<>(response, HttpStatus.OK);
	} 
	
	@DeleteMapping(value = "tournaments/delete")
	public ResponseEntity<?> updateTournament(@RequestParam(required = true) String tournamentId) throws InterruptedException, ExecutionException{
		String response = tournamentService.deleteTournament(tournamentId);
		if(response.equals("Tournament not found.")) {
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
	
}