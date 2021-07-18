
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

import com.d1gaming.event.image.EventImageService;
import com.d1gaming.library.image.ImageModel;
import com.d1gaming.library.response.MessageResponse;
import com.d1gaming.library.team.Team;
import com.d1gaming.library.team.TeamCreationRequest;
import com.d1gaming.library.team.TeamInviteRequest;
import com.d1gaming.library.user.User;

@RestController
@CrossOrigin(origins = "localhost:4200")
@RequestMapping(value = "/teamsapi")
@PreAuthorize("permitAll()")
public class TeamController {

	@Autowired
	private TeamService teamService;
	
	@Autowired
	private EventImageService imageService;
	
	@GetMapping(value = "/teams/search", params= "teamId")
	public ResponseEntity<Team> getTeamById(@RequestParam(required = true)String teamId) throws InterruptedException, ExecutionException{
		Optional<Team> team = teamService.getTeamById(teamId);
		if(team == null) {
			return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<Team>(team.get(), HttpStatus.OK);
	}

	@GetMapping(value = "/teams/search", params = "teamName")
	public ResponseEntity<Team> getTeamByName(@RequestParam(required = true)String teamName) throws InterruptedException, ExecutionException{
		Optional<Team> team = teamService.getTeamByName(teamName);
		if(team == null) {
			return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<Team>(team.get(), HttpStatus.OK);
	}
	
	
	@GetMapping(value = "/teams")
	public ResponseEntity<List<Team>> getAllTeams() throws InterruptedException, ExecutionException{
		List<Team> teamLs = teamService.getAllTeams();
		if(teamLs.isEmpty()) {
			return new ResponseEntity<List<Team>>(teamLs, HttpStatus.NO_CONTENT);
		}
		return new ResponseEntity<List<Team>>(teamLs, HttpStatus.OK);
	}
	
	@GetMapping(value = "/teams/users")
	public ResponseEntity<List<User>> getAllUsersInTeam(@RequestParam(required = true)String teamId) throws InterruptedException, ExecutionException{
		List<User> usersInTeam = teamService.getAllUsersInTeam(teamId);
		if(usersInTeam.isEmpty()) {
			return new ResponseEntity<List<User>>(usersInTeam, HttpStatus.NO_CONTENT);
		}
		return new ResponseEntity<List<User>>(usersInTeam, HttpStatus.OK);
	}
	
	@GetMapping(value="/teams/image")
	public ResponseEntity<ImageModel> getTeamImage(@RequestParam(required = true)String teamId) throws InterruptedException, ExecutionException{
		Optional<ImageModel> teamImageModel = imageService.getTeamImage(teamId);
		if(teamImageModel.isPresent()) {
			return new ResponseEntity<ImageModel>(teamImageModel.get(), HttpStatus.OK);
		}
		return new ResponseEntity<ImageModel>(teamImageModel.get(), HttpStatus.NO_CONTENT);
	}
	
	@PostMapping(value = "/teams/create")
	@PreAuthorize("hasRole('PLAYER') or hasRole('ADMIN')")
	public ResponseEntity<Object> createTeam(@RequestBody(required = true) TeamCreationRequest team) throws InterruptedException, ExecutionException{	
		if(teamService.getTeamByName(team.getTeamToRegister().getTeamName()) != null){
			return new ResponseEntity<Object>(new MessageResponse("Team name is already in use."), HttpStatus.BAD_REQUEST);
		}
		Optional<Team> response = teamService.postTeam(team.getTeamToRegister(), team.getTeamModerator());			
		return new ResponseEntity<Object>(response.get(), HttpStatus.OK);
	}
	
	@PostMapping(value = "/teams/create/image")
	@PreAuthorize("hasRole('PLAYER') or hasRole('ADMIN')")
	public ResponseEntity<MessageResponse> addImageToTeam(@RequestBody(required = true)ImageModel teamImageModel) throws InterruptedException, ExecutionException, IOException{
		String response = imageService.saveTeamImage(teamImageModel);
		if(response.equals("Team not found.")) {
			return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.OK);
	}
	
	@PostMapping(value = "/teams/users/add")
	@PreAuthorize("hasRole('TEAM_ADMIN') or hasRole('ADMIN')")
	public ResponseEntity<MessageResponse> addUserToTeam(@RequestBody(required = true) TeamCreationRequest teamCreationRequest) throws InterruptedException, ExecutionException{
		String response = teamService.addUserToTeam(teamCreationRequest.getTeamModerator(), teamCreationRequest.getTeamToRegister());
		if(response.equals("Not found.")) {
			return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.NOT_FOUND);
		}
		else if(response.equals("Invalid")) {
			return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.BAD_REQUEST);
		}
		return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.OK);
	}
	
	@PostMapping(value = "/teams/invite")
	@PreAuthorize("hasRole('TEAM_ADMIN') or hasRole('ADMIN')")
	public ResponseEntity<MessageResponse> sendTeamInvite(@RequestBody TeamInviteRequest teamInvite) throws InterruptedException, ExecutionException{
		String response = teamService.sendTeamInvite(teamInvite);
		if(response.equals("User not found.")) {
			return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.NOT_FOUND);
		}
		else if(response.equals("User is already part of team.")){			
			return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.BAD_REQUEST);
		}
		return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.OK);
	}
	
	@DeleteMapping(value = "/teams/delete")
	@PreAuthorize("hasRole('TEAM_ADMIN') or hasRole('ADMIN')")
	public ResponseEntity<MessageResponse> deleteTeam(@RequestBody(required = true)Team team) throws InterruptedException, ExecutionException{
		String response = teamService.deleteTeamById(team);
		if(response.equals("Team not found.")){
			return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.NOT_FOUND);
		}
		else if(response.equals("Team could not be deleted.")) {
			return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.EXPECTATION_FAILED);
		}
		else {
			return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.OK);
		}
	}
	
	@DeleteMapping(value = "/teams/ban")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<MessageResponse> banTeam(@RequestBody(required = true)Team team) throws InterruptedException, ExecutionException{
		String response = teamService.banTeamById(team);
		if(response.equals("Team not found.")) {
			return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.NOT_FOUND);
		}
		else if(response.equals("Team could not be banned.")) {
			return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.EXPECTATION_FAILED);
		}
		else {
			return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.OK);
		}
	}
	
	@DeleteMapping(value= "/teams/update")
	@PreAuthorize("hasRole('ADMIN') or hasRole('TEAM_ADMIN')")
	public ResponseEntity<?> deleteTeamField(@RequestParam(required = true)String teamId,
											 @RequestParam(required = true)String teamField) throws InterruptedException, ExecutionException{
		String response = teamService.deleteTeamField(teamId, teamField);
		if(response.equals("Team not found.")) {
			return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.NOT_FOUND);
		}
		else if(response.equals("Delete failed.")) {
			return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.EXPECTATION_FAILED);
		}
		else {
			return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.OK);
		}
	}
	
	@PutMapping(value = "/teams/update")
	@PreAuthorize("hasRole('ADMIN') or hasRole('TEAM_ADMIN')")
	public ResponseEntity<MessageResponse> updateTeam(@RequestBody Team newTeam) throws InterruptedException, ExecutionException{
		String response = teamService.updateTeam(newTeam);
		if(response.equals("Not found.")) {
			return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.OK);
	}
	
	@PutMapping(value = "/teams/update/field")
	@PreAuthorize("hasRole('PLAYER') or hasRole('TEAM_ADMIN')")
	public ResponseEntity<MessageResponse> updateTeamField(@RequestParam(required = true)String teamId,
											 @RequestParam(required = true)String teamField,
											 @RequestParam(required = true)String replaceValue) throws InterruptedException, ExecutionException{
		String response = teamService.updateTeamField(teamId, teamField, replaceValue);
		switch(response) {
		case "Team not found.":
			return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.NOT_FOUND);
		case "This field cannot be updated.":
			return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.BAD_REQUEST);
		case "Not enough tokens.":
			return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.BAD_REQUEST);
		case "Name is already in use.":
			return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.BAD_REQUEST);
		default:
			return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.OK);
		}
	}
}