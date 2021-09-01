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

import com.d1gaming.event.teamChallenge.TeamChallengeService;
import com.d1gaming.library.challenge.Challenge;
import com.d1gaming.library.challenge.ChallengeStatus;
import com.d1gaming.library.match.Match;
import com.d1gaming.library.match.MatchStatus;
import com.d1gaming.library.role.Role;
import com.d1gaming.library.team.Team;
import com.d1gaming.library.team.TeamCodChallenge;
import com.d1gaming.library.team.TeamStatus;
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
	
	@Autowired
	private TeamChallengeService teamChallengeService;
	
	
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
		Challenge challengeOnDB = challengeSnapshot.toObject(Challenge.class);
		if(challengeSnapshot.exists() && (challengeOnDB.getChallengeStatus().equals(ChallengeStatus.ACTIVE) 
										|| challengeOnDB.getChallengeStatus().equals(ChallengeStatus.IN_PROGRESS)
										|| challengeOnDB.getChallengeStatus().equals(ChallengeStatus.TERMINATED) )) {
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
	
	private boolean isActiveTeam(String teamId) throws InterruptedException, ExecutionException{
		DocumentReference teamReference = firestore.collection("teams").document(teamId);
		DocumentSnapshot teamSnapshot = teamReference.get().get();
		if(teamSnapshot.exists() && teamSnapshot.toObject(Team.class).getTeamStatus().equals(TeamStatus.ACTIVE)) {
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
	
	public List<Challenge> getAllChallengesById(List<String> challengesId) throws InterruptedException, ExecutionException{
		List<Challenge> challengeList = new ArrayList<>();
		for(String challengeId: challengesId) {
			Challenge retrievedChallenge = getChallengeById(challengeId);
			if(retrievedChallenge != null) {
				challengeList.add(retrievedChallenge);
			}
		}
		return challengeList;
	}
	
	//Get all Challenges available in collection.
	public List<Challenge> getAllChallenges() throws InterruptedException, ExecutionException{
		//Retrieve all documents asynchronously.
		ApiFuture<QuerySnapshot> snapshot = getChallengesCollection().whereEqualTo("challengeStatus", "ACTIVE").get();
		List<QueryDocumentSnapshot> documentList = snapshot.get().getDocuments();
		//If there are no documents, return null.
		List<Challenge> challengeList = new ArrayList<>();
		documentList.forEach(document -> {
			documentList.add(document);
		});
		return challengeList;
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
																.filter(userChallenge -> userChallenge.getUserChallengeId().equals(challengeId))
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
		Query query = getChallengesCollection().whereGreaterThan("challengeDate", oneWeekFromNow).whereEqualTo("challengeStatus", ChallengeStatus.ACTIVE);
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
		Query query = getChallengesCollection().whereLessThanOrEqualTo("challengeDate", oneWeekFromNow).whereEqualTo("challengeStatus", ChallengeStatus.ACTIVE);
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
	
	public Challenge postChallenge(String userId, Challenge challenge) throws InterruptedException, ExecutionException {
		if(isActiveUser(userId)) {
			DocumentReference userReference = firestore.collection("users").document(userId);
			User userOnDB = userReference.get().get().toObject(User.class);
			if(userOnDB.getUserTokens() >= challenge.getChallengeTokenFee()) {				
				challenge.setChallengeStatus(ChallengeStatus.ACTIVE);
				challenge.setStartedChallenge(false);
				challenge.setNumberOfPlayedMatches(0);
				addChallengeModeratorRoleToUser(userOnDB);
				DocumentReference challengeReference = getChallengesCollection().add(challenge).get();
				String documentId = challengeReference.getId();
				challenge.setChallengeId(documentId);
				WriteBatch batch = firestore.batch();
				batch.update(challengeReference, "challengeId", documentId);
				batch.update(userReference, "userTokens", FieldValue.increment(-challenge.getChallengeTokenFee()));
				batch.commit().get();
				return challenge;
			}
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
		if(isActive(challengeId)) {
			DocumentReference reference = getChallengesCollection().document(challengeId);			
			DocumentSnapshot challengeSnapshot = reference.get().get();
			Challenge challengeOnDB = challengeSnapshot.toObject(Challenge.class);
			this.teamChallengeService.removeTeamFromChallenge(challengeOnDB.getChallengeHostTeam(), challengeOnDB);
			if(challengeOnDB.getChallengeAwayTeam() != null) {
				this.teamChallengeService.removeTeamFromChallenge(challengeOnDB.getChallengeAwayTeam(), challengeOnDB);
			}
			WriteBatch batch = firestore.batch();
			batch.delete(reference);
			batch.commit().get();
			return "Challenge was deleted successfully.";
		}
		return "Not found.";
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
			if(!challengeOnDB.isStartedChallenge()) {				
				WriteBatch batch = firestore.batch();
				batch.update(challengeReference, "startedChallenge", true);
				batch.commit().get();			
				createMatchesForTeams(challengeOnDB);
				return challengeOnDB;
			}
			return challengeOnDB;
		}
		return null;
	}
	
	public void createMatchesForTeams(Challenge challenge) throws InterruptedException, ExecutionException {
		
			DocumentReference challengeReference = getChallengesCollection().document(challenge.getChallengeId());
			addMatchToTeams(challenge.getChallengeHostTeam(), challenge.getChallengeAwayTeam(), challenge);
			WriteBatch batch = firestore.batch();
			batch.update(challengeReference, "numberOfPlayedMatches", FieldValue.increment(1));
			batch.commit().get();
			
	}
	
	private Match addMatchToTeams(Team localTeam, Team awayTeam, Challenge challenge) throws InterruptedException, ExecutionException {
		if(isActiveTeam(localTeam.getTeamId()) && isActiveTeam(awayTeam.getTeamId()) && isActive(challenge.getChallengeId())) {
			Team localTeamOnDB = firestore.collection("teams").document(localTeam.getTeamId()).get().get().toObject(Team.class);
			Team awayTeamOnDB = firestore.collection("teams").document(awayTeam.getTeamId()).get().get().toObject(Team.class);		
			List<TeamCodChallenge> localTeamCodChallengesList = firestore.collection("teams").document(localTeam.getTeamId()).collection("teamCodChallenges").get().get()
																							.getDocuments()
																							.stream()
																							.map(document -> document.toObject(TeamCodChallenge.class))
																							.filter(teamCodChallenge -> teamCodChallenge.getTeamChallengeId().equals(challenge.getChallengeId()))
																							.collect(Collectors.toList());
			List<TeamCodChallenge> awayTeamCodChallengesList = firestore.collection("teams").document(awayTeam.getTeamId()).collection("teamCodChallenges").get().get()
																							.getDocuments()
																							.stream()
																							.map(document -> document.toObject(TeamCodChallenge.class))
																							.filter(teamCodChallenge -> teamCodChallenge.getTeamChallengeId().equals(challenge.getChallengeId()))
																							.collect(Collectors.toList());
			TeamCodChallenge localTeamCodChallenge = localTeamCodChallengesList.get(0);
			TeamCodChallenge awayTeamCodChallenge = awayTeamCodChallengesList.get(0);
			DocumentReference localTeamCodChallengeReference = firestore.collection("teams").document(localTeam.getTeamId()).collection("teamCodChallenges").document(localTeamCodChallenge.getTeamCodChallengeId());
			DocumentReference awayTeamCodChallengeReference = firestore.collection("teams").document(awayTeam.getTeamId()).collection("teamCodChallenges").document(awayTeamCodChallenge.getTeamCodChallengeId());
			List<Match> localTeamCodChallengeMatchesList = localTeamCodChallenge.getTeamCodChallengeMatches();
			List<Match> awayTeamCodChallengeMatchesList = awayTeamCodChallenge.getTeamCodChallengeMatches();
			Match match = new Match(challenge.getChallengeId(), localTeamOnDB, awayTeamOnDB, 0, 0, MatchStatus.ACTIVE, challenge.getChallengeGame().equals("Fifa") ? "Fifa" : "Call Of Duty");
			DocumentReference addedDocument = firestore.collection("challenges").document(challenge.getChallengeId()).collection("challengeMatches").add(match).get();
			String documentId = addedDocument.getId();
			match.setMatchId(documentId);
			localTeamCodChallengeMatchesList.add(match);
			awayTeamCodChallengeMatchesList.add(match);
			WriteBatch batch = firestore.batch();
			batch.update(addedDocument, "matchId", documentId);
			batch.update(localTeamCodChallengeReference, "teamCodChallengeMatches", localTeamCodChallengeMatchesList);
			batch.update(awayTeamCodChallengeReference, "teamCodChallengeMatches", awayTeamCodChallengeMatchesList);
			batch.commit().get();
			List<User> localTeamUsers = localTeamOnDB.getTeamUsers();
			List<User> awayTeamUsers = awayTeamOnDB.getTeamUsers();
			localTeamUsers.forEach(teamUser -> {
				try {
					addMatchToUser(teamUser, challenge, match);
				} catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();
				}
			});
			
			awayTeamUsers.forEach(teamUser -> {
				try {
					addMatchToUser(teamUser, challenge, match);
				} catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();
				}
			});
			return match;
		}
		return null;
	}
	
	public String addMatchToUser(User user, Challenge challenge, Match match) throws InterruptedException, ExecutionException {
		if(isActiveUser(user.getUserId())) {
			DocumentReference userReference = firestore.collection("users").document(user.getUserId());
			User userOnDB = userReference.get().get().toObject(User.class);
			List<UserChallenge> userChallengeList = userOnDB
														.getUserChallenges()
														.stream()
														.filter(userChallenge -> userChallenge.getUserChallengeId().equals(challenge.getChallengeId()))
														.collect(Collectors.toList());
			UserChallenge userChallenge = userChallengeList.get(0);
			List<Match> userChallengeMatches = userChallenge.getUserChallengeMatches();
			userChallengeMatches.add(match);
			WriteBatch batch = firestore.batch();
			batch.update(userReference, "userChallenges", userChallengeList);
			batch.commit().get();
			return "Match added";
		}
		return null;
	}

}
