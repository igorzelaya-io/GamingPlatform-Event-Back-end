package com.d1gaming.event.team;

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

import com.d1gaming.library.team.Team;
import com.d1gaming.library.team.TeamInviteRequest;

@RestController
@CrossOrigin(origins = "localhost:4200")
@RequestMapping(value = "teamsapi")
public class TeamController {

	@Autowired
	private TeamService teamService;
	
	@GetMapping(value = "/teams/search", params = "teamName")
	public ResponseEntity<?> getTeamByName(@RequestParam(required = true)String teamName) throws InterruptedException, ExecutionException{
		Team team = teamService.getTeamByName(teamName);
		if(team == null) {
			return new ResponseEntity<>(team, HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<>(team, HttpStatus.OK);
	}
	
	@GetMapping(value = "/teams/search", params= "teamId")
	public ResponseEntity<?> getTeamById(@RequestParam(required = true)String teamId) throws InterruptedException, ExecutionException{
		Team team = teamService.getTeamById(teamId);
		if(team == null) {
			return new ResponseEntity<>(team, HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<>(team, HttpStatus.OK);
	}
	
	@GetMapping(value = "/teams")
	public ResponseEntity<?> getAllTeams() throws InterruptedException, ExecutionException{
		List<Team> teamLs = teamService.getAllTeams();
		if(teamLs.isEmpty()) {
			return new ResponseEntity<>(teamLs, HttpStatus.NO_CONTENT);
		}
		return new ResponseEntity<>(teamLs, HttpStatus.OK);
	}
	
	@PostMapping(value = "/teams/create")
	public ResponseEntity<?> createTeam(@RequestBody Team team) throws InterruptedException, ExecutionException{
		String response = teamService.postTeam(team);
		if(response.equals("Team could not be created.")) {
			return new ResponseEntity<>(response, HttpStatus.EXPECTATION_FAILED);
		}
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	@PostMapping(value = "/teams/invite")
	public ResponseEntity<?> sendTeamInvite(@RequestBody TeamInviteRequest teamInvite) throws InterruptedException, ExecutionException{
		String response = teamService.sendTeamInvite(teamInvite);
		if(response.equals("User not found.")) {
			return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
		}
		else if(response.equals("Invite could not be sent.")) {
			return new ResponseEntity<>(response, HttpStatus.EXPECTATION_FAILED);
		}
		else {			
			return new ResponseEntity<>(response, HttpStatus.OK);
		}
	}
	
	@DeleteMapping(value = "/teams/delete")
	public ResponseEntity<?> deleteTeam(@RequestParam(required = true)String teamId) throws InterruptedException, ExecutionException{
		String response = teamService.deleteTeamById(teamId);
		if(response.equals("Team not found.")){
			return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
		}
		else if(response.equals("Team could not be deleted.")) {
			return new ResponseEntity<>(response, HttpStatus.EXPECTATION_FAILED);
		}
		else {
			return new ResponseEntity<>(response, HttpStatus.OK);
		}
	}
	
	@DeleteMapping(value = "/teams/ban")
	public ResponseEntity<?> banTeam(@RequestParam(required = true)String teamId) throws InterruptedException, ExecutionException{
		String response = teamService.banTeamById(teamId);
		if(response.equals("Team not found.")) {
			return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
		}
		else if(response.equals("Team could not be banned.")) {
			return new ResponseEntity<>(response, HttpStatus.EXPECTATION_FAILED);
		}
		else {
			return new ResponseEntity<>(response, HttpStatus.OK);
		}
	}
	
	@DeleteMapping(value= "/teams/update")
	public ResponseEntity<?> deleteTeamField(@RequestParam(required = true)String teamId,
											 @RequestParam(required = true)String teamField) throws InterruptedException, ExecutionException{
		String response = teamService.deleteUserField(teamId, teamField);
		if(response.equals("Team not found.")) {
			return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
		}
		else if(response.equals("Delete failed.")) {
			return new ResponseEntity<>(response, HttpStatus.EXPECTATION_FAILED);
		}
		else {
			return new ResponseEntity<>(response, HttpStatus.OK);
		}
	}
	
	@PutMapping(value = "/teams/update")
	public ResponseEntity<?> updateTeam(@RequestBody Team newTeam) throws InterruptedException, ExecutionException{
		String response = teamService.updateTeam(newTeam);
		if(response.equals("Not found.")) {
			return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	@PutMapping(value = "/teams/update")
	public ResponseEntity<?> updateTeamField(@RequestParam(required = true)String teamId,
											 @RequestParam(required = true)String teamField,
											 @RequestParam(required = true)String replaceValue) throws InterruptedException, ExecutionException{
		String response = teamService.updateTeamField(teamId, teamField, replaceValue);
		switch(response) {
		case "Team not found.":
			return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
		case "This field cannot be updated.":
			return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
		case "Not enough tokens.":
			return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
		case "Name is already in use.":
			return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
		default:
			return new ResponseEntity<>(response, HttpStatus.OK);
		}
	}
}