package com.d1gaming.event.team;

import java.util.ArrayList;
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
import com.d1gaming.library.team.TeamCodTournament;
import com.d1gaming.library.team.TeamFifaTournament;
import com.d1gaming.library.team.TeamInviteRequest;
import com.d1gaming.library.team.TeamInviteRequestStatus;
import com.d1gaming.library.team.TeamStatus;
import com.d1gaming.library.tournament.Tournament;
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
		team.setTeamRequests(new ArrayList<>());
		team.setTeamStatus(TeamStatus.ACTIVE);
		team.setTeamUsers(new ArrayList<>());
		addTeamAdminRoleToUser(teamLeader);
		DocumentReference reference = getTeamsCollection().add(team).get();
		String teamId = reference.getId();
		team.setTeamId(teamId);
		WriteBatch batch = firestore.batch();
		batch.update(reference, "teamId", teamId);
		batch.commit().get();
		addUserToTeam(teamLeader, team);
		return Optional.of(team);
	}
	
	public String addUserToTeam(User user, Team team ) throws InterruptedException, ExecutionException {
		if(isActiveUser(user.getUserId()) && isActive(team.getTeamId())) {
			DocumentReference userReference = getUserReference(user.getUserId());
			DocumentReference teamReference = getTeamReference(team.getTeamId());
			User userOnDB = userReference.get().get().toObject(User.class);
			List<User> currTeamUserList = team.getTeamUsers();
			List<Team> currUserTeamList = userOnDB.getUserTeams();
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
				batch.commit().get();
				return "Added User to team.";
			}
			return "Invalid";	
		}
		return "Not found.";
	}
	
	//Delete Team by its ID. In reality this method just changes a Team's Status to INACTIVE.
	public String deleteTeamById(Team team) throws InterruptedException, ExecutionException {
		//Evaluate if document exists in collection.
		if(isActive(team.getTeamId())) {
			DocumentReference reference = getTeamReference(team.getTeamId());
			User user = team.getTeamModerator();
			CollectionReference teamCodTournaments = reference.collection("teamCodTournaments");
			if(!teamCodTournaments.get().get().isEmpty()) {
				List<Tournament> teamTournaments = teamCodTournaments.get().get()
													.getDocuments()
													.stream()
													.map(document -> document.toObject(TeamCodTournament.class))
													.map(teamCodTournament -> teamCodTournament.getTeamCodTournament())
													.collect(Collectors.toList());
													
				deleteTeamFromAllTournaments(team.getTeamId(), teamTournaments);
			}
			CollectionReference teamFifaTournaments = reference.collection("teamFifaTournaments");
			if(!teamFifaTournaments.get().get().isEmpty()) {
				List<Tournament> teamTournaments = teamFifaTournaments.get().get()
						.getDocuments()
						.stream()
						.map(document -> document.toObject(TeamFifaTournament.class))
						.map(teamFifaTournament -> teamFifaTournament.getTeamTournament())
						.collect(Collectors.toList());
						
				deleteTeamFromAllTournaments(team.getTeamId(), teamTournaments);
			}
			removeTeamAdminRoleFromUser(user);
			deleteTeamFromAllUsers(team);
			if(team.ishasImage()) {
				deleteTeamImage(team.getTeamId());
			}
			reference.delete();
			return new StringBuilder("Team with ID: '").append(team.getTeamId()).append("' was deleted.").toString();
		}
		return "Team not found.";
	}
	
	public void deleteTeamImage(String teamId) throws InterruptedException, ExecutionException {
		Optional<ImageModel> teamImageModel = eventImagesService.getTeamImage(teamId);
		if(!teamImageModel.isEmpty()) {
			firestore.collection("teamImages").document(teamImageModel.get().getImageModelDocumentId()).delete();
		}
	}
	
	private void deleteTeamFromAllUsers(Team team) {
		List<User> teamUsers = team.getTeamUsers();
		teamUsers.stream()
				.forEach(user -> {
					try {
						deleteTeamFromUser(user.getUserId(), team.getTeamId());
					} catch (InterruptedException | ExecutionException e) {
						e.printStackTrace();
					}
				});
	}
	
	private void deleteTeamFromUser(String userId, String teamId) throws InterruptedException, ExecutionException {
		if(isActiveUser(userId)) {
			DocumentReference userReference = getUserReference(userId);
			DocumentReference teamReference = getTeamReference(teamId);
			Team teamOnDB = teamReference.get().get().toObject(Team.class);
			User userOnDB = userReference.get().get().toObject(User.class);
			boolean isPartOfTeam = teamOnDB.getTeamUsers()
											.stream()
											.anyMatch(teamUser -> teamUser.getUserId().equals(userId));
			if(isPartOfTeam) {
				List<Team> userTeamNewList = userOnDB.getUserTeams()
											.stream()
											.filter(team -> !team.getTeamId().equals(teamId))
											.collect(Collectors.toList());
				WriteBatch batch = firestore.batch(); 
				batch.update(userReference, "userTeams", userTeamNewList);
				batch.commit().get();
				
			}				
		}
	}
	private void deleteTeamFromAllTournaments(String teamId, List<Tournament> teamTournamentList) {
		teamTournamentList.stream()
					      .forEach(tournament -> {
							try {
								deleteTeamFromTournament(teamId, tournament.getTournamentId());
							} catch (InterruptedException | ExecutionException e) {
								e.printStackTrace();
							}
						});
	}
	
	
	
	private void deleteTeamFromTournament(String teamId, String tournamentId) throws InterruptedException, ExecutionException {
		DocumentReference tournamentReference = this.firestore.collection("tournaments").document(tournamentId);
		Tournament tournamentOnDB = tournamentReference.get().get().toObject(Tournament.class);
		boolean isPartOfTournament = tournamentOnDB.getTournamentTeams()
												.stream()
												.anyMatch(team -> team.getTeamId().equals(teamId));	
		if(isPartOfTournament) {
			List<Team> newTournamentTeamList = tournamentOnDB.getTournamentTeams()
												.stream()
												.filter(team -> !team.getTeamId().equals(teamId))
												.collect(Collectors.toList());
			WriteBatch batch = firestore.batch();
			batch.update(tournamentReference, "tournamentTeams", newTournamentTeamList);
			batch.commit().get();
		}
	}
	
	public String banTeamById(Team team) throws InterruptedException, ExecutionException {
		if(isActive(team.getTeamId())) {
			DocumentReference teamReference = getTeamReference(team.getTeamId());
			User currTeamLeader = team.getTeamModerator();
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
			Team teamOnDB = teamReference.get().get().toObject(Team.class);
			List<User> usersOnTeam = teamOnDB.getTeamUsers();
			boolean isPartOfTeam = usersOnTeam
										.stream()
										.anyMatch(user -> user.getUserId().equals(request.getRequestedUser().getUserId()));
			if(!isPartOfTeam) {				
				List<TeamInviteRequest> userRequests = userReference.get().get().toObject(User.class).getUserTeamRequests();
				List<TeamInviteRequest> teamRequests = teamReference.get().get().toObject(Team.class).getTeamRequests();
				
				teamRequests.add(request);
				userRequests.add(request);
				request.setRequestedTime(new Date(System.currentTimeMillis()));
				WriteBatch batch = firestore.batch();
				batch.update(userReference, "userTeamRequests", userRequests);
				batch.update(teamReference, "teamRequests", teamRequests);
				batch.commit().get();
				return "Invite sent successfully.";
			}
			return "User is already part of team.";
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