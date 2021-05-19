package com.d1gaming.event.tournament;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.d1gaming.event.teamTournament.TeamTournamentService;
import com.d1gaming.library.match.Match;
import com.d1gaming.library.match.MatchStatus;
import com.d1gaming.library.node.BinaryTree;
import com.d1gaming.library.node.TreeNode;
import com.d1gaming.library.node.TreeRound;
import com.d1gaming.library.role.Role;
import com.d1gaming.library.team.Team;
import com.d1gaming.library.team.TeamTournamentStatus;
import com.d1gaming.library.tournament.Tournament;
import com.d1gaming.library.tournament.TournamentStatus;
import com.d1gaming.library.user.User;
import com.d1gaming.library.user.UserDetailsImpl;
import com.d1gaming.library.user.UserStatus;
import com.d1gaming.library.user.UserTournament;
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
	private TeamTournamentService teamTournamentService;
	
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

	public List<Match> getAllTournamentMatches(String tournamentId) throws InterruptedException, ExecutionException{
		if(isActiveTournament(tournamentId)) {
			return getTournamentReference(tournamentId).collection("tournamentMatches").get().get()
																.getDocuments()
																.stream()
																.map(document -> document.toObject(Match.class))
																.collect(Collectors.toList());
		}
		return new ArrayList<>();
	
	}
	

	public List<Match> getAllTournamentInactiveMatches(String tournamentId) throws InterruptedException, ExecutionException{
		if(isActiveTournament(tournamentId)) {
			return getTournamentReference(tournamentId).collection("tournamentMatches").get().get()
																.getDocuments()
																.stream()
																.map(document -> document.toObject(Match.class))
																.filter(match -> match.getMatchStatus().equals(MatchStatus.INACTIVE))
																.collect(Collectors.toList());
		}
		return new ArrayList<>();
	}
	
	public List<Match> getAllUserActiveMatches(String userId, String tournamentId) throws InterruptedException, ExecutionException{
		if(isActiveUser(userId) && isActiveTournament(tournamentId)) {
			User userOnDB = getUserReference(userId).get().get().toObject(User.class);
			List<UserTournament> userTournaments = userOnDB.getUserTournaments()
												.stream()
												.filter(userTournament -> userTournament.getUserTournamentStatus().equals(TeamTournamentStatus.ACTIVE))
												.filter(userTournament -> userTournament.getUserTournament().getTournamentId().equals(tournamentId))
												.collect(Collectors.toList());
			UserTournament userTournament = userTournaments.get(0);
			return userTournament.getUserTournamentMatches()
					.stream()
					.filter(match -> match.getMatchStatus().equals(MatchStatus.ACTIVE))
					.collect(Collectors.toList());
		}
		return new ArrayList<>();
	}
	
	final long ONE_WEEK_IN_MILLISECONDS = 604800000;
	
	public List<Tournament> getAllTournamentsAfterOneWeek() throws InterruptedException, ExecutionException{
		Date oneWeekFromNow = new Date(System.currentTimeMillis() + ONE_WEEK_IN_MILLISECONDS); 
		Query query = getTournamentsCollection().whereGreaterThan("tournamentDate", oneWeekFromNow);
		return query.get().get().getDocuments()
				.stream()
				.map(document -> document.toObject(Tournament.class))
				.filter(tournament -> {
					try {
						return isActiveTournament(tournament.getTournamentId());
					} catch (InterruptedException | ExecutionException e) {
						e.printStackTrace();
					}
					return false;
				})
				.collect(Collectors.toList());
	}
	
	public List<Tournament> getAllTournamentsBeforeOneWeek() throws InterruptedException, ExecutionException{
		Date oneWeekFromNow = new Date(System.currentTimeMillis() + ONE_WEEK_IN_MILLISECONDS);
		Query query = getTournamentsCollection().whereLessThanOrEqualTo("tournamentDate", oneWeekFromNow);
		return query.get().get().getDocuments()
				.stream()
				.map(document -> document.toObject(Tournament.class))
				.filter(tournament -> {
					try {
						return isActiveTournament(tournament.getTournamentId());
					} catch (InterruptedException | ExecutionException e) {
						e.printStackTrace();
					}
					return false;
				})
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
			tournament.setTournamentStatus(TournamentStatus.ACTIVE);
			tournament.setTournamentTeams(new ArrayList<>());
			tournament.setTournamentModerator(user);
			tournament.setTournamentLeaderboardForLeague(new ArrayList<>());
			tournament.setTournamentTeamBracketStack(new Stack<>());
			tournament.setStartedTournament(false);
			addModeratorRoleToUser(user);
			DocumentReference reference = getTournamentsCollection().add(tournament).get();
			String documentId = reference.getId();
			WriteBatch batch = firestore.batch();
			batch.update(reference, "tournamentId", documentId);
			batch.commit().get();
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
	
	public Tournament activateTournament(Tournament tournament) throws InterruptedException, ExecutionException {
		if(isActiveTournament(tournament.getTournamentId())) {			
			DocumentReference tourneyReference = getTournamentReference(tournament.getTournamentId());
			Tournament tournamentOnDB = tourneyReference.get().get().toObject(Tournament.class);
			createNodesForTournament(tournamentOnDB);
			WriteBatch batch = firestore.batch();
			batch.update(tourneyReference, "startedTournament", true);
			batch.commit().get();
			return tourneyReference.get().get().toObject(Tournament.class);
		}
		return null;
	}
	
	private void createNodesForTournament(Tournament tournament) throws InterruptedException, ExecutionException {
		double numberOfMatches = 1;
		int numberOfRounds = 1;
		double numberOfTeamsInRound = 2;
		while(numberOfTeamsInRound < tournament.getTournamentNumberOfTeams() ) {
			numberOfTeamsInRound = 2;
			numberOfMatches += 1;
			numberOfTeamsInRound = Math.pow(numberOfTeamsInRound, numberOfMatches);
			numberOfRounds++;
		}
		BinaryTree tournamentTree = new BinaryTree();
		tournamentTree.setBinaryTreeNumberOfRounds(numberOfRounds);
		List<TreeRound> tournamentRounds = new ArrayList<TreeRound>();
		final int REMAINING_TEAMS_IN_BRACKET = (int) numberOfTeamsInRound - tournament.getTournamentNumberOfTeams();
		final int ROUND_ONE_NUMBER_OF_TEAMS = tournament.getTournamentNumberOfTeams() - REMAINING_TEAMS_IN_BRACKET;
		int roundTwoNumberOfTeams = (ROUND_ONE_NUMBER_OF_TEAMS / 2) + REMAINING_TEAMS_IN_BRACKET;
		TreeNode[] roundOneNodes = new TreeNode[ROUND_ONE_NUMBER_OF_TEAMS / 2];
		
		for(int i = 0; i < ROUND_ONE_NUMBER_OF_TEAMS / 2 ; i++) {
			Team localTeam = tournament.getTournamentTeamBracketStack().pop();
			Team awayTeam = tournament.getTournamentTeamBracketStack().pop();
			Match match = createMatchForTeams(awayTeam, localTeam, tournament);
			TreeNode roundOneNode = new TreeNode(match);
			roundOneNodes[i] = roundOneNode;
		}
		
		TreeRound roundOne = new TreeRound();
		roundOne.setTreeRoundLevel(0);
		roundOne.setTreeRoundNodes(roundOneNodes);
		
		int indexOfRoundOneNode = 0;
		int numberOfTeamsToPop = REMAINING_TEAMS_IN_BRACKET;
		boolean isPushedTeamIntoRoundTwo = false;
		boolean isGreaterNumberOfTeamsInRoundTwo = ROUND_ONE_NUMBER_OF_TEAMS < roundTwoNumberOfTeams ? true : false;
		int numberOfNodesToConnectToRoundOne = isGreaterNumberOfTeamsInRoundTwo ? (roundTwoNumberOfTeams - ROUND_ONE_NUMBER_OF_TEAMS) / 2 : null;
		TreeNode[] roundTwoNodes = new TreeNode[roundTwoNumberOfTeams / 2];
		for(int i = 0; i < roundTwoNumberOfTeams / 2 ; i++) {
			if(isGreaterNumberOfTeamsInRoundTwo) {
				if(!isPushedTeamIntoRoundTwo && numberOfNodesToConnectToRoundOne != 0) {						
					TreeNode roundOneNode = roundOneNodes[indexOfRoundOneNode];
					TreeNode roundTwoNode = new TreeNode();
					Team awayTeamInRoundTwo = tournament.getTournamentTeamBracketStack().pop();
					Match roundTwoMatch = createMatchForTeams(awayTeamInRoundTwo, null, tournament);
					roundTwoNode.setValue(roundTwoMatch);
					roundTwoNode.setLeft(roundOneNode);
					roundOne.getTreeRoundNodes()[indexOfRoundOneNode].setRootNode(roundTwoNode);
					roundTwoNodes[i] = roundTwoNode;
					numberOfNodesToConnectToRoundOne--;
					isPushedTeamIntoRoundTwo = true;
					indexOfRoundOneNode++;
				}
				else {
					TreeNode roundTwoNode = new TreeNode();
					Team localTeam = tournament.getTournamentTeamBracketStack().pop(); 
					Team awayTeam = tournament.getTournamentTeamBracketStack().pop();
					Match match = createMatchForTeams(awayTeam, localTeam, tournament);
					roundTwoNode.setValue(match);
					roundTwoNodes[i] = roundTwoNode;
					isPushedTeamIntoRoundTwo = false;
				}
				
			}
			else if(REMAINING_TEAMS_IN_BRACKET > 0 && !isGreaterNumberOfTeamsInRoundTwo){
				if(!isPushedTeamIntoRoundTwo && numberOfTeamsToPop != 0) {
					TreeNode roundOneNode = roundOneNodes[indexOfRoundOneNode];
					TreeNode roundTwoNode = new TreeNode();
					Team awayTeamInRoundTwo = tournament.getTournamentTeamBracketStack().pop();
					Match roundTwoMatch = createMatchForTeams(awayTeamInRoundTwo, null, tournament);
					roundTwoNode.setValue(roundTwoMatch);
					roundTwoNode.setLeft(roundOneNode);
					roundOne.getTreeRoundNodes()[indexOfRoundOneNode].setRootNode(roundTwoNode);
					isPushedTeamIntoRoundTwo = true;
					numberOfTeamsToPop--;
					indexOfRoundOneNode++;
				}
				else {
					TreeNode roundOneLeftNode = roundOneNodes[indexOfRoundOneNode];
					TreeNode roundOneRightNode = roundOneNodes[indexOfRoundOneNode + 1];
					TreeNode roundTwoNode = new TreeNode();
					Match match = new Match();
					roundTwoNode.setValue(match);
					roundTwoNode.setLeft(roundOneLeftNode);
					roundTwoNode.setRight(roundOneRightNode);
					roundOne.getTreeRoundNodes()[indexOfRoundOneNode].setRootNode(roundTwoNode);
					roundOne.getTreeRoundNodes()[indexOfRoundOneNode + 1].setRootNode(roundTwoNode);
					indexOfRoundOneNode += 2;
				}
			}
			else {
				TreeNode roundOneLeftNode = roundOneNodes[indexOfRoundOneNode];
				TreeNode roundOneRightNode = roundOneNodes[indexOfRoundOneNode + 1];
				TreeNode roundTwoNode = new TreeNode();
				Match roundTwoMatch = new Match();
				roundTwoNode.setValue(roundTwoMatch);
				roundTwoNode.setLeft(roundOneLeftNode);
				roundTwoNode.setRight(roundOneRightNode);
				roundOne.getTreeRoundNodes()[indexOfRoundOneNode].setRootNode(roundTwoNode);
				roundOne.getTreeRoundNodes()[indexOfRoundOneNode + 1].setRootNode(roundTwoNode);
				indexOfRoundOneNode += 2;
			}
		}
		tournamentRounds.add(roundOne);
		
		TreeRound roundTwo = new TreeRound();
		roundTwo.setTreeRoundLevel(1);
		roundTwo.setTreeRoundNodes(roundTwoNodes);
		
		int numberOfRoundsLeft = 0;
		int numberOfTeamsInRoundTwoToDivide = roundTwoNumberOfTeams / 2;
		while(numberOfTeamsInRoundTwoToDivide != 2) {
			numberOfTeamsInRoundTwoToDivide = numberOfTeamsInRoundTwoToDivide / 2;
			numberOfRoundsLeft++;
		}
		
		int roundTwoQuotient = 2;
		int levelOfRoundX = 2;
		
		TreeNode[] roundBeforeFinalsNodes = new TreeNode[roundTwoNumberOfTeams / 2];
		TreeRound roundX = new TreeRound();
		for(int i = 0; i < numberOfRoundsLeft; i++) {
			levelOfRoundX++;
			int roundXNumberOfTeams = roundTwoNumberOfTeams / roundTwoQuotient;
			TreeNode[] roundXNodes = new TreeNode[roundXNumberOfTeams / 2];
			roundX.setTreeRoundLevel(levelOfRoundX);			
			int indexOfRoundXNode = 0;
			if(i == 0) {
				for(int j = 0; j < roundXNumberOfTeams / 2; j++) {
					 TreeNode roundXNode = new TreeNode();
					 final Match roundXMatch = new Match();
					 TreeNode roundTwoLeftNode = roundTwoNodes[indexOfRoundXNode];
					 TreeNode roundTwoRightNode = roundTwoNodes[indexOfRoundXNode + 1];
					 roundXNode.setValue(roundXMatch);
					 roundXNode.setLeft(roundTwoLeftNode);
					 roundXNode.setRight(roundTwoRightNode);
					 roundTwo.getTreeRoundNodes()[indexOfRoundXNode].setRootNode(roundXNode);
					 roundTwo.getTreeRoundNodes()[indexOfRoundXNode + 1].setRootNode(roundXNode);
					 indexOfRoundXNode += 2;
					 roundXNodes[i] = roundXNode;
				}
				roundBeforeFinalsNodes = roundXNodes;
				roundX.setTreeRoundNodes(roundBeforeFinalsNodes);
				roundXNodes = new TreeNode[roundBeforeFinalsNodes.length];
				indexOfRoundXNode = 0;
				roundTwoQuotient += 2;
				tournamentRounds.add(roundTwo);
				continue;
			}
			for(int j = 0; j < roundXNumberOfTeams / 2; j++) {
				TreeNode roundXNode = new TreeNode();
				final Match roundXMatch = new Match();
				TreeNode roundBeforeLeftNode = roundBeforeFinalsNodes[indexOfRoundXNode];
				TreeNode roundBeforeRightNode = roundBeforeFinalsNodes[indexOfRoundXNode + 1];
				roundXNode.setValue(roundXMatch);
				roundXNode.setLeft(roundBeforeLeftNode);
				roundXNode.setRight(roundBeforeRightNode);
				roundX.getTreeRoundNodes()[indexOfRoundXNode].setRootNode(roundXNode);
				roundX.getTreeRoundNodes()[indexOfRoundXNode + 1].setRootNode(roundXNode);
				indexOfRoundXNode += 2;
				roundXNodes[i] = roundXNode;
			}
			tournamentRounds.add(roundX);
			roundTwoQuotient += 2;
			indexOfRoundXNode = 0;
			roundBeforeFinalsNodes = roundXNodes;
			roundX.setTreeRoundNodes(roundBeforeFinalsNodes);
			roundXNodes = new TreeNode[roundBeforeFinalsNodes.length];
		}
		TreeNode roundBeforeFinalsLeftNode = roundBeforeFinalsNodes[0];
		TreeNode roundBeforeFinalsRightNode = roundBeforeFinalsNodes[1];
		TreeNode finalRoundNode = new TreeNode();
		final Match finalMatch = new Match();
		finalRoundNode.setValue(finalMatch);
		finalRoundNode.setLeft(roundBeforeFinalsLeftNode);
		finalRoundNode.setRight(roundBeforeFinalsRightNode);
		roundX.getTreeRoundNodes()[0].setRootNode(finalRoundNode);
		roundX.getTreeRoundNodes()[1].setRootNode(finalRoundNode);
		tournamentRounds.add(roundX);
		TreeRound finalRound = new TreeRound();
		finalRound.setTreeRoundLevel(levelOfRoundX + 1);
		TreeNode[] finalRoundNodes = { finalRoundNode };
		finalRound.setTreeRoundNodes(finalRoundNodes);
		tournamentRounds.add(finalRound);
		tournamentTree.setBinaryTreeRounds(tournamentRounds);
		tournament.setTournamentBracketTree(tournamentTree);
		WriteResult tournamentReference = getTournamentReference(tournament.getTournamentId()).set(tournament).get();
		System.out.println("Update Time: " + tournamentReference.getUpdateTime());
	}
	
	private Match createMatchForTeams(Team awayTeam, Team localTeam, Tournament tournament) throws InterruptedException, ExecutionException {
		Match match = new Match();
		if(localTeam != null) {
			match.setMatchLocalTeam(localTeam);
			match.setLocalTeamMatchScore(0);
			match.setAwayTeamMatchScore(0);
			match.setMatchAwayTeam(awayTeam);
			match.setMatchTournament(tournament);
			match.setUploaded(false);
			match.setMatchStatus(MatchStatus.ACTIVE);
			if(tournament.getTournamentGame().equals("Fifa")) {
				this.teamTournamentService.addMatchToFifaTeams(localTeam, awayTeam, tournament);
				return match;
			}
			this.teamTournamentService.addMatchToCodTeams(localTeam, awayTeam, tournament);
			return match;
		}
		match.setAwayTeamMatchScore(0);
		match.setMatchAwayTeam(awayTeam);
		match.setMatchStatus(MatchStatus.ACTIVE);
		match.setUploaded(false);
		return match;
	}
}