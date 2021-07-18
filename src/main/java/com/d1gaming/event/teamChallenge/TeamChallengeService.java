package com.d1gaming.event.teamChallenge;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.d1gaming.library.challenge.Challenge;
import com.d1gaming.library.challenge.ChallengeStatus;
import com.d1gaming.library.match.Match;
import com.d1gaming.library.match.MatchStatus;
import com.d1gaming.library.team.Team;
import com.d1gaming.library.team.TeamCodChallenge;
import com.d1gaming.library.team.TeamStatus;
import com.d1gaming.library.team.TeamTournamentStatus;
import com.d1gaming.library.user.User;
import com.d1gaming.library.user.UserStatus;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;

@Service
public class TeamChallengeService {

	@Autowired
	private Firestore firestore;

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
	
	private DocumentReference getTeamChallengeReference(String teamId, String challengeId) {
		return getTeamCodChallengeCollectionReference(teamId).document(challengeId);
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
	
	public Optional<Match> getTeamMatchFromChallenge(String matchId, String challengeId) throws InterruptedException, ExecutionException{
		List<Match> challengeMatchesList = getChallengeReference(challengeId).collection("challengeMatches").get().get()
																			.getDocuments()
																			.stream()
																			.map(document -> document.toObject(Match.class))
																			.filter(match -> match.getMatchId().equals(matchId))
																			.collect(Collectors.toList());
		if(!challengeMatchesList.isEmpty()) {
			return Optional.of(challengeMatchesList.get(0));
		}
		return null;
	}
	
	public List<Challenge> getAllCodChallengesFromTeam(String teamId) throws InterruptedException, ExecutionException{
		if(isActive(teamId)) {
			QuerySnapshot queryForChallenges = getTeamCodChallengeCollectionReference(teamId).get().get();
			return queryForChallenges
							.getDocuments()
							.stream()
							.map(document -> document.toObject(TeamCodChallenge.class))
							.map(teamCodChallenge -> teamCodChallenge.getTeamCodChallenge())
							.collect(Collectors.toList());
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
	
	
	
	
	
}
