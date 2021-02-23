package com.d1gaming.event.team;

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
import org.springframework.web.multipart.MultipartFile;

import com.d1gaming.library.image.ImageModel;
import com.d1gaming.library.team.Team;
import com.d1gaming.library.team.TeamInviteRequest;

@RestController
@CrossOrigin(origins = "localhost:4200")
@RequestMapping(value = "/teamsapi")
@PreAuthorize("permitAll()")
public class TeamController {

	@Autowired
	private TeamService teamService;
	
	@GetMapping(value = "/teams/search", params= "teamId")
	public ResponseEntity<?> getTeamById(@RequestParam(required = true)String teamId) throws InterruptedException, ExecutionException{
		Optional<Team> team = teamService.getTeamById(teamId);
		if(team == null) {
			return new ResponseEntity<>(team, HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<>(team.get(), HttpStatus.OK);
	}

	@GetMapping(value = "/teams/search", params = "teamName")
	public ResponseEntity<?> getTeamByName(@RequestParam(required = true)String teamName) throws InterruptedException, ExecutionException{
		Optional<Team> team = teamService.getTeamByName(teamName);
		if(team == null) {
			return new ResponseEntity<>(team, HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<>(team.get(), HttpStatus.OK);
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
	@PreAuthorize("hasRole('PLAYER') or hasRole('ADMIN')")
	public ResponseEntity<?> createTeam(@RequestBody(required = true) Team team,
										@RequestBody(required = false) MultipartFile file) throws InterruptedException, ExecutionException, IOException{	
		if(file != null) {
			ImageModel teamImage = new ImageModel(file.getOriginalFilename(), file.getContentType(),
													file.getBytes());
			String response = teamService.postTeamWithImage(team, teamImage);
			return new ResponseEntity<>(response, HttpStatus.OK);
		}
		String response = teamService.postTeam(team);			
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	@PostMapping(value = "/teams/invite")
	@PreAuthorize("hasRole('PLAYER') or hasRole('ADMIN')")
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
	@PreAuthorize("hasRole('PLAYER') or hasRole('ADMIN')")
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
	@PreAuthorize("hasRole('PLAYER') or hasRole('ADMIN')")
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
	@PreAuthorize("hasRole('ADMIN') or hasRole('PLAYER')")
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
	
	@PutMapping(value = "/teams/update/field")
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