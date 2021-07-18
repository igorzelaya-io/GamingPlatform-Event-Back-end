package com.d1gaming.event.challenge;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.d1gaming.library.challenge.Challenge;
import com.d1gaming.library.challenge.ChallengeStatus;
import com.d1gaming.library.match.Match;
import com.d1gaming.library.match.MatchStatus;
import com.d1gaming.library.role.Role;
import com.d1gaming.library.team.TeamTournamentStatus;
import com.d1gaming.library.user.User;
import com.d1gaming.library.user.UserChallenge;
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
public class ChallengeService {

	private final String CHALLENGES_COLLECTION = "challenges";
	
	@Autowired
	private Firestore firestore;
	
	
	//Get CollectionReference for Challenges collection.
	private CollectionReference getChallengesCollection() {
		return firestore.collection(this.CHALLENGES_COLLECTION);
	}
	
	private DocumentReference getChallengeReference(String challengeId) {
		return getChallengesCollection().document(challengeId);
	}
	
	private boolean isActive(String challengeId) throws InterruptedException, ExecutionException {
		DocumentReference challengeReference = getChallengeReference(challengeId);
		DocumentSnapshot challengeSnapshot = challengeReference.get().get();
		if(challengeSnapshot.exists() && challengeSnapshot.toObject(Challenge.class).getChallengeStatus().equals(ChallengeStatus.ACTIVE)) {
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
	
	
	//Get a challenge by its ID.
	public Challenge getChallengeById(String challengeId) throws InterruptedException, ExecutionException {
		//If document does not exist, return null.
		if(isActive(challengeId)) {
			DocumentReference reference = getChallengeReference(challengeId);
			return reference.get().get().toObject(Challenge.class);
		}
		return null;
	}
	
	
	//Get all Challenges available in collection.
	public List<Challenge> getAllChallenges() throws InterruptedException, ExecutionException{
		//Retrieve all documents asynchronously.
		ApiFuture<QuerySnapshot> snapshot = getChallengesCollection().get();
		List<QueryDocumentSnapshot> documentList = snapshot.get().getDocuments();
		//If there are no documents, return null.
		List<Challenge> userList = new ArrayList<>();
		documentList.forEach(document -> {
			documentList.add(document);
		});
		return userList;
	}
	
	public List<Match> getAllChallengeMatches(String challengeId) throws InterruptedException, ExecutionException{
		if(isActive(challengeId)) {
			return getChallengeReference(challengeId).collection("challengeMatches").get().get()
															.getDocuments()
															.stream()
															.map(document -> document.toObject(Match.class))
															.collect(Collectors.toList());
		}
		return new ArrayList<>();
	}
	
	public List<Match> getAllChallengeInactiveMatches(String challengeId) throws InterruptedException, ExecutionException{
		if(isActive(challengeId)) {
			return getChallengeReference(challengeId).collection("challengeMatches").get().get()
															.getDocuments()
															.stream()
															.map(document -> document.toObject(Match.class))
															.filter(match -> match.getMatchStatus().equals(MatchStatus.INACTIVE))
															.collect(Collectors.toList());
		}
		return new ArrayList<>();
	}
	
	public List<Match> getAllUserActiveMatches(String userId, String challengeId) throws InterruptedException, ExecutionException{
		if(isActive(challengeId) && isActiveUser(userId)) {
			User userOnDB = firestore.collection("users").document(userId).get().get().toObject(User.class);
			List<UserChallenge> userChallenges = userOnDB.getUserChallenges()
																.stream()
																.filter(userChallenge -> userChallenge.getUserChallengeStatus().equals(TeamTournamentStatus.ACTIVE))
																.filter(userChallenge -> userChallenge.getUserChallenge().getChallengeId().equals(challengeId))
																.collect(Collectors.toList());
			UserChallenge userChallenge = userChallenges.get(0);
			return userChallenge.getUserChallengeMatches()
									.stream()
									.filter(match -> match.getMatchStatus().equals(MatchStatus.ACTIVE))
									.collect(Collectors.toList());
								
		}
		return new ArrayList<>();
	}
	
	private final long ONE_WEEK_IN_MILLISECONDS = 604800000;
	
	public List<Challenge> getAllChallengesAfterOneWeek() throws InterruptedException, ExecutionException{
		Date oneWeekFromNow = new Date(System.currentTimeMillis() + ONE_WEEK_IN_MILLISECONDS);
		Query query = getChallengesCollection().whereGreaterThan("challengeDate", oneWeekFromNow);
		return query.get().get()
					.getDocuments()
					.stream()
					.map(document -> document.toObject(Challenge.class))
					.filter(challenge -> {
						try {
							return isActive(challenge.getChallengeId());
						} catch (InterruptedException | ExecutionException e) {
							
							e.printStackTrace();
						}
						return false;
					})
					.collect(Collectors.toList());
	}
	
	public List<Challenge> getAllChallengesBeforeOneWeek() throws InterruptedException, ExecutionException{
		Date oneWeekFromNow = new Date(System.currentTimeMillis() + ONE_WEEK_IN_MILLISECONDS);
		Query query = getChallengesCollection().whereLessThanOrEqualTo("challengeDate", oneWeekFromNow);
		return query.get().get().getDocuments()
						.stream()
						.map(document -> document.toObject(Challenge.class))
						.filter(challenge -> {
							try {
								return isActive(challenge.getChallengeId());
							} catch (InterruptedException | ExecutionException e) {
								e.printStackTrace();
							}
							return false;
						})
						.collect(Collectors.toList());
	}
	
	public Challenge postChallenge(User user, Challenge challenge) throws InterruptedException, ExecutionException {
		if(isActiveUser(user.getUserId())) {
			challenge.setChallengeStatus(ChallengeStatus.ACTIVE);
			challenge.setStartedChallenge(false);
			challenge.setChallengeMatches(new ArrayList<>());
			addChallengeModeratorRoleToUser(user);
			DocumentReference challengeReference = getChallengesCollection().add(challenge).get();
			String documentId = challengeReference.getId();
			challenge.setChallengeId(documentId);
			WriteBatch batch = firestore.batch();
			batch.update(challengeReference, "challengeId", documentId);
			batch.commit().get();
			return challenge;
		}
		return null;
	}
	
	private void addChallengeModeratorRoleToUser(User user) throws InterruptedException, ExecutionException {
		List<Role> userRoleList = user.getUserRoles();
		boolean hasRole = userRoleList
							.stream()
							.anyMatch(role -> role.getAuthority().equals(Role.CHALLENGE_ADMIN));
		if(!hasRole) {
			Role challengeAdminRole = new Role(Role.CHALLENGE_ADMIN);
			DocumentReference userReference = firestore.collection("users").document(user.getUserId());
			userRoleList.add(challengeAdminRole);
			WriteBatch batch = firestore.batch();
			batch.update(userReference, "userRoles", userRoleList);
			batch.commit().get();
		}
	}
	
	
	
	//Delete Challenge from collection by its ID.
	public String deleteChallengeById(String challengeId) throws InterruptedException, ExecutionException {
		DocumentReference reference = getChallengesCollection().document(challengeId);
		DocumentSnapshot snapshot = getChallengesCollection().document(challengeId).get().get();
		//Evaluate if challenge exists in collection.
		if(!snapshot.exists()) {
			return "Challenge not found.";
		}
		WriteBatch batch = firestore.batch();
		batch.delete(reference);
		batch.commit().get();
		return "Challenge with id '" + snapshot.toObject(Challenge.class).getChallengeId() + "' was deleted.";
	}
	
	//Delete challenge field
	public String deleteChallengeField(String challengeId, String challengeField) throws InterruptedException, ExecutionException {
		DocumentReference reference = getChallengesCollection().document(challengeId);
		DocumentSnapshot snapshot = reference.get().get();
		//Evaluate if challenge exists.
		if(snapshot.exists()) {
			Map<String,Object> map = new HashMap<>();
			map.put(challengeField, FieldValue.delete());
			WriteBatch batch = firestore.batch();
			batch.update(reference, map);
			List<WriteResult> ls = batch.commit().get();
			ls.forEach(result -> System.out.println("Update time: " + result.getUpdateTime()));
			return "Field deleted successfully"; 
		}
		return "Challenge not found.";
	}
	
	//Update a challenge.
	public String updateChallenge(Challenge challenge) throws InterruptedException, ExecutionException {
		DocumentReference reference = getChallengesCollection().document(challenge.getChallengeId());	
		//Evaluate if challenge exists.
		if(reference.get().get().exists()) {
			WriteBatch batch = firestore.batch();
			batch.set(reference, challenge);
			batch.commit().get();
			return "User updated Successfully.";
		}
		return "Challenge not found";
	}
	
	public String updateField(String challengeId, String challengeField, String replaceValue) throws InterruptedException, ExecutionException {
		DocumentReference reference = getChallengesCollection().document(challengeId);
		if(reference.get().get().exists()) {
			WriteBatch batch = firestore.batch();
			batch.update(reference, challengeField, replaceValue);
			batch.commit().get();
			return "Field updated successfully";
		}
		return "Challenge not found.";
	}
	
	public Challenge activateChallenge(String challengeId) throws InterruptedException, ExecutionException {
		if(isActive(challengeId)) {
			DocumentReference challengeReference = getChallengesCollection().document(challengeId);
			Challenge challengeOnDB = challengeReference.get().get().toObject(Challenge.class);
			challengeOnDB.setStartedChallenge(true);
			challengeReference.set(challengeOnDB);
			return challengeOnDB;
		}
		return null;
	}

}
