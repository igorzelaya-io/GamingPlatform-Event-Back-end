package com.d1gaming.event.image;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.d1gaming.library.image.ImageModel;
import com.d1gaming.library.team.Team;
import com.d1gaming.library.team.TeamStatus;
import com.d1gaming.library.user.User;
import com.d1gaming.library.user.UserStatus;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteBatch;

@Service
public class EventImageService {

	@Autowired
	private Firestore firestore;
	
	private CollectionReference getTeamsCollection() {
		return firestore.collection("teams");
	}
	
	private DocumentReference getTeamReference(String teamId) {
		return getTeamsCollection().document(teamId);
	}
	
	private boolean isActive(String teamId) throws InterruptedException, ExecutionException {
		DocumentReference teamReference = getTeamReference(teamId);
		DocumentSnapshot snapshot = teamReference.get().get();
		if(snapshot.exists() && snapshot.toObject(Team.class).getTeamStatus().equals(TeamStatus.ACTIVE)) {
			return true;
		}
		return false;
	}
	
	public Optional<ImageModel> getTeamImage(String teamId) throws InterruptedException, ExecutionException{
		if(isActive(teamId)) {
			QuerySnapshot teamImageQuery = firestore.collection("teamImages").whereEqualTo("dtoID", teamId).get().get();
			if(!teamImageQuery.isEmpty()) {
				return Optional.of(teamImageQuery.getDocuments().get(0).toObject(ImageModel.class));
			}
		}
		return null;
	}
	
	public String saveTeamImage(ImageModel teamImage) throws InterruptedException, ExecutionException {
		if(isActive(teamImage.getDtoID())){
			Optional<ImageModel> teamImageModel = getTeamImage(teamImage.getDtoID());
			if(teamImageModel != null) {
				firestore.collection("teamImages").document(teamImageModel.get().getImageModelDocumentId()).set(teamImage).get();
				return "Image updated successfully"; 
			}
			DocumentReference teamImageReference = firestore.collection("teamImages").add(teamImage).get();
			DocumentReference teamReference = getTeamReference(teamImage.getDtoID());
			
			String documentId = teamImageReference.getId();
			WriteBatch batch = firestore.batch();
			batch.update(teamImageReference, "imageModelDocumentId", documentId);
			batch.update(teamReference, "hasImage", true);
			batch.commit().get();			
			Team teamOnDB = teamReference.get().get().toObject(Team.class);
			List<User> teamUsers = teamOnDB.getTeamUsers();
			teamUsers.forEach(user -> {
				try {
					addImageToUser(user, teamOnDB);
				} catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();
				}
			});
			return "Image added successfully";
		}		
		return "Not found.";
	}
	
	public void addImageToUser(User user, Team team) throws InterruptedException, ExecutionException {
		DocumentReference userReference = firestore.collection("users").document(user.getUserId());
		DocumentSnapshot userSnapshot = userReference.get().get();
		User userOnDB = userSnapshot.toObject(User.class);
		if(userSnapshot.exists() && user.getUserStatusCode().equals(UserStatus.ACTIVE)) {
			List<Team> userTeams = userOnDB.getUserTeams();
			List<Team> newUserTeamsList = userTeams
											.stream()
											.filter(userTeam -> !userTeam.getTeamId().equals(team.getTeamId()))
											.collect(Collectors.toList());
			newUserTeamsList.add(team);
			WriteBatch batch = firestore.batch();
			batch.update(userReference, "userTeams", newUserTeamsList);
			batch.commit().get();
		}
	}
}
