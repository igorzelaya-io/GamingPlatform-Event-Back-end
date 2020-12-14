package com.d1gaming.event.team;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.d1gaming.library.request.TeamInviteRequest;
import com.d1gaming.library.team.Team;
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

	private final String TEAM_COLLECTION = "teams";
	
	@Autowired
	Firestore firestore;
	
	private CollectionReference getTeamsCollection() {
		return firestore.collection(this.TEAM_COLLECTION);
	}
	
	
	//Get a Team by its Id.
	public Team getTeamById(String teamId) throws InterruptedException, ExecutionException {
		DocumentReference reference = getTeamsCollection().document(teamId);
		//Evaluate if Team exists in collection.
		if(reference.get().get().exists()) {
			DocumentSnapshot snapshot = reference.get().get();
			return snapshot.toObject(Team.class);	
		}
		return null;
	}
	
	public Team getTeamByName(String teamName) throws InterruptedException, ExecutionException {
		DocumentReference reference = getTeamsCollection().document(teamName);
		if(!reference.get().get().exists()) {
			DocumentSnapshot snapshot = reference.get().get();
			return snapshot.toObject(Team.class);
		}
		return null;
	}
	
	//Get all teams available in a collection.
	public List<Team> getAllTeams() throws InterruptedException, ExecutionException{
		ApiFuture<QuerySnapshot> collection = getTeamsCollection().get();
		List<QueryDocumentSnapshot> snapshot = collection.get().getDocuments();
		//If Snapshot contains documents(Teams).
		if(!snapshot.isEmpty()) {
			List<Team> teamLs = new ArrayList<>();
			//Add each Document to Team List.
			snapshot.forEach( document -> {
				teamLs.add(document.toObject(Team.class));
			});
			return teamLs;
		}
		return null;
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
	
	//Delete Team by its ID. In reality this method just changes a Team's Status to INACTIVE.
	public String deleteTeamById(String teamId) throws InterruptedException, ExecutionException {
		DocumentReference reference = getTeamsCollection().document(teamId);
		//Evaluate if document exists in collection.
		if(reference.get().get().exists()) {
			WriteBatch batch = firestore.batch();
			//Change teamStatus to Inactive.
			batch.update(reference, "teamStatus", TeamStatus.INACTIVE);
			List<WriteResult> results = batch.commit().get();
			results.forEach(result -> 
				System.out.println("Update Time: " + result.getUpdateTime())
			);
		}
		//Evaluate if update did actually take place.
		if(reference.get().get().toObject(Team.class).getTeamStatus().equals(TeamStatus.INACTIVE)) {
			return "Team with ID: '" + teamId + "' was deleted.";
		}
		return "Team not found.";
	}
	
	//Replace a team's given field by given replaceValue.
	public String deleteUserField(String teamId, String teamField) throws InterruptedException, ExecutionException {
		DocumentReference reference = getTeamsCollection().document(teamId);
		//Evaluate if document exists.
		if(!reference.get().get().exists() && teamField != "teamName") {
			WriteBatch batch = firestore.batch();
			Map<String,Object> map = new HashMap<>();
			//Delete given field value.
			map.put(teamField, FieldValue.delete());
			batch.update(reference,map);
			List<WriteResult> results = batch.commit().get();
			results.forEach(result -> 
				System.out.println("Update Time: " + result.getUpdateTime())
			);
			//Evaluate if delete changes did actually take place.
			if(reference.get().get().get(teamField) == null) {
				return "Team field deleted successfully";
			}		
		}
		return "Team not found.";
	}

	public String updateTeamField(String teamId, String teamField, String replaceValue) throws InterruptedException, ExecutionException {
		DocumentReference reference = getTeamsCollection().document(teamId);
		if(reference.get().get().exists()) {
			if(teamField.equals("teamName")) {
				ApiFuture<String> futureTransaction = firestore.runTransaction(transaction -> {
					return " ";
				});
			}
		}
		return "Team not found.";
	}
	
	public String sendTeamInvite(TeamInviteRequest request) throws InterruptedException, ExecutionException{
		DocumentReference reference = firestore.collection("users").document(request.getRequestedUser().getUserId());
		if(!reference.get().get().exists()) {
			return "User not found.";
		}
		List<TeamInviteRequest>  userRequests = request.getRequestedUser().getUserTeamRequests();
		userRequests.add(request);
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
	}
}	