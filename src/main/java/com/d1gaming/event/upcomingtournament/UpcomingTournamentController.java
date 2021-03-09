package com.d1gaming.event.upcomingtournament;

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
@CrossOrigin(origins="localhost:4200")
@PreAuthorize("permitAll()")
@RequestMapping("upcomingtournamentsapi")
public class UpcomingTournamentController {
	
	@Autowired
	private UpcomingTournamentService upcomingTournamentService;
	
	@GetMapping(value="/upcomingTournaments/search")
	public ResponseEntity<?> getUpcomingTournamentByID(@RequestParam(required = true)String tournamentId) throws InterruptedException, ExecutionException{
		Optional<Tournament> tournament = upcomingTournamentService.getTournamentById(tournamentId);
		if(tournament != null) {
			return new ResponseEntity<>(tournament.get(), HttpStatus.OK);
		}
		return new ResponseEntity<>("Tournament not found.", HttpStatus.NOT_FOUND);
	}
	
	@GetMapping(value="/upcomingTournaments/search", params="tournamentName")
	public ResponseEntity<?> getUpcomingTournamentByName(@RequestParam(required = true)String tournamentName) throws InterruptedException, ExecutionException{
		Optional<Tournament> tournament = upcomingTournamentService.getTournamentByName(tournamentName);
		if(tournament != null) {
			return new ResponseEntity<>(tournament.get(), HttpStatus.OK);
		}
		return new ResponseEntity<>("Tournament not found.", HttpStatus.NOT_FOUND);
	}
	
	@GetMapping(value="/upcomingTournaments")
	public ResponseEntity<?> getAllUpcomingTournaments() throws InterruptedException, ExecutionException{
		List<Tournament> upcomingTournamentsList = upcomingTournamentService.getAllUpcomingTournaments();
		if(upcomingTournamentsList.isEmpty()) {
			return new ResponseEntity<>(upcomingTournamentsList, HttpStatus.NO_CONTENT);
		}
		return new ResponseEntity<>(upcomingTournamentsList, HttpStatus.OK);
	}
	
	@GetMapping(value="/upcomingTournaments/teams")
	public ResponseEntity<?> getAllTeamsOnTournament(@RequestParam(required = true)String tournamentId) throws InterruptedException, ExecutionException{
		List<Team> tournamentTeamList = upcomingTournamentService.getTournamentTeams(tournamentId);
		if(tournamentTeamList.isEmpty()) {
			return new ResponseEntity<>(tournamentTeamList, HttpStatus.NO_CONTENT);
		}
		return new ResponseEntity<>(tournamentTeamList, HttpStatus.OK);
	}
	
	@PostMapping(value="/upcomingTournaments/create")
	public ResponseEntity<?> postTournament(@RequestBody(required = true)TournamentCreationRequest tournamentToPost) throws InterruptedException, ExecutionException{
		String response = upcomingTournamentService.postTournament(tournamentToPost.getTournamentUserModerator(), tournamentToPost.getTournamentToBeCreated());
		if(response.equals("Not found.")) {
			return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	@DeleteMapping("/upcomingTournaments/delete")
	public ResponseEntity<?> deleteTournament(@RequestBody(required = true)Tournament tournament) throws InterruptedException, ExecutionException{
		String response = upcomingTournamentService.deleteTournament(tournament);
		if(response.equals("Tournament not found.")) {
			return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	@PutMapping("/upcomingTournaments/update")
	public ResponseEntity<?> updateTournament(@RequestBody(required = true)Tournament tournament) throws InterruptedException, ExecutionException{
		String response = upcomingTournamentService.updateTournament(tournament);
		if(response.equals("Tournament not found.")) {
			return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
}
