package com.d1gaming.event.upcomingtournament;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.d1gaming.library.role.Role;
import com.d1gaming.library.team.Team;
import com.d1gaming.library.tournament.Tournament;
import com.d1gaming.library.tournament.TournamentStatus;
import com.d1gaming.library.user.User;
import com.d1gaming.library.user.UserStatus;
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
public class UpcomingTournamentService {

	@Autowired
	private Firestore firestore;
	
	private final String UPCOMING_TOURNAMENTS_COLLECTION = "upcoming_tournaments";
	
	
	private CollectionReference getUpcomingTournamentsCollection() {
		return firestore.collection(UPCOMING_TOURNAMENTS_COLLECTION);
	}
	
	private DocumentReference getUpcomingTournamentReference(String upcomingTournamentId) {
		return getUpcomingTournamentsCollection().document(upcomingTournamentId);
	}
	
	private DocumentReference getUserReference(String userId) {
		return getUpcomingTournamentsCollection().document(userId);
	}

	private boolean isActiveUpcomingTournament(String upcomingTournamentId) throws InterruptedException, ExecutionException {
		DocumentReference tournamentReference = getUpcomingTournamentReference(upcomingTournamentId);
		DocumentSnapshot tournamentSnapshot = tournamentReference.get().get();
		if(tournamentSnapshot.exists() && tournamentSnapshot.toObject(Tournament.class).getTournamentStatus().equals(TournamentStatus.ACTIVE)) {
			return true;
		}
		return false;
	}
	
	private boolean isActiveUser(String userId) throws InterruptedException, ExecutionException {
		DocumentReference userReference = firestore.collection("users").document(userId);
		DocumentSnapshot userSnapshot = userReference.get().get();
		if(userSnapshot.exists() && userSnapshot.toObject(User.class).getUserStatusCode().equals(UserStatus.ACTIVE)) {
			return true;
		}
		return false;
	}
	
	
	public User getUserById(String userId) throws InterruptedException, ExecutionException {
		DocumentReference userReference = firestore.collection("users").document(userId);
		DocumentSnapshot userSnapshot = userReference.get().get();
		if(userSnapshot.exists() && userSnapshot.toObject(User.class).getUserStatusCode().equals(UserStatus.ACTIVE)) {
			return userSnapshot.toObject(User.class);
		}
		return null; 
	}
	
	public User getUserByUserName(String userName) throws InterruptedException, ExecutionException {
		Query query = firestore.collection("users").whereEqualTo("userName", userName);
		QuerySnapshot querySnapshot = query.get().get();
		if(!querySnapshot.isEmpty()) {
			List<User> userList = querySnapshot.toObjects(User.class);
			for(User currUser: userList) {
				return currUser;
			}
		}
		return null;
	}
	
	public Optional<Tournament> getTournamentById(String tournamentId) throws InterruptedException, ExecutionException {
		if(isActiveUpcomingTournament(tournamentId)) {
			DocumentReference reference = getUpcomingTournamentsCollection().document(tournamentId);
			DocumentSnapshot snapshot = reference.get().get();
			return Optional.of(snapshot.toObject(Tournament.class));
		}
		return null;
	}
	
	public List<Tournament> getAllUpcomingTournaments() throws InterruptedException, ExecutionException{
		ApiFuture<QuerySnapshot> future = getUpcomingTournamentsCollection().get();
		return future.get().getDocuments()
				.stream()
				.map(document -> document.toObject(Tournament.class))
				.collect(Collectors.toList());
	}
	
	public Optional<Tournament> getTournamentByName(String tournamentName) throws InterruptedException, ExecutionException{
		ApiFuture<QuerySnapshot> snapshot = getUpcomingTournamentsCollection().whereEqualTo("tournamentName", tournamentName).get();
		if(!snapshot.get().isEmpty()) {
			List<QueryDocumentSnapshot> documents = snapshot.get().getDocuments();
			return Optional.of(documents
					.stream()
					.map(document -> document.toObject(Tournament.class))
					.collect(Collectors.toList()).get(0));
		}
		return null;
	}
	
	public List<Team> getTournamentTeams(String tournamentId) throws InterruptedException, ExecutionException {
		if(isActiveUpcomingTournament(tournamentId)) {
			DocumentReference tournamentReference = getUpcomingTournamentReference(tournamentId);
			DocumentSnapshot tournamentSnapshot = tournamentReference.get().get();
			return tournamentSnapshot.toObject(Tournament.class).getTournamentTeams();
		}
		return new ArrayList<>();
	}
	
	public String postTournament(User user, Tournament tournament) throws InterruptedException, ExecutionException {
		if(isActiveUser(user.getUserId())) {
			DocumentReference reference = getUpcomingTournamentsCollection().add(tournament).get();
			String documentId = reference.getId();
			WriteBatch batch = firestore.batch();
			batch.update(reference, "tournamentStatus", TournamentStatus.ACTIVE);
			batch.update(reference, "tournamentId", documentId);
			batch.update(reference, "tournamentModerator", user);
			batch.commit().get()
					.stream()
					.forEach(result -> System.out.println("Update Time: " + result.getUpdateTime()));
			
			addModeratorRoleToUser(user);
			return "Tournament with ID: '" + documentId + "' was created succesfully";
		}
		return "Not found.";
	}
	
	public String deleteTournament(Tournament tournament) throws InterruptedException, ExecutionException {
		if(isActiveUpcomingTournament(tournament.getTournamentId())) {
			DocumentReference tournamentReference = getUpcomingTournamentReference(tournament.getTournamentId());
			User user = tournament.getTournamentModerator();
			removeModeratorRoleFromUser(user);
			WriteBatch batch = firestore.batch();
			batch.update(tournamentReference, "tournamentStatus", TournamentStatus.INACTIVE);
			batch.commit().get().stream().forEach(result -> {
				System.out.println("Update Time: " + result.getUpdateTime());
			});
			return "Tournament with ID: '" + tournamentReference.getId() + "' was deleted.";
			
		}
		return "Tournament not found.";
	}
	
	public String updateTournament(Tournament tournament) throws InterruptedException, ExecutionException {
		if(isActiveUpcomingTournament(tournament.getTournamentId())) {
			DocumentReference reference = getUpcomingTournamentsCollection().document(tournament.getTournamentId());
			WriteBatch batch = firestore.batch();
			batch.set(reference, tournament);
			List<WriteResult> results = batch.commit().get();
			results.forEach(result -> System.out.println("Update Time: " + result.getUpdateTime()));
			return "Tournament updated successfully";
		}
		return "Tournament not found.";
	}
	
	public String deleteTournamentField(String tournamentId, String tournamentField) throws InterruptedException, ExecutionException {
		if(isActiveUpcomingTournament(tournamentId)) {
			DocumentReference reference = getUpcomingTournamentReference(tournamentId);
			WriteBatch batch = firestore.batch();
			batch.update(reference, tournamentField, FieldValue.delete());
			List<WriteResult> results = batch.commit().get();
			results.forEach(result -> System.out.println("Update time: " + result.getUpdateTime()));
			return "Tournament field deleted.";
		}
		return "Not found.";
	}
	
	private void addModeratorRoleToUser(User user) throws InterruptedException, ExecutionException {
		List<Role> userRoleLs = user.getUserRoles();
		Role role = new Role(Role.TOURNEY_ADMIN);
		if(!userRoleLs.contains(role)) {
			DocumentReference userReference = getUserReference(user.getUserId());
			userRoleLs.add(role);
			WriteBatch batch = firestore.batch();
			batch.update(userReference, "userRoles", userRoleLs);
			batch.commit().get()
				.stream()
				.forEach(result -> System.out.println("Update Time: " + result.getUpdateTime()));
		}
	}
	
	private void removeModeratorRoleFromUser(User user) throws InterruptedException, ExecutionException {
		DocumentReference reference = getUserReference(user.getUserId());
		List<Role> userRoleLs = user.getUserRoles();
		Role role = new Role(Role.TOURNEY_ADMIN);
		if(userRoleLs.contains(role)) {
			int index = userRoleLs.indexOf(role);
			userRoleLs.remove(index);
			WriteBatch batch = firestore.batch();
			batch.update(reference, "userRoles", userRoleLs);
			List<WriteResult> results = batch.commit().get();
			results.forEach(result -> 
				System.out.println("Update Time: " + result.getUpdateTime()));
		}
	}
}
