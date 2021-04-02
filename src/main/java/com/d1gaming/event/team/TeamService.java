package com.d1gaming.event.team;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.d1gaming.event.image.EventImageService;
import com.d1gaming.library.image.ImageModel;
import com.d1gaming.library.role.Role;
import com.d1gaming.library.team.Team;
import com.d1gaming.library.team.TeamInviteRequest;
import com.d1gaming.library.team.TeamInviteRequestStatus;
import com.d1gaming.library.team.TeamStatus;
import com.d1gaming.library.user.User;
import com.d1gaming.library.user.UserStatus;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteBatch;
import com.google.cloud.firestore.WriteResult;

@Service
public class TeamService {

	@Autowired
	private Firestore firestore;
	
	@Autowired
	private EventImageService eventImagesService;
	
	private final String TEAM_COLLECTION = "teams";
	
	private CollectionReference getTeamsCollection() {
		return firestore.collection(this.TEAM_COLLECTION);
	}
	
	private CollectionReference getUsersCollection() {
		return firestore.collection("users");
	}
	
	
	public boolean isActive(String teamId) throws InterruptedException, ExecutionException {
		DocumentReference teamReference = getTeamReference(teamId);
		DocumentSnapshot teamSnapshot = teamReference.get().get();
		if(teamSnapshot.exists() && teamSnapshot.toObject(Team.class).getTeamStatus().equals(TeamStatus.ACTIVE)) {
			return true;
		}
		return false;
	}
	
	public boolean isActiveUser(String userId) throws InterruptedException, ExecutionException {
		DocumentReference userReference = firestore.collection("users").document(userId);
		DocumentSnapshot userSnapshot = userReference.get().get();
		if(userSnapshot.exists() && userSnapshot.toObject(User.class).getUserStatusCode().equals(UserStatus.ACTIVE)) {
			return true;
		}
		return false;
	}
	
	private DocumentReference getTeamReferenceByName(String teamName) throws InterruptedException, ExecutionException {
		QuerySnapshot querySnapshot = getTeamsCollection().whereEqualTo("teamName", teamName).get().get();
		List<Team> teamLs = querySnapshot.toObjects(Team.class);
		DocumentReference teamReference = null;
		for(Team team : teamLs) {
			teamReference = getTeamReference(team.getTeamId());
		}
		return teamReference;
	}
	
	public DocumentReference getTeamReference(String teamId) {
		return getTeamsCollection().document(teamId);
	}
	
	public DocumentReference getUserReference(String userId) {
		return getUsersCollection().document(userId);
	}
	
	//Get a Team by its Id.
	public Optional<Team> getTeamById(String teamId) throws InterruptedException, ExecutionException {
		if(isActive(teamId)) {
			DocumentReference teamReference = getTeamReference(teamId);
			DocumentSnapshot teamSnapshot = teamReference.get().get();
			return Optional.of(teamSnapshot.toObject(Team.class));
		}
		return null;
	}
	
	public Optional<Team> getTeamByName(String teamName) throws InterruptedException, ExecutionException {	
		Query query = getTeamsCollection().whereEqualTo("teamName", teamName);
		QuerySnapshot querySnapshot = query.get().get();
		if(!querySnapshot.isEmpty()) {
			List<Team> teamList = querySnapshot.toObjects(Team.class);
			for(Team currTeam : teamList) {
				return Optional.of(currTeam);
			}
		}
		return null;
	}
	
	public Optional<Team> getTeamByEmail(String teamEmail) throws InterruptedException, ExecutionException{
		Query query = getTeamsCollection().whereEqualTo("teamEmail", teamEmail);
		QuerySnapshot querySnapshot = query.get().get();
		if(!querySnapshot.isEmpty()) {
			List<Team> teamList = querySnapshot.toObjects(Team.class);
			for(Team currTeam : teamList) {
				return Optional.of(currTeam);
			}
		}
		return null;
	}	
		
	//Get all teams available in a collection.
	public List<Team> getAllTeams() throws InterruptedException, ExecutionException{
		ApiFuture<QuerySnapshot> collection = getTeamsCollection().get();
		return collection.get().getDocuments()
				.stream()
				.map(document -> document.toObject(Team.class))
				.collect(Collectors.toList());
	}
	
	public List<User> getAllUsersInTeam(String teamId) throws InterruptedException, ExecutionException{
		if(isActive(teamId)) {
			DocumentReference teamReference = getTeamReference(teamId);
			DocumentSnapshot teamSnapshot = teamReference.get().get();
			return teamSnapshot.toObject(Team.class).getTeamUsers(); 
		}
		return new ArrayList<>();
	}
	
	public Optional<Team> postTeam(Team team, User teamLeader) throws InterruptedException, ExecutionException {
		team.setTeamChallenges(new ArrayList<>());
		team.setTeamTournaments(new ArrayList<>());
		team.setTeamRequests(new ArrayList<>());
		team.setTeamStatus(TeamStatus.ACTIVE);
		team.setTeamLeader(teamLeader);
		team.setTeamUsers(new ArrayList<>());
		addTeamAdminRoleToUser(teamLeader);
		DocumentReference reference = getTeamsCollection().add(team).get();
//		DocumentReference userReference = getUserReference(teamLeader.getUserId());
//		List<Team> userTeamList = teamLeader.getUserTeams();
//		userTeamList.add(team);
//		userReference.update("userTeams", userTeamList);
		String teamId = reference.getId();
		team.setTeamId(teamId);
		WriteBatch batch = firestore.batch();
		batch.update(reference, "teamId", teamId);
		batch.commit().get()
					.stream()
					.forEach(result -> System.out.println("Update Time: " + result.getUpdateTime()));
		addUserToTeam(teamLeader, team);
		return Optional.of(team);
	}
	
	public String addUserToTeam(User user, Team team ) throws InterruptedException, ExecutionException {
		if(isActiveUser(user.getUserId()) && isActive(team.getTeamId())) {
			DocumentReference userReference = getUserReference(user.getUserId());
			DocumentReference teamReference = getTeamReference(team.getTeamId());
			List<User> currTeamUserList = team.getTeamUsers();
			List<Team> currUserTeamList = user.getUserTeams();
			boolean alreadyContainsTeam = currUserTeamList
											.stream()
											.anyMatch(teamInList -> teamInList.getTeamName().equals(team.getTeamName()));
			
			boolean alreadyContainsUser = currTeamUserList
											.stream()
											.anyMatch(userInList -> userInList.getUserName().equals(user.getUserName()));
			if(!alreadyContainsTeam && !alreadyContainsUser) {
				WriteBatch batch = firestore.batch();
				user.setUserTeams(null);
				team.setTeamUsers(null);
				currTeamUserList.add(user);
				currUserTeamList.add(team);
				batch.update(teamReference, "teamUsers", currTeamUserList);
				batch.update(userReference, "userTeams", currUserTeamList);
				batch.commit().get()
						.stream()
						.forEach(result -> System.out.println("Update Time: " + result.getUpdateTime()));
				return "Added User to team.";
			}
			return "Invalid";	
		}
		return "Not found.";
	}
	
	public String postTeamWithImage(Team team, User teamLeader, ImageModel teamImage) throws InterruptedException, ExecutionException {
		team.setTeamChallenges(new ArrayList<>());
		team.setTeamTournaments(new ArrayList<>());
		team.setTeamStatus(TeamStatus.ACTIVE);
		team.setTeamRequests(new ArrayList<>());
		team.setTeamLeader(teamLeader);
		addTeamAdminRoleToUser(teamLeader);
		DocumentReference reference = getTeamsCollection().add(team).get();
		String teamId = reference.getId();
		WriteBatch batch = firestore.batch();
		batch.update(reference, "teamId", teamId);
		batch.commit().get()
			.stream()
			.forEach(result -> System.out.println("Update Time: " + result.getUpdateTime()));
		eventImagesService.saveTeamImage(teamId, teamImage);
		return "Team created.";
	}
	
	//Delete Team by its ID. In reality this method just changes a Team's Status to INACTIVE.
	public String deleteTeamById(Team team) throws InterruptedException, ExecutionException {
		//Evaluate if document exists in collection.
		if(isActive(team.getTeamId())) {
			DocumentReference reference = getTeamReference(team.getTeamId());
			WriteBatch batch = firestore.batch();
			User user = team.getTeamLeader();
			removeTeamAdminRoleFromUser(user);
			//Change teamStatus to Inactive.
			batch.update(reference, "teamStatus", TeamStatus.INACTIVE);
			List<WriteResult> results = batch.commit().get();
			results.forEach(result -> 
				System.out.println("Update Time: " + result.getUpdateTime())
			);
			return "Team with ID: '" + team.getTeamId() + "' was deleted.";
		}
		return "Team not found.";
	}
	
	public String banTeamById(Team team) throws InterruptedException, ExecutionException {
		if(isActive(team.getTeamId())) {
			DocumentReference teamReference = getTeamReference(team.getTeamId());
			User currTeamLeader = team.getTeamLeader();
			removeTeamAdminRoleFromUser(currTeamLeader);
			WriteBatch batch = firestore.batch();
			batch.update(teamReference, "teamStatus", TeamStatus.BANNED);
			List<WriteResult> results = batch.commit().get();
			results.forEach(result -> 
				System.out.println("Update Time: " + result.getUpdateTime()));
			DocumentSnapshot snapshot = teamReference.get().get();
			if(snapshot.toObject(Team.class).getTeamStatus().equals(TeamStatus.BANNED)) {
				return "Team with ID: '" + teamReference.getId() + "' was banned.";
			}
			return "Team could not be banned.";
		}
		return "Team not found.";
	}
	
	//Replace a team's given field by given replaceValue.
	public String deleteTeamField(String teamId, String teamField) throws InterruptedException, ExecutionException {
		//Evaluate if document exists.
		if( isActive(teamId) && teamField != "teamName") {
			DocumentReference reference = getTeamsCollection().document(teamId);
			WriteBatch batch = firestore.batch();
			//Delete given field value.
			batch.update(reference, teamField, FieldValue.delete());
			List<WriteResult> results = batch.commit().get();
			results.forEach(result -> 
				System.out.println("Update Time: " + result.getUpdateTime())
			);
			return "Field deleted successfully.";
		}
		return "Team not found.";
	}
	
	public String updateTeam(Team newTeam) throws InterruptedException, ExecutionException {
		if(isActive(newTeam.getTeamId())) {
			final DocumentReference reference = getTeamReference(newTeam.getTeamId());
			WriteBatch batch = firestore.batch();
			batch.set(reference, newTeam);
			List<WriteResult> results = batch.commit().get();
			results.forEach(result -> 
					System.out.println("Update Time: " + result.getUpdateTime()));
			return "Team updated successfully.";
		}
		return "Team not found.";
	}

	public String updateTeamField(String teamId, String teamField, String replaceValue) throws InterruptedException, ExecutionException {
		String response = "Team not found.";
		if(isActive(teamId)) {
			DocumentReference reference = getTeamsCollection().document(teamId);
			if(!teamField.equals("teamName") && !teamField.equals("teamId")) {
				WriteBatch batch = firestore.batch();
				batch.update(reference, teamField, replaceValue);
				List<WriteResult> results = batch.commit().get();
				results.forEach(result -> 
								System.out.println("Update Time: " + result.getUpdateTime()));
				return "Field updated successfully.";
			}
			else if(teamField.equals("teamName")) {
				response = updateTeamName(teamId, replaceValue);
			}
			else {
				response = "This field cannot be updated.";
			}
		}
		return response;
	}
	
	public String updateTeamName(String teamId, String newTeamName) throws InterruptedException, ExecutionException {
		String response = "Team not found.";
		if(isActive(teamId)) {
			DocumentReference teamReference = getTeamsCollection().document(teamId);
			Query query = getTeamsCollection().whereEqualTo("teamName", newTeamName);
			QuerySnapshot querySnapshot = query.get().get();
			if(querySnapshot.isEmpty()) {
				ApiFuture<String> futureTransaction = firestore.runTransaction(transaction -> {
					DocumentSnapshot snapshot = transaction.get(teamReference).get();
					User teamModerator = (User) snapshot.get("teamModerator");
					DocumentReference userReference = getUserReference(teamModerator.getUserId());
					double tokens = teamModerator.getUserTokens();
					if(tokens >= 100) {
						transaction.update(userReference, "userTokens", FieldValue.increment(-100));
						transaction.update(teamReference, "teamName", newTeamName);
						return "Team name updated to: '" + newTeamName + "'";
					}
					return "Not enough tokens.";
				});
				response = futureTransaction.get();
			}
			return "Name is already in use.";
		}
		return response;
	}
	
	public String sendTeamInvite(TeamInviteRequest request) throws InterruptedException, ExecutionException{
		if(isActiveUser(request.getRequestedUser().getUserId()) && isActive(request.getTeamRequest().getTeamId()))  {
			request.setRequestStatus(TeamInviteRequestStatus.PENDING);
			DocumentReference userReference = firestore.collection("users").document(request.getRequestedUser().getUserId());
			DocumentReference teamReference = getTeamReference(request.getTeamRequest().getTeamId());
			List<TeamInviteRequest> userRequests = userReference.get().get().toObject(User.class).getUserTeamRequests();
			List<TeamInviteRequest> teamRequests = teamReference.get().get().toObject(Team.class).getTeamRequests();
			teamRequests.add(request);
			userRequests.add(request);
			request.setRequestedTime(new Date(System.currentTimeMillis()));
			WriteBatch batch = firestore.batch();
			batch.update(userReference, "userTeamRequests", userRequests);
			batch.update(teamReference, "teamRequests", teamRequests);
			batch.commit().get()
					.stream()
					.forEach(result -> System.out.println("Update Time: " + result.getUpdateTime()));
			return "Invite sent successfully.";
		}
		return "Not found.";
	}
	
	private void addTeamAdminRoleToUser(User user) throws InterruptedException, ExecutionException {
		if(isActiveUser(user.getUserId())) {
			List<Role> currentUserRolesList = user.getUserRoles();
			boolean hasRole = currentUserRolesList
								.stream()
								.anyMatch(role -> role.getAuthority().equals("TEAM_ADMIN"));
			if(!hasRole) {				
				DocumentReference userReference = getUsersCollection().document(user.getUserId());
				WriteBatch batch = firestore.batch();
				currentUserRolesList.add(new Role(Role.TEAM_ADMIN));
				batch.update(userReference, "userRoles", currentUserRolesList);
				batch.commit().get()
				.stream()
				.forEach(result -> System.out.println("Update Time: " + result.getUpdateTime()));;
			}
		}
	}
	
	private void removeTeamAdminRoleFromUser(User user) throws InterruptedException, ExecutionException {
		if(isActiveUser(user.getUserId())) {
			List<Role> currentUserRoles = user.getUserRoles();
			boolean hasRole = currentUserRoles
								.stream()
								.anyMatch(role -> role.getAuthority().equals("TEAM_ADMIN"));
			if(hasRole) {
				DocumentReference userReference = getUsersCollection().document(user.getUserId());
				WriteBatch batch = firestore.batch();
				currentUserRoles = currentUserRoles
									.stream()
									.filter(role -> !role.getAuthority().equals("TEAM_ADMIN"))
									.collect(Collectors.toList());
				batch.update(userReference, "userRoles", currentUserRoles);
				batch.commit().get()
					.stream()
					.forEach(result -> System.out.println("Update Time: " + result.getUpdateTime()));
			}
		}
	}
}	