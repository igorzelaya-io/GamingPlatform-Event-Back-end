package com.d1gaming.event.teamChallenge;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.d1gaming.event.challenge.ChallengeService;
import com.d1gaming.library.challenge.Challenge;
import com.d1gaming.library.challenge.ChallengeStatus;
import com.d1gaming.library.match.DisputedMatch;
import com.d1gaming.library.match.DisputedMatchStatus;
import com.d1gaming.library.match.Match;
import com.d1gaming.library.match.MatchStatus;
import com.d1gaming.library.team.Team;
import com.d1gaming.library.team.TeamCodChallenge;
import com.d1gaming.library.team.TeamStatus;
import com.d1gaming.library.team.TeamTournamentStatus;
import com.d1gaming.library.user.User;
import com.d1gaming.library.user.UserChallenge;
import com.d1gaming.library.user.UserStatus;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteBatch;
import com.google.cloud.firestore.WriteResult;

@Service
public class TeamChallengeService {

	@Autowired
	private Firestore firestore;
	
	@Autowired
	private ChallengeService challengeService;
	
	 

	private final String TEAM_COLLECTION = "teams";
	
	private final String TEAM_COD_CHALLENGE_SUBCOLLECTION = "teamCodChallenges";

	private CollectionReference getTeamCollectionReference() {
		return this.firestore.collection(TEAM_COLLECTION);
	}
	
	private CollectionReference getTeamCodChallengeCollectionReference(String teamId) {
		return this.firestore.collection("teams").document(teamId).collection(TEAM_COD_CHALLENGE_SUBCOLLECTION);
	}
	
	private DocumentReference getTeamReference(String teamId) {
		return getTeamCollectionReference().document(teamId);
	}
	
	private DocumentReference getChallengeReference(String challengeId) {
		return firestore.collection("challenges").document(challengeId);
	}
	
	private boolean isActive(String teamId) throws InterruptedException, ExecutionException {
		DocumentReference teamReference = getTeamReference(teamId);
		DocumentSnapshot teamSnapshot = teamReference.get().get();
		if(teamSnapshot.exists() && teamSnapshot.toObject(Team.class).getTeamStatus().equals(TeamStatus.ACTIVE)) {
			return true;
		}
		return false;
	}
	
	private boolean isActiveChallenge(String challengeId) throws InterruptedException, ExecutionException {
		DocumentReference challengeReference = getChallengeReference(challengeId);
		DocumentSnapshot challengeSnapshot = challengeReference.get().get();
		Challenge challengeOnDB = challengeSnapshot.toObject(Challenge.class);
		if(challengeSnapshot.exists() && (challengeOnDB.getChallengeStatus().equals(ChallengeStatus.ACTIVE) 
											|| challengeOnDB.getChallengeStatus().equals(ChallengeStatus.IN_PROGRESS)
											|| challengeOnDB.getChallengeStatus().equals(ChallengeStatus.TERMINATED))) {
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
	
	public Optional<Challenge> getCodChallengeFromTeamById(String teamId, String challengeId) throws InterruptedException, ExecutionException{
		if(isActive(teamId)) {
			List<TeamCodChallenge> teamCodChallenges = getTeamCodChallengeCollectionReference(teamId).get().get()
																	.getDocuments()
																	.stream()
																	.map(document -> document.toObject(TeamCodChallenge.class))
																	.filter(teamCodChallenge -> teamCodChallenge.getTeamChallengeId().equals(challengeId))
																	.collect(Collectors.toList());
			
			return Optional.of(challengeService.getChallengeById(teamCodChallenges.get(0).getTeamChallengeId()));
			
		}
		return null;
	}
	
	public Optional<Match> getTeamMatchFromChallenge(String matchId, String challengeId) throws InterruptedException, ExecutionException{
		List<Match> challengeMatchesList = getChallengeReference(challengeId).collection("challengeMatches").get().get()
																			.getDocuments()
																			.stream()
																			.map(document -> document.toObject(Match.class))
																			.filter(match -> match.getMatchId().equals(matchId))
																			.collect(Collectors.toList());
		
		return Optional.of(challengeMatchesList.get(0));
	}
	
	public List<Challenge> getAllCodChallengesFromTeam(String teamId) throws InterruptedException, ExecutionException{
		if(isActive(teamId)) {
			QuerySnapshot queryForChallenges = getTeamCodChallengeCollectionReference(teamId).get().get();
			List<String> challengeIds = queryForChallenges
					.getDocuments()
					.stream()
					.map(document -> document.toObject(TeamCodChallenge.class))
					.map(teamCodChallenge -> teamCodChallenge.getTeamChallengeId())
					.collect(Collectors.toList());
			return challengeService.getAllChallengesById(challengeIds);
			
		}
		return new ArrayList<Challenge>();
	}
	
	public List<Match> getAllInactiveMatchesFromChallenge(String teamId, String challengeId) throws InterruptedException, ExecutionException {
		if(isActive(teamId) && isActiveChallenge(challengeId)) {
			return getTeamCodChallengeCollectionReference(teamId).get().get()
															.getDocuments()
															.stream()
															.map(document -> document.toObject(TeamCodChallenge.class))
															.filter(teamCodChallenge -> teamCodChallenge.getTeamCodChallengeId().equals(challengeId))
															.collect(Collectors.toList())
															.get(0)
															.getTeamCodChallengeMatches()
															.stream()
															.filter(match -> match.getMatchStatus().equals(MatchStatus.INACTIVE))
															.collect(Collectors.toList());
			
		}
		return new ArrayList<>();
	}
	
	
	public List<Match> getAllActiveMatchesFromChallenge(String teamId, String challengeId) throws InterruptedException, ExecutionException{
		if(isActive(teamId) && isActiveChallenge(challengeId)) {
			return getTeamCodChallengeCollectionReference(teamId).get().get()
															.getDocuments()
															.stream()
															.map(document -> document.toObject(TeamCodChallenge.class))
															.filter(teamCodChallenge -> teamCodChallenge.getTeamCodChallengeId().equals(challengeId))
															.collect(Collectors.toList())
															.get(0)
															.getTeamCodChallengeMatches()
															.stream()
															.filter(match -> match.getMatchStatus().equals(MatchStatus.ACTIVE))
															.collect(Collectors.toList());
			
		}
		return new ArrayList<>();
	}
	
	public List<Match> getAllDisputedMatchesFromChallenge(String challengeId) throws InterruptedException, ExecutionException{
		if(isActiveChallenge(challengeId)) {
			return getChallengeReference(challengeId).collection("challengeMatches").get().get()
															.getDocuments()
															.stream()
															.map(document -> document.toObject(Match.class))
															.filter(match -> match.getMatchStatus().equals(MatchStatus.DISPUTED))
															.collect(Collectors.toList());
		}
		return new ArrayList<>();
	}
	
	//
	public String addTeamToChallenge(Team team, Challenge challenge) throws InterruptedException, ExecutionException {
		if(isActiveChallenge(challenge.getChallengeId()) && isActive(team.getTeamId())) {
			if(team.getTeamModerator().getUserTokens() >= challenge.getChallengeTokenFee()) {				
				DocumentReference challengeReference = getChallengeReference(challenge.getChallengeId());
				DocumentReference teamReference = getTeamReference(team.getTeamId());
				DocumentReference teamModeratorReference = firestore.collection("users").document(team.getTeamModerator().getUserId());
				Team teamOnDB = teamReference.get().get().toObject(Team.class);
				Challenge challengeOnDB = challengeReference.get().get().toObject(Challenge.class);
				challengeOnDB.setChallengeAwayTeam(teamOnDB);
				List<Match> teamCodChallengeMatches = new ArrayList<>();
				TeamCodChallenge teamCodChallengeSubdocument = new TeamCodChallenge(challengeOnDB.getChallengeId(), teamCodChallengeMatches, 0, 0, 0, 0, 0, 0, TeamTournamentStatus.IN_PROGRESS);
				teamCodChallengeSubdocument.setTeamChallengeTeamId(team.getTeamId());
				DocumentReference addedDocumentToTeamCodChallenges = getTeamCodChallengeCollectionReference(team.getTeamId()).add(teamCodChallengeSubdocument).get();
				String documentId = addedDocumentToTeamCodChallenges.getId();
				WriteBatch batch = firestore.batch();
				batch.update(addedDocumentToTeamCodChallenges, "teamCodChallengeId", documentId);
				batch.update(teamModeratorReference, "userTokens", FieldValue.increment(-challenge.getChallengeTokenFee()));
				batch.update(challengeReference, "challengeAwayTeam", teamOnDB);
				batch.update(challengeReference, "challengeStatus", ChallengeStatus.IN_PROGRESS);
				batch.commit().get();
				List<User> teamUsers = teamOnDB.getTeamUsers();
				teamUsers.forEach(teamUser -> {
					try {
						addChallengeToUser(teamUser, teamOnDB, challengeOnDB);
					} catch (InterruptedException | ExecutionException e) {
						e.printStackTrace();
					}
				});
				return "Team added to challenge successfully.";
			}
			return "Not enough tokens";
		}
		return "Not found.";
	}
	
	public void addChallengeToUser(User user, Team team, Challenge challenge) throws InterruptedException, ExecutionException {
		if(isActiveUser(user.getUserId()) && isActive(team.getTeamId()) && isActiveChallenge(challenge.getChallengeId())) {
			List<Match> userChallengeMatches = new ArrayList<>();
			UserChallenge userChallenge = new UserChallenge(challenge.getChallengeId(), team, userChallengeMatches, 0, 0, TeamTournamentStatus.IN_PROGRESS);
			DocumentReference userReference = firestore.collection("users").document(user.getUserId());
			User userOnDB = userReference.get().get().toObject(User.class);
			List<UserChallenge> userChallenges = userOnDB.getUserChallenges();
			userChallenges.add(userChallenge);
			WriteBatch batch = firestore.batch();
			batch.update(userReference, "userChallenges", userChallenges);
			batch.commit().get();
		}
	}
	
	public String removeTeamFromChallenge(Team team, Challenge challenge) throws InterruptedException, ExecutionException {
		if(isActiveChallenge(challenge.getChallengeId()) && isActive(team.getTeamId())) {
			DocumentReference teamReference = getTeamReference(team.getTeamId());
			DocumentReference challengeReference = getChallengeReference(challenge.getChallengeId());
			Challenge challengeOnDB = challengeReference.get().get().toObject(Challenge.class);
			Team teamOnDB = teamReference.get().get().toObject(Team.class);
			if(challengeOnDB.getChallengeAwayTeam().getTeamId().equals(team.getTeamId())) {
				List<TeamCodChallenge> teamCodChallenges = getTeamCodChallengeCollectionReference(team.getTeamId()).get().get()
																					.getDocuments()
																					.stream()
																					.map(document -> document.toObject(TeamCodChallenge.class))
																					.filter(teamCodChallenge -> teamCodChallenge.getTeamChallengeId().equals(challenge.getChallengeId()))
																					.collect(Collectors.toList());
				TeamCodChallenge teamCodChallenge = teamCodChallenges.get(0);
				getTeamCodChallengeCollectionReference(team.getTeamId()).document(teamCodChallenge.getTeamCodChallengeId()).delete();
				List<User> teamUsers = teamOnDB.getTeamUsers(); 
				teamUsers.forEach(teamUser -> {
					try {
						removeUserChallenge(teamUser, challengeOnDB);
					} catch (InterruptedException | ExecutionException e) {
						e.printStackTrace();
					}
				});
				WriteBatch batch = firestore.batch();
				batch.update(challengeReference, "challengeAwayTeam", null);
				batch.update(challengeReference, "challengeStatus", ChallengeStatus.ACTIVE);
				batch.commit().get();
				return "Team removed from challenge successfully.";
			}
		}
		return "Not found.";
	}
	
	public void removeUserChallenge(User user, Challenge challenge) throws InterruptedException, ExecutionException {
		if(isActiveUser(user.getUserId()) && isActiveChallenge(challenge.getChallengeId())) {
			DocumentReference userReference = firestore.collection("users").document(user.getUserId()); 
			User userOnDB = userReference.get().get().toObject(User.class);
			List<UserChallenge> userChallenges = userOnDB.getUserChallenges();
			List<UserChallenge> newUserChallengesList = userChallenges
															.stream()
															.filter(userChallenge -> !userChallenge.getUserChallengeId().equals(challenge.getChallengeId()))
															.collect(Collectors.toList());
			WriteBatch batch = firestore.batch();
			batch.update(userReference, "userChallenges", newUserChallengesList);
			batch.commit().get();
		}
	}
	
	public String uploadCodMatchResult(Match match, String challengeId, String teamId) throws InterruptedException, ExecutionException {
		if(isActiveChallenge(challengeId)) {
			DocumentReference challengeReference = getChallengeReference(challengeId);
			Challenge challengeOnDB = challengeReference.get().get().toObject(Challenge.class);
			Match matchOnDB = challengeReference.collection("challengeMatches").document(match.getMatchId()).get().get().toObject(Match.class);
			if(!matchOnDB.getMatchStatus().equals(MatchStatus.DISPUTED)) {
				Team uploadingTeam = firestore.collection("teams").document(teamId).get().get().toObject(Team.class);
				boolean isLocalTeam = match.getMatchLocalTeam().getTeamId().equals(uploadingTeam.getTeamId());
				if(!isValidMatchUploadedStatus(challengeOnDB, match, matchOnDB)) {
					match.setMatchStatus(MatchStatus.DISPUTED);
					match.setDisputedMatch(true);
					if(isLocalTeam) {
						match.setLocalTeamUploaded(true);
					}
					else {
						match.setAwayTeamUploaded(true);
					}
					challengeReference.collection("challengeMatches").document(match.getMatchId()).set(match);
					return "Match disputed.";
				}
				if(!match.isLocalTeamUploaded() && !match.isAwayTeamUploaded()) {
					if(isLocalTeam) {
						match.setLocalTeamUploaded(true);
					}
					else {
						match.setAwayTeamUploaded(true);
					}
					challengeReference.collection("challengeMatches").document(match.getMatchId()).set(match).get();
					return "Result recorded";
				}
				if(isLocalTeam) {
					match.setLocalTeamUploaded(true);
				}
				else {
					match.setAwayTeamUploaded(true);
				}
			}
			if(matchOnDB.getMatchStatus().equals(MatchStatus.DISPUTED)) {
				addResolvedStatusToDisputedMatch(matchOnDB.getMatchId());
				match.setDisputedMatch(false);
				match.setHasImage(false);
			}
			match.setMatchStatus(MatchStatus.INACTIVE);
			WriteResult resultFromReplacement = challengeReference.collection("challengeMatches").document(match.getMatchId()).set(match).get();
			System.out.println("Replaced Document at: " + resultFromReplacement.getUpdateTime());
			addInvalidStatusToTeamCodChallengeMatch(challengeId, match.getMatchAwayTeam(),  match);
			addInvalidStatusToTeamCodChallengeMatch(challengeId, match.getMatchLocalTeam(), match);
			TeamCodChallenge localTeamCodChallenge = getTeamCodChallengeCollectionReference(match.getMatchLocalTeam().getTeamId()).get().get()
																								.getDocuments()
																								.stream()
																								.map(document -> document.toObject(TeamCodChallenge.class))
																								.filter(teamCodChallenge -> teamCodChallenge.getTeamChallengeId().equals(challengeId))
																								.collect(Collectors.toList())
																								.get(0);
			
			TeamCodChallenge awayTeamCodChallenge = getTeamCodChallengeCollectionReference(match.getMatchAwayTeam().getTeamId()).get().get()
																								.getDocuments()
																								.stream()
																								.map(document -> document.toObject(TeamCodChallenge.class))
																								.filter(teamCodChallenge -> teamCodChallenge.getTeamChallengeId().equals(challengeId))
																								.collect(Collectors.toList())
																								.get(0);
			DocumentReference localTeamCodChallengeReference = getTeamReference(match.getMatchLocalTeam().getTeamId()).collection("teamCodChallenges").document(localTeamCodChallenge.getTeamCodChallengeId());
			DocumentReference awayTeamCodChallengeReference = getTeamReference(match.getMatchAwayTeam().getTeamId()).collection("teamCodChallenges").document(awayTeamCodChallenge.getTeamCodChallengeId());
			Team winningTeam = match.getMatchWinningTeam();
			Team losingTeam = winningTeam.getTeamId().equals(match.getMatchLocalTeam().getTeamId()) ? match.getMatchAwayTeam() : match.getMatchLocalTeam();
			WriteBatch batch = firestore.batch();
			batch.update(winningTeam.getTeamId().equals(localTeamCodChallenge.getTeamChallengeTeamId())
						? localTeamCodChallengeReference : awayTeamCodChallengeReference, "teamChallengeNumberOfMatchesWins", FieldValue.increment(1));
			batch.update(winningTeam.getTeamId().equals(localTeamCodChallenge.getTeamChallengeTeamId())
						? awayTeamCodChallengeReference: localTeamCodChallengeReference, "teamChallengeNumberOfMatchesLosses", FieldValue.increment(1));
			if(winningTeam.getTeamId().equals(match.getMatchLocalTeam().getTeamId())) {
				localTeamCodChallenge.setTeamChallengeNumberOfMatchesWins(localTeamCodChallenge.getTeamChallengeNumberOfMatchesWins() + 1);
			}
			else {
				awayTeamCodChallenge.setTeamChallengeNumberOfMatchesWins(awayTeamCodChallenge.getTeamChallengeNumberOfMatchesWins() + 1);
			}
			batch.update(localTeamCodChallengeReference, "teamChallengeNumberOfMatchesPlayed", FieldValue.increment(1));
			batch.update(awayTeamCodChallengeReference, "teamChallengeNumberOfMatchesPlayed", FieldValue.increment(1));
			addResultToTeams(winningTeam, losingTeam, challengeOnDB);
			batch.commit().get();
			WriteBatch matchesBatch = firestore.batch();
			if(challengeOnDB.getChallengeMatchesNumber().equals("Best of 1")){
				terminateChallenge(challengeOnDB, winningTeam, losingTeam);
			}
			else if(challengeOnDB.getChallengeMatchesNumber().equals("Best of 3")) {
				if(challengeOnDB.getNumberOfPlayedMatches() == 1) {
					addMatchToTeams(match.getMatchAwayTeam(), match.getMatchLocalTeam(), challengeOnDB);
					matchesBatch.update(challengeReference, "numberOfPlayedMatches", FieldValue.increment(1));
				}
				else if(challengeOnDB.getNumberOfPlayedMatches() == 2) {
					if(awayTeamCodChallenge.getTeamChallengeNumberOfMatchesWins() == 2 || localTeamCodChallenge.getTeamChallengeNumberOfMatchesWins() == 2) {
						terminateChallenge(challengeOnDB, winningTeam, losingTeam);
					}
					else {						
						addMatchToTeams(match.getMatchLocalTeam(), match.getMatchAwayTeam(), challengeOnDB);
						matchesBatch.update(challengeReference, "numberOfPlayedMatches", FieldValue.increment(1));
					}
				}
				else if(challengeOnDB.getNumberOfPlayedMatches() == 3) {
					terminateChallenge(challengeOnDB, winningTeam, losingTeam);
				}
			}
			else {
				switch(challengeOnDB.getNumberOfPlayedMatches()) {
				
				case 1:
					addMatchToTeams(match.getMatchAwayTeam(), match.getMatchLocalTeam(), challengeOnDB);
					matchesBatch.update(challengeReference, "numberOfPlayedMatches", FieldValue.increment(1));
					break;
				case 2:
					addMatchToTeams(match.getMatchLocalTeam(), match.getMatchAwayTeam(), challengeOnDB);
					matchesBatch.update(challengeReference, "numberOfPlayedMatches", FieldValue.increment(1));
					break;
				case 3:
					if(awayTeamCodChallenge.getTeamChallengeNumberOfMatchesWins() == 3 || localTeamCodChallenge.getTeamChallengeNumberOfMatchesWins() == 3) {
						terminateChallenge(challengeOnDB, winningTeam, losingTeam);
						break;
					}
					else {
						addMatchToTeams(match.getMatchAwayTeam(), match.getMatchLocalTeam(), challengeOnDB);
						matchesBatch.update(challengeReference, "numberOfPlayedMatches", FieldValue.increment(1));
						break;
					}
				case 4:
					if(awayTeamCodChallenge.getTeamChallengeNumberOfMatchesWins() == 3 || localTeamCodChallenge.getTeamChallengeNumberOfMatchesWins() == 3) {
						terminateChallenge(challengeOnDB, winningTeam, losingTeam);
						break;
					}
					else {
						addMatchToTeams(match.getMatchLocalTeam(), match.getMatchAwayTeam(), challengeOnDB);
						matchesBatch.update(challengeReference, "numberOfPlayedMatches", FieldValue.increment(1));
						break;
					}
				case 5:
					terminateChallenge(challengeOnDB, winningTeam, losingTeam);
					break;
				default:
					terminateChallenge(challengeOnDB, winningTeam, losingTeam);
				}
			}
			matchesBatch.commit().get();
			return "Match results updated successfully.";
		}
		return "Not found.";
	}
	
	private boolean isValidMatchUploadedStatus(Challenge challengeOnDB, Match match, Match matchOnDB) throws InterruptedException, ExecutionException {
		if(matchOnDB.isLocalTeamUploaded() || matchOnDB.isAwayTeamUploaded()) {
			Team winningTeam = match.getMatchWinningTeam();
			if(matchOnDB.getMatchWinningTeam().getTeamId().equals(winningTeam.getTeamId())) {
				return true;
			}
			else {
				return false;
			}
		}
		return true;
	}
	
	private void terminateChallenge(Challenge challenge, Team challengeWinningTeam, Team challengeLosingTeam) throws InterruptedException, ExecutionException {
		DocumentReference teamModeratorReference = firestore.collection("users").document(challengeWinningTeam.getTeamModerator().getUserId());
		DocumentReference challengeReference = getChallengeReference(challenge.getChallengeId());
		WriteBatch batch = firestore.batch();
		batch.update(challengeReference, "challengeStatus", ChallengeStatus.TERMINATED);
		batch.update(challengeReference, "challengeWinningTeam", challengeWinningTeam);
		batch.update(teamModeratorReference, "userCash", FieldValue.increment(challenge.getChallengeCashPrize()));
		batch.commit().get();
	}
	
	private void addResultToTeams(Team winningTeam, Team losingTeam, Challenge challengeOnDB) throws InterruptedException, ExecutionException {
		DocumentReference winningTeamReference = getTeamReference(winningTeam.getTeamId());
		DocumentReference losingTeamReference = getTeamReference(losingTeam.getTeamId());
		WriteBatch batch = firestore.batch();
		if(challengeOnDB.getChallengeGame().equals("Call Of Duty")) {
			batch.update(winningTeamReference, "teamCodTotalWs", FieldValue.increment(1));
			batch.update(losingTeamReference, "teamCodTotalLs", FieldValue.increment(1));
		}
		else {
			batch.update(winningTeamReference, "teamFifaTotalWs", FieldValue.increment(1));
			batch.update(losingTeamReference, "teamFifaTotalLs", FieldValue.increment(1));
		}
		batch.commit().get();
		List<User> winningTeamUsers = winningTeam.getTeamUsers();
		List<User> losingTeamUsers = losingTeam.getTeamUsers();
		winningTeamUsers
						.stream()
						.forEach(user -> {
							try {
								addResultToUser(user, true, challengeOnDB);
							} catch (InterruptedException | ExecutionException e) {
								e.printStackTrace();
							}
						});
		losingTeamUsers
						.stream()
						.forEach(user -> {
							try {
								addResultToUser(user, false, challengeOnDB);
							} catch (InterruptedException | ExecutionException e) {
								e.printStackTrace();
							}
						});
	}
	
	private void addResultToUser(User user, boolean isWinningUser, Challenge challenge) throws InterruptedException, ExecutionException {
		DocumentReference userReference = firestore.collection("users").document(user.getUserId());
		WriteBatch batch = firestore.batch();
		if(challenge.getChallengeGame().equals("Call Of Duty")) {
			batch.update(userReference, isWinningUser ? "userCodTotalWs" : "userCodTotalLs", FieldValue.increment(1));
		}
		else {
			batch.update(userReference, isWinningUser ? "userFifaTotalWs" : "userFifaTotalLs", FieldValue.increment(1));
		}
		batch.commit().get();
	}

	private Match addMatchToTeams(Team localTeam, Team awayTeam, Challenge challenge) throws InterruptedException, ExecutionException {
		if(isActive(localTeam.getTeamId()) && isActive(awayTeam.getTeamId()) && isActiveChallenge(challenge.getChallengeId())) {
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
	
	public void addInvalidStatusToTeamCodChallengeMatch(String challengeId, Team team, Match match) throws InterruptedException, ExecutionException {
		if(isActive(team.getTeamId())) {
			Team teamOnDB = getTeamReference(team.getTeamId()).get().get().toObject(Team.class);
			List<TeamCodChallenge> teamCodChallengeWithChallenge = getTeamCodChallengeCollectionReference(team.getTeamId()).get().get()
																		.getDocuments()
																		.stream()
																		.map(document -> document.toObject(TeamCodChallenge.class))
																		.filter(teamCodChallenge -> teamCodChallenge.getTeamChallengeId().equals(challengeId))
																		.collect(Collectors.toList());
			if(!teamCodChallengeWithChallenge.isEmpty()) {
				TeamCodChallenge teamCodChallenge = teamCodChallengeWithChallenge.get(0);
				List<Match> teamCodChallengeMatches = teamCodChallenge.getTeamCodChallengeMatches();
				List<Match> teamCodChallengeMatchesWithMatch = teamCodChallenge.getTeamCodChallengeMatches()
																				.stream()
																				.filter(challengeMatch -> challengeMatch.getMatchId().equals(match.getMatchId()))
																				.collect(Collectors.toList());
				if(!teamCodChallengeMatches.isEmpty()) {
					Match matchOnDB = teamCodChallengeMatchesWithMatch.get(0);
					int indexOfMatch = teamCodChallengeMatches.indexOf(matchOnDB);
					teamCodChallengeMatches.remove(indexOfMatch);
					teamCodChallengeMatches.add(match);
					DocumentReference teamCodChallengeReference = getTeamReference(team.getTeamId()).collection("teamCodChallenges").document(teamCodChallenge.getTeamCodChallengeId());
					WriteBatch batch = firestore.batch();
					batch.update(teamCodChallengeReference, "teamCodChallengeMatches", teamCodChallengeMatches);
					batch.commit().get();
				}
			}
			List<User> teamUsers = teamOnDB.getTeamUsers();
			teamUsers.forEach(user -> {
				try {
					addInvalidStatusToUserMatch(user, challengeId, match);
				} catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();
				}
			});
		}
	}
	
	public void addInvalidStatusToUserMatch(User user, String challengeId, Match match) throws InterruptedException, ExecutionException {
		DocumentReference userReference = firestore.collection("users").document(user.getUserId());
		User userOnDB = userReference.get().get().toObject(User.class);
		List<UserChallenge> userChallengeListWithChallenge = userOnDB.getUserChallenges()
														.stream()
														.filter(userChallenge -> userChallenge.getUserChallengeId().equals(challengeId))
														.collect(Collectors.toList());
		List<UserChallenge> userChallengeList = userOnDB.getUserChallenges();
		if(!userChallengeListWithChallenge.isEmpty()) {
			UserChallenge userChallenge = userChallengeListWithChallenge.get(0);
			int indexOfChallenge = userChallengeList.indexOf(userChallenge);
			userChallengeList.remove(indexOfChallenge);
			List<Match> userChallengeMatches = userChallenge.getUserChallengeMatches();
			List<Match> userChallengeMatchesWithMatch = userChallengeMatches
														.stream()
														.filter(challengeMatch -> challengeMatch.getMatchId().equals(match.getMatchId()))
														.collect(Collectors.toList());
			Match matchOnDB = userChallengeMatchesWithMatch.get(0);
			int indexOfMatch = userChallengeMatches.indexOf(matchOnDB);
			userChallengeMatches.remove(indexOfMatch);
			userChallengeMatches.add(match);
			userChallenge.setUserChallengeMatches(userChallengeMatches);
			userChallengeList.add(userChallenge);
			WriteBatch batch = firestore.batch();
			batch.update(userReference, "userChallenges", userChallengeList);
			batch.commit().get();
		}
	}
	
	public void addResolvedStatusToDisputedMatch(String matchId) throws InterruptedException, ExecutionException {
		QuerySnapshot query = firestore.collection("disputedMatches").whereEqualTo("disputedMatchMatchId", matchId).get().get();
		DisputedMatch disputedMatch = query.toObjects(DisputedMatch.class).get(0);
		disputedMatch.setDisputedMatchStatus(DisputedMatchStatus.RESOLVED);
		firestore.collection("disputedMatches").document(disputedMatch.getDisputedMatchDocumentId()).set(disputedMatch);
	}

}
