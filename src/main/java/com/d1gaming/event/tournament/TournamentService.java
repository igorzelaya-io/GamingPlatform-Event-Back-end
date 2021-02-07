package com.d1gaming.event.tournament;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.d1gaming.event.team.TeamService;
import com.d1gaming.library.role.ERole;
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
public class TournamentService {

	private final String TOURNAMENTS_COLLECTION = "tournaments";
	
	@Autowired
	private Firestore firestore;
	
	@Autowired
	private TeamService teamService;
	
	private CollectionReference getTournamentsCollection() {
		return firestore.collection(this.TOURNAMENTS_COLLECTION);
	}
	
	private DocumentReference getTournamentReference(String tournamentId) {
		return getTournamentsCollection().document(tournamentId);
	}
	
	private boolean isActiveTournament(String tournamentId) throws InterruptedException, ExecutionException {
		DocumentReference tourneyReference = getTournamentReference(tournamentId);
		DocumentSnapshot tourneySnapshot = tourneyReference.get().get();
		if(tourneySnapshot.toObject(Tournament.class).getTournamentStatus().equals(TournamentStatus.ACTIVE) && tourneySnapshot.exists()) {
			return true;
		}
		return false;
	}
	
	public User getUserById(String userId) throws InterruptedException, ExecutionException {
		DocumentReference userReference = firestore.collection("users").document(userId);
		DocumentSnapshot userSnapshot = userReference.get().get();
		if(userSnapshot.exists()) {
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
	
	private DocumentReference getUserReference(String userId) {
		return firestore.collection("users").document(userId);
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
	
	public List<Team> getTournamentTeams(String tournamentId) throws InterruptedException, ExecutionException{
		List<Team> tournamentTeamList = new ArrayList<>();
		if(isActiveTournament(tournamentId)) {
			DocumentReference tournamentReference = getTournamentReference(tournamentId);
			DocumentSnapshot tournamentSnapshot = tournamentReference.get().get();
			Tournament tournament = tournamentSnapshot.toObject(Tournament.class);
			tournamentTeamList = tournament.getTournamentTeams();
			return tournamentTeamList;
		}
		return null;
	}
	
	
	public String postTournament(User user, Tournament tournament) throws InterruptedException, ExecutionException {
		if(user.getUserStatusCode().equals(UserStatus.ACTIVE) && getUserById(user.getUserId()) != null) {
			DocumentReference reference = getTournamentsCollection().add(tournament).get();
			DocumentSnapshot snapshot = reference.get().get();
			String documentId = reference.getId();
			WriteBatch batch = firestore.batch();
			batch.update(reference, "tournamentId", documentId);
			batch.update(reference, "tournamentModerator", user);
			List<WriteResult> results = batch.commit().get();
			results.forEach(result -> {
				System.out.println("Update Time: " + result.getUpdateTime());
			});
			addModeratorRoleToUser(user);
			if(!snapshot.exists()) {
				return "Tournament could not be created";
			}
			return "Tournament with ID: '" + documentId + "' was created succesfully";
		}
		return "Not found.";
	}
	
	public String deleteTournament(String tournamentId) throws InterruptedException, ExecutionException {
		if(isActiveTournament(tournamentId)) {
			DocumentReference reference = getTournamentReference(tournamentId);
			DocumentSnapshot snapshot = reference.get().get();
			Tournament tournament = snapshot.toObject(Tournament.class);
			User user = tournament.getTournamentModerator();
			removeModeratorRoleFromUser(user);
			WriteBatch batch = firestore.batch();
			batch.update(reference, "tournamentStatus", TournamentStatus.INACTIVE);
			List<WriteResult> results = batch.commit().get();
			results.forEach(result -> {
				System.out.println("Update Time: " + result.getUpdateTime());
			});
			if(snapshot.toObject(Tournament.class).getTournamentStatus().equals(TournamentStatus.INACTIVE)) {
				return "Tournament with ID: '" + reference.getId() + "' was deleted.";
			}
			return "Tournament could not be deleted.";
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
	
	public String addTeamToTournament(Team team, Tournament tournament) throws InterruptedException, ExecutionException {
		if(isActiveTournament(tournament.getTournamentId()) && teamService.isActive(team.getTeamId())) {
			DocumentReference tourneyReference = getTournamentReference(tournament.getTournamentId());
			List<Team> tournamentTeamList = tournament.getTournamentTeams();
			WriteBatch batch = firestore.batch();
			tournamentTeamList.add(team);
			batch.update(tourneyReference, "tournamentTeams", tournamentTeamList);
			batch.update(tourneyReference, "tournamentNumberOfTeams", FieldValue.increment(1));
			List<WriteResult> results = batch.commit().get();
			results.forEach(result -> 
								System.out.println("Update Time: " +result.getUpdateTime()));
			return "Team added successfully to tournament.";
		}
		return "Not found.";
	}
	
	public String removeTeamFromTournament(Team team, Tournament tournament) throws InterruptedException, ExecutionException {
		if(isActiveTournament(tournament.getTournamentId()) && teamService.isActive(team.getTeamId())) {
			DocumentReference tournamentReference = getTournamentReference(tournament.getTournamentId());
			List<Team> tournamentTeamList = tournament.getTournamentTeams();
			if(tournamentTeamList.contains(team)) {
				int teamIndex = tournamentTeamList.indexOf(team);
				tournamentTeamList.remove(teamIndex);
				WriteBatch batch = firestore.batch();
				batch.update(tournamentReference, "tournamentTeams", tournamentTeamList);
				batch.update(tournamentReference, "tournamentNumberOfTeams", FieldValue.increment(-1));
				List<WriteResult> results = batch.commit().get();
				results.forEach(result -> 
						System.out.println("Update Time: " + result.getUpdateTime()));
				return "Team removed successfully.";
			}
		}
		return "Not found.";
	}
	
	private void addModeratorRoleToUser(User user) throws InterruptedException, ExecutionException {
		DocumentReference userReference = getUserReference(user.getUserId());
		List<Role> userRoleLs = user.getUserRoles();
		Role role = new Role("TourneyModerator", ERole.ROLE_TOURNEY_MODERATOR);
		if(userRoleLs.contains(role)) {
			return;
		}
		userRoleLs.add(role);
		WriteBatch batch = firestore.batch();
		batch.update(userReference, "userRoles", userRoleLs);
		List<WriteResult> results = batch.commit().get();
		results.forEach(result -> System.out.println("Update Time: " + result.getUpdateTime()));
	}
	
	private void removeModeratorRoleFromUser(User user) throws InterruptedException, ExecutionException {
		DocumentReference reference = getUserReference(user.getUserId());
		List<Role> userRoleLs = user.getUserRoles();
		Role role = new Role("TourneyModerator", ERole.ROLE_TOURNEY_MODERATOR);
		if(userRoleLs.contains(role)) {
			int roleIndex = userRoleLs.indexOf(role);
			userRoleLs.remove(roleIndex);
			WriteBatch batch = firestore.batch();
			batch.update(reference, "userRoles", userRoleLs);
			List<WriteResult> results = batch.commit().get();
			results.forEach(result -> 
				System.out.println("Update Time: " + result.getUpdateTime()));
		}
		return;
	}
}