package com.d1gaming.event.tournament;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;

import com.d1gaming.library.role.ERole;
import com.d1gaming.library.role.Role;
import com.d1gaming.library.tournament.Tournament;
import com.d1gaming.library.tournament.TournamentStatus;
import com.d1gaming.library.user.User;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteBatch;
import com.google.cloud.firestore.WriteResult;

public class TournamentService {

	private final String TOURNAMENTS_COLLECTION = "tournaments";
	
	@Autowired
	private Firestore firestore;
	
	private CollectionReference getTournamentsCollection() {
		return firestore.collection(this.TOURNAMENTS_COLLECTION);
	}
	
	public Optional<Tournament> getTournamentById(String tournamentId) throws InterruptedException, ExecutionException {
		DocumentReference reference = getTournamentsCollection().document(tournamentId);
		if(!reference.get().get().exists()) {
			return null;
		}
		DocumentSnapshot snapshot = reference.get().get();
		return Optional.of(snapshot.toObject(Tournament.class));
	}
	
	//Get all documents from a collection.
	public List<Tournament> getAllTournaments() throws InterruptedException, ExecutionException{
		//asynchronously retrieve all documents
		ApiFuture<QuerySnapshot> future = getTournamentsCollection().get();
		// future.get() blocks on response
		List<QueryDocumentSnapshot> documents = future.get().getDocuments();
		List<Tournament> tournamentLs = new ArrayList<>();
		documents.forEach(document -> {
			tournamentLs.add(document.toObject(Tournament.class));
		});
		return tournamentLs;
	}
	
	//Get a document By its ID.
	public Optional<Tournament> getTournamentByName(String tournamentName) throws InterruptedException, ExecutionException {
		ApiFuture<QuerySnapshot> snapshot = getTournamentsCollection().whereEqualTo("tournamentName", tournamentName).get();
		if(!snapshot.get().isEmpty()) {
			List<QueryDocumentSnapshot> documents = snapshot.get().getDocuments();
			for(DocumentSnapshot doc : documents) {
				return Optional.of(doc.toObject(Tournament.class));
			}
		}
		return null;
	}
	
	public String postTournament(User user, Tournament tournament) throws InterruptedException, ExecutionException {
		DocumentReference reference = getTournamentsCollection().add(tournament).get();
		String documentId = reference.getId();
		DocumentSnapshot snapshot = reference.get().get();
		WriteBatch batch = firestore.batch();
		batch.update(reference, "tournamentId", documentId);
		addModeratorRoleToUser(user);
		List<WriteResult> results = batch.commit().get();
		results.forEach(result -> {
			System.out.println("Update Time: " + result.getUpdateTime());
		});
		if(!snapshot.exists()) {
			return "Tournament could not be created";
		}
		return "Tournament with ID: '" + documentId + "' was created succesfully";
	}
	
	public String deleteTournament(String tournamentId) throws InterruptedException, ExecutionException {
		DocumentReference reference = getTournamentsCollection().document(tournamentId);
		if(!reference.get().get().exists()) {
			return "Tournament not found.";
		}
		WriteBatch batch = firestore.batch();
		batch.update(reference, "tournamentStatus", TournamentStatus.INACTIVE);
		List<WriteResult> results = batch.commit().get();
		results.forEach(result -> {
			System.out.println("Update Time: " + result.getUpdateTime());
		});
		DocumentSnapshot snapshot = reference.get().get();
		if(snapshot.toObject(Tournament.class).getTournamentStatus().equals(TournamentStatus.INACTIVE)) {
			return "Tournament with ID: '" + reference.getId() + "' was deleted.";
		}
		return "Tournament could not be deleted.";
	}
	
	private void addModeratorRoleToUser(User user) throws InterruptedException, ExecutionException {
		DocumentReference userReference = firestore.collection("users").document(user.getUserId());
		if(!userReference.get().get().exists()) {
			return;
		}
		WriteBatch batch = firestore.batch();
		List<Role> userRoleLs = user.getUserRoles();
		Role role = new Role("TourneyModerator", ERole.ROLE_TOURNEY_MODERATOR);
		if(userRoleLs.contains(role)) {
			return;
		}
		userRoleLs.add(role);
		batch.update(userReference, "userRoles", userRoleLs);
		List<WriteResult> results = batch.commit().get();
		results.forEach(result -> System.out.println("Update Time: " + result.getUpdateTime()));
	}
	
}