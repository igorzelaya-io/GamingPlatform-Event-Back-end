package com.d1gaming.event.team;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.d1gaming.event.image.EventImageService;
import com.d1gaming.library.image.ImageModel;
import com.d1gaming.library.team.Team;
import com.d1gaming.library.team.TeamInviteRequest;
import com.d1gaming.library.team.TeamStatus;
import com.d1gaming.library.user.User;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
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
	
	
	public boolean isActive(String teamId) throws InterruptedException, ExecutionException {
		DocumentReference teamReference = getTeamReference(teamId);
		DocumentSnapshot teamSnapshot = teamReference.get().get();
		if(teamSnapshot.exists() && teamSnapshot.toObject(Team.class).getTeamStatus().equals(TeamStatus.ACTIVE)) {
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
		DocumentReference teamReference = getTeamReferenceByName(teamName);		
		if(isActive(teamReference.getId())) {
			DocumentSnapshot teamSnapshot = teamReference.get().get();
			return Optional.of(teamSnapshot.toObject(Team.class));
		}
		return null;
	}
	
	//Get all teams available in a collection.
	public List<Team> getAllTeams() throws InterruptedException, ExecutionException{
		ApiFuture<QuerySnapshot> collection = getTeamsCollection().get();
		List<QueryDocumentSnapshot> snapshot = collection.get().getDocuments();
		List<Team> teamLs = new ArrayList<>();
		//If Snapshot contains documents(Teams).
		if(!snapshot.isEmpty()) {
			//Add each Document to Team List.
			snapshot.forEach( document -> {
				teamLs.add(document.toObject(Team.class));
			});
		}
		return teamLs;
	}
	
	public String postTeam(Team team) throws InterruptedException, ExecutionException {
		DocumentReference reference = getTeamsCollection().add(team).get();
		String teamId = reference.getId();
		DocumentSnapshot snapshot = reference.get().get();
		snapshot.toObject(Team.class).setTeamId(teamId);
		if(snapshot.exists()) {
			return "Team with ID: '" + teamId  + "' has been created.";
		}
		return "Team could not be created.";
	}
	
	public String postTeamWithImage(Team team, ImageModel teamImage) throws InterruptedException, ExecutionException {
		DocumentReference reference = getTeamsCollection().add(team).get();
		String teamId = reference.getId();
		DocumentSnapshot snapshot = reference.get().get();
		snapshot.toObject(Team.class).setTeamId(teamId);
		eventImagesService.saveTeamImage(teamId, teamImage);
		if(snapshot.exists()) {
			return "Team with ID: '" +teamId + "' was created.";
		}
		return "Team could not be created.";
	}
	
	//Delete Team by its ID. In reality this method just changes a Team's Status to INACTIVE.
	public String deleteTeamById(String teamId) throws InterruptedException, ExecutionException {
		//Evaluate if document exists in collection.
		if(isActive(teamId)) {
			DocumentReference reference = getTeamReference(teamId);
			WriteBatch batch = firestore.batch();
			//Change teamStatus to Inactive.
			batch.update(reference, "teamStatus", TeamStatus.INACTIVE);
			List<WriteResult> results = batch.commit().get();
			results.forEach(result -> 
				System.out.println("Update Time: " + result.getUpdateTime())
			);
			//Evaluate if update did actually take place.
			DocumentSnapshot snapshot = reference.get().get();
			if(snapshot.toObject(Team.class).getTeamStatus().equals(TeamStatus.INACTIVE)) {
				return "Team with ID: '" + teamId + "' was deleted.";
			}
			return "Team could not be deleted.";
		}
		return "Team not found.";
	}
	
	public String banTeamById(String teamId) throws InterruptedException, ExecutionException {
		if(isActive(teamId)) {
			DocumentReference teamReference = getTeamReference(teamId);
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
	public String deleteUserField(String teamId, String teamField) throws InterruptedException, ExecutionException {
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
			//Evaluate if delete changes did actually take place.
			if(reference.get().get().get(teamField) == null) {
				return "Team field deleted successfully";
			}		
			return "Delete failed.";
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
					//DocumentReference userReference = userService.getUserReference(teamModerator.getUserId());
					double tokens = teamModerator.getUserTokens();
					if(tokens >= 100) {
					//	transaction.update(userReference, "userTokens", FieldValue.increment(-100));
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
		//if(userService.getUserReference(request.getRequestedUser().getUserId()) != null && isActive(request.getTeamRequest().getTeamId()))  {
			List<TeamInviteRequest>  userRequests = request.getRequestedUser().getUserTeamRequests();
			DocumentReference reference = firestore.collection("users").document(request.getRequestedUser().getUserId());
			userRequests.add(request);
			request.setRequestedTime(new Date(System.currentTimeMillis()));
			WriteBatch batch = firestore.batch();
			batch.update(reference, "userTeamRequests", userRequests);
			List<WriteResult> results = batch.commit().get();
			results.forEach(result -> 
				System.out.println("Update Time: " + result.getUpdateTime())
			);
			User user = request.getRequestedUser();
			if(user.getUserTeamRequests().contains(request)) {
				return "Invite sent successfully.";
			}
			return "Invite could not be sent.";
		//}
		//return "Not found.";
	}
}	