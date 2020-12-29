package com.d1gaming.event.image;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.d1gaming.library.image.ImageModel;
import com.d1gaming.library.image.ImageUtils;
import com.d1gaming.library.team.Team;
import com.d1gaming.library.team.TeamStatus;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteBatch;
import com.google.cloud.firestore.WriteResult;

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
			DocumentReference reference = getTeamsCollection().document(teamId);
			DocumentSnapshot snapshot = reference.get().get();
			ImageModel compressedImage = snapshot.toObject(Team.class).getTeamImage();
			ImageModel decompressedImage = new ImageModel(compressedImage.getImageName(), compressedImage.getImageType(),
															ImageUtils.decompressBytes(compressedImage.getImageByte()));
			return Optional.of(decompressedImage);
		}
		return null;
	}
	
	public String saveTeamImage(String teamId, ImageModel image) throws InterruptedException, ExecutionException {
		if(isActive(teamId)){
			DocumentReference reference = getTeamsCollection().document(teamId);
			ImageModel compressedImage = new ImageModel(image.getImageName(), image.getImageType(), 
														ImageUtils.compressBytes(image.getImageByte()));
			WriteBatch batch = firestore.batch();
			batch.update(reference, "teamImage", compressedImage);
			List<WriteResult> results = batch.commit().get();
			results.forEach(result -> System.out.println("Update Time: " + result.getUpdateTime()));
			return "Image saved successfully.";
		}		
		return "Team not found.";
	}
}
