package com.d1gaming.event.tournament;

import java.util.ArrayList;
import java.util.Date;
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
import com.d1gaming.library.user.UserDetailsImpl;
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
public class TournamentService {

	private final String TOURNAMENTS_COLLECTION = "tournaments";
	
	@Autowired
	private Firestore firestore;
	
	private CollectionReference getTournamentsCollection() {
		return firestore.collection(this.TOURNAMENTS_COLLECTION);
	}
	
	private DocumentReference getTournamentReference(String tournamentId) {
		return getTournamentsCollection().document(tournamentId);
	}
	
	private DocumentReference getUserReference(String userId) {
		return firestore.collection("users").document(userId);
	}
	
	private boolean isActiveTournament(String tournamentId) throws InterruptedException, ExecutionException {
		DocumentReference tourneyReference = getTournamentReference(tournamentId);
		DocumentSnapshot tourneySnapshot = tourneyReference.get().get();
		if(tourneySnapshot.toObject(Tournament.class).getTournamentStatus().equals(TournamentStatus.ACTIVE) && tourneySnapshot.exists()) {
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
	
	public Optional<UserDetailsImpl> getUserDetailsByUserName(String userName) throws InterruptedException, ExecutionException{
		Query query = firestore.collection("users").whereEqualTo("userName", userName);
		QuerySnapshot querySnapshot = query.get().get();
		if(!querySnapshot.isEmpty()) {
			List<User> userList = querySnapshot.toObjects(User.class);
			for(User currUser: userList) {
				return Optional.of(UserDetailsImpl.build(currUser));
			}
		}
		return null;
	}
	
	public Optional<Tournament> getTournamentById(String tournamentId) throws InterruptedException, ExecutionException {
		if(isActiveTournament(tournamentId)) {
			DocumentReference reference = getTournamentsCollection().document(tournamentId);
			DocumentSnapshot snapshot = reference.get().get();
			return Optional.of(snapshot.toObject(Tournament.class));
		}
		return null;
	}
	
	//Get all documents from a collection.
	public List<Tournament> getAllTournaments() throws InterruptedException, ExecutionException{
		//asynchronously retrieve all documents
		ApiFuture<QuerySnapshot> future = getTournamentsCollection().get();
		// future.get() blocks on response
		return future.get().getDocuments()
				.stream()
				.map(document -> document.toObject(Tournament.class))
				.collect(Collectors.toList());
	}
	
//	public List<Tournament> getMostPopularCallOfDutyTournaments(){
//		
//	}
//	
//	public List<Tournament> getMostPopularFifaTournaments(){
//		
//	}
//	
//	public List<Tournament> getMostPopularUpcomingCallOfDutyTournaments(){
//		
//	}
//	
//	public List<Tournament> getMostPopulatUpcomingFifaTournaments(){
//		
//	}
	
	final long ONE_WEEK_IN_MILLISECONDS = 604800000;
	
	public List<Tournament> getAllTournamentsAfterOneWeek() throws InterruptedException, ExecutionException{
		Date oneWeekFromNow = new Date(System.currentTimeMillis() + ONE_WEEK_IN_MILLISECONDS); 
		Query query = getTournamentsCollection().whereGreaterThan("tournamentDate", oneWeekFromNow);
		return query.get().get().getDocuments()
				.stream()
				.map(document -> document.toObject(Tournament.class))
				.collect(Collectors.toList());
	}
	
	public List<Tournament> getAllTournamentsBeforeOneWeek() throws InterruptedException, ExecutionException{
		Date oneWeekFromNow = new Date(System.currentTimeMillis() + ONE_WEEK_IN_MILLISECONDS);
		Date now = new Date(System.currentTimeMillis());
		Query query = getTournamentsCollection().whereGreaterThan("tournamentDate", now).whereLessThanOrEqualTo("tournamentDate", oneWeekFromNow);
		return query.get().get().getDocuments()
				.stream()
				.map(document -> document.toObject(Tournament.class))
				.collect(Collectors.toList());
	}
	
	//Get a document By its ID.
	public Optional<Tournament> getTournamentByName(String tournamentName) throws InterruptedException, ExecutionException {
		ApiFuture<QuerySnapshot> snapshot = getTournamentsCollection().whereEqualTo("tournamentName", tournamentName).get();
		if(!snapshot.get().isEmpty()) {
			List<QueryDocumentSnapshot> documents = snapshot.get().getDocuments();
			return Optional.of(documents
					.stream()
					.map(document -> document.toObject(Tournament.class))
					.collect(Collectors.toList()).get(0));
		}
		return null;
	}
	
	public List<Team> getTournamentTeams(String tournamentId) throws InterruptedException, ExecutionException{
		if(isActiveTournament(tournamentId)) {
			DocumentReference tournamentReference = getTournamentReference(tournamentId);
			DocumentSnapshot tournamentSnapshot = tournamentReference.get().get();
			return tournamentSnapshot.toObject(Tournament.class).getTournamentTeams();
		}
		return new ArrayList<>();
	}
	
	
	public String postTournament(User user, Tournament tournament) throws InterruptedException, ExecutionException {
		if(isActiveUser(user.getUserId())) {
			DocumentReference reference = getTournamentsCollection().add(tournament).get();
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
		if(isActiveTournament(tournament.getTournamentId())) {
			DocumentReference tournamentReference = getTournamentReference(tournament.getTournamentId());
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
		if(isActiveTournament(tournament.getTournamentId())) {
			DocumentReference reference = getTournamentsCollection().document(tournament.getTournamentId());
			WriteBatch batch = firestore.batch();
			batch.set(reference, tournament);
			List<WriteResult> results = batch.commit().get();
			results.forEach(result -> System.out.println("Update Time: " + result.getUpdateTime()));
			return "Tournament updated successfully";
		}
		return "Tournament not found.";
	}
	
	public String deleteTournamentField(String tournamentId, String tournamentField) throws InterruptedException, ExecutionException {
		if(isActiveTournament(tournamentId)) {
			DocumentReference reference = getTournamentReference(tournamentId);
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
		boolean hasRole = userRoleLs
				.stream()
				.anyMatch(role -> role.getAuthority().equals("TOURNEY_ADMIN"));
		if(!hasRole) {
			Role role = new Role(Role.TOURNEY_ADMIN);
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
		boolean hasRole = userRoleLs
					.stream()
					.anyMatch(role -> role.getAuthority().equals("TOURNEY_ADMIN"));
		if(hasRole) {
			List<Role> userRoleLsWithoutModeratorRole = userRoleLs
					.stream()
					.filter(role -> !role.getAuthority().equals("TOURNEY_ADMIN"))
					.collect(Collectors.toList());
			WriteBatch batch = firestore.batch();
			batch.update(reference, "userRoles", userRoleLsWithoutModeratorRole);
			List<WriteResult> results = batch.commit().get();
			results.forEach(result -> 
				System.out.println("Update Time: " + result.getUpdateTime()));
		}
	}
}