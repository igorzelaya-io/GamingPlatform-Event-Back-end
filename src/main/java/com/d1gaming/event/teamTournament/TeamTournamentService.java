 package com.d1gaming.event.teamTournament;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.d1gaming.library.match.Match;
import com.d1gaming.library.match.MatchStatus;
import com.d1gaming.library.team.Team;
import com.d1gaming.library.team.TeamCodTournament;
import com.d1gaming.library.team.TeamFifaTournament;
import com.d1gaming.library.team.TeamStatus;
import com.d1gaming.library.team.TeamTournamentStatus;
import com.d1gaming.library.tournament.Tournament;
import com.d1gaming.library.tournament.TournamentStatus;
import com.d1gaming.library.user.User;
import com.d1gaming.library.user.UserStatus;
import com.d1gaming.library.user.UserTournament;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteBatch;

@Service
public class TeamTournamentService {

	@Autowired
	private Firestore firestore;
	
	private final String TEAM_COLLECTION = "teams";
	
	private final String TEAM_FIFA_TOURNAMENT_SUBCOLLECTION = "teamFifaTournaments";
	
	private final String TEAM_COD_TOURNAMENT_SUBCOLLECTION = "teamCodTournaments";
	
	private CollectionReference getTeamCollectionReference() {
		return firestore.collection(TEAM_COLLECTION);
	}
	
	private CollectionReference getFifaTournamentsSubcollectionFromTeam(String teamId) {
		return firestore.collection(TEAM_COLLECTION).document(teamId).collection(TEAM_FIFA_TOURNAMENT_SUBCOLLECTION);
	}
	
	private CollectionReference getCodTournamentsSubcollectionFromTeam(String teamId) {
		return firestore.collection(TEAM_COLLECTION).document(teamId).collection(TEAM_COD_TOURNAMENT_SUBCOLLECTION);
	}
	
	private DocumentReference getFifaTournamentReferenceFromTeam(String teamId, String tournamentDocumentId) {
		return getFifaTournamentsSubcollectionFromTeam(teamId).document(tournamentDocumentId);
	}
	
	private DocumentReference getCodTournamentReferenceFromTeam(String teamId, String tournamentDocumentId) {
		return getCodTournamentsSubcollectionFromTeam(teamId).document(tournamentDocumentId);
	}
	
	private DocumentReference getTournamentReference(String tournamentId) {
		return firestore.collection("tournaments").document(tournamentId); 
	}	
	
	private DocumentReference getTeamReference(String teamId) {
		return getTeamCollectionReference().document(teamId);
	}
	
	private boolean isActive(String teamId) throws InterruptedException, ExecutionException {
		DocumentReference teamReference = getTeamReference(teamId);
		DocumentSnapshot teamSnapshot = teamReference.get().get();
		if(teamSnapshot.exists() && teamSnapshot.toObject(Team.class).getTeamStatus().equals(TeamStatus.ACTIVE)) {
			return true;
		}
		return false;
	}
	
	private boolean isActiveTournament(String tournamentId) throws InterruptedException, ExecutionException {
		DocumentReference tournamentReference = firestore.collection("tournaments").document(tournamentId);
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
	
	
	public Optional<Match> getTeamMatchFromTournament(String matchId, String tournamentId) throws InterruptedException, ExecutionException{
		List<Match> tournamentMatchesList = getTournamentReference(tournamentId).collection("tournamentMatches").get().get()
													   .getDocuments()
													   .stream()
													   .map(document -> document.toObject(Match.class))
													   .filter(match -> match.getMatchId().equals(matchId))
													   .collect(Collectors.toList());
		return Optional.of(tournamentMatchesList.get(0));
	}
	
	
	public List<Tournament> getAllCodTournamentsFromTeam(String teamId) throws InterruptedException, ExecutionException{
		if(isActive(teamId)) {
			QuerySnapshot queryForDocuments = getCodTournamentsSubcollectionFromTeam(teamId).get().get();
			return queryForDocuments.getDocuments()
							.stream()
							.map(document -> document.toObject(TeamCodTournament.class))
							.map(teamCodTournament -> teamCodTournament.getTeamCodTournament())
							//.filter(tournament -> isActiveTournament());
							.collect(Collectors.toList());
		}
		return new ArrayList<>();
	}
	
	public List<Tournament> getAllFifaTournamentsFromTeam(String teamId) throws InterruptedException, ExecutionException {
		if(isActive(teamId)) {
			QuerySnapshot queryForTournaments = getFifaTournamentsSubcollectionFromTeam(teamId).get().get();
			return queryForTournaments.getDocuments()
					.stream()
					.map(document -> document.toObject(TeamFifaTournament.class))
					.map(teamFifaTournament -> teamFifaTournament.getTeamTournament())
					.collect(Collectors.toList());
		
		}
		return new ArrayList<>();
	}
	
	public List<Match> getAllInactiveFifaMatchesFromTournament(String teamId, String tournamentId) throws InterruptedException, ExecutionException{
		if(isActive(teamId) && isActiveTournament(tournamentId)) {
			return getFifaTournamentsSubcollectionFromTeam(teamId).get().get()
															.getDocuments()
															.stream()
															.map(document -> document.toObject(TeamFifaTournament.class))
															.filter(teamFifaTournament -> teamFifaTournament.getTeamTournament().getTournamentId().equals(tournamentId))
															.collect(Collectors.toList()).get(0)
															.getTeamTournamentMatches()
															.stream()
															.filter(match -> match.getMatchStatus().equals(MatchStatus.INACTIVE))
															.collect(Collectors.toList());
		}
		return new ArrayList<>();
	}
	
	public List<Match> getAllInactiveCodMatchesFromTournament(String teamId, String tournamentId) throws InterruptedException, ExecutionException{
		if(isActive(teamId) && isActiveTournament(tournamentId)) {
			return getCodTournamentsSubcollectionFromTeam(teamId).get().get()
															.getDocuments()
															.stream()
															.map(document -> document.toObject(TeamCodTournament.class))
															.filter(teamCodTournament -> teamCodTournament.getTeamCodTournament().getTournamentId().equals(tournamentId))
															.collect(Collectors.toList())
															.get(0)
															.getTeamCodTournamentMatches()
															.stream()
															.filter(match -> match.getMatchStatus().equals(MatchStatus.INACTIVE))
															.collect(Collectors.toList());
			
		}
		return new ArrayList<>();
	}
	
	public List<Match> getAllActiveFifaMatchesFromTournament(String teamId, String tournamentId) throws InterruptedException, ExecutionException{
		if(isActive(teamId) && isActiveTournament(tournamentId)) {
			return getFifaTournamentsSubcollectionFromTeam(teamId).get().get()
															.getDocuments()
															.stream()
															.map(document -> document.toObject(TeamFifaTournament.class))
															.filter(teamFifaTournament -> teamFifaTournament.getTeamTournament().getTournamentId().equals(tournamentId))
															.collect(Collectors.toList()).get(0)
															.getTeamTournamentMatches()
															.stream()
															.filter(match -> match.getMatchStatus().equals(MatchStatus.ACTIVE))
															.collect(Collectors.toList());
		}
		return new ArrayList<>();
	}
	
	public List<Match> getAllActiveCodMatchesFromTournament(String teamId, String tournamentId) throws InterruptedException, ExecutionException{
		if(isActive(teamId) && isActive(tournamentId)) {
			return getCodTournamentsSubcollectionFromTeam(teamId).get().get()
					.getDocuments()
					.stream()
					.map(document -> document.toObject(TeamCodTournament.class))
					.filter(teamCodTournament -> teamCodTournament.getTeamCodTournament().getTournamentId().equals(tournamentId))
					.collect(Collectors.toList())
					.get(0)
					.getTeamCodTournamentMatches()
					.stream()
					.filter(match -> match.getMatchStatus().equals(MatchStatus.ACTIVE))
					.collect(Collectors.toList());
		}
		return new ArrayList<>();
	}

	public List<Team> getAllTeamsWithHighestWins(Tournament tournament) throws InterruptedException, ExecutionException{
		if(isActiveTournament(tournament.getTournamentId())) {
			List<Team> teamsWithMoreWins = new ArrayList<>();
			//TODO: this method belongs to the tournament service.
			return teamsWithMoreWins;
		}
		return new ArrayList<>();
	}
	
	public Optional<Tournament> getFifaTournamentFromTeamById(String teamId, String tournamentId) throws InterruptedException, ExecutionException {
		if(isActiveTournament(teamId)) {
			List<TeamCodTournament> teamCodTournaments = getTeamReference(teamId).collection("teamCodTournaments").get().get()
																	.getDocuments()
																	.stream()
																	.map(document -> document.toObject(TeamCodTournament.class))
																	.filter(teamCodTournament -> teamCodTournament.getTeamCodTournament().getTournamentId().equals(tournamentId))
																	.collect(Collectors.toList());
			return Optional.of(teamCodTournaments.get(0).getTeamCodTournament());
		}
		return null; 
	}
	
	public Optional<Tournament> getCodTournamentFromTeamById(String teamId, String tournamentId) throws InterruptedException, ExecutionException {
		if(isActiveTournament(teamId) && isActive(tournamentId)) {
			List<TeamFifaTournament> teamFifaTournaments = getTeamReference(teamId).collection("teamFifaTournaments").get().get()
																	.getDocuments()
																	.stream()
																	.map(document -> document.toObject(TeamFifaTournament.class))
																	.filter(teamFifaTournament -> teamFifaTournament.getTeamTournament().getTournamentId().equals(tournamentId))
																	.collect(Collectors.toList());
			return Optional.of(teamFifaTournaments.get(0).getTeamTournament());
		}
		return null; 
	}
	
	
	public String addTeamToFifaTournament(Team team, Tournament tournament) throws InterruptedException, ExecutionException {
		if(isActiveTournament(tournament.getTournamentId()) && isActive(team.getTeamId())) {
			DocumentReference tourneyReference = getTournamentReference(tournament.getTournamentId());
			DocumentReference teamModeratorReference = firestore.collection("users").document(team.getTeamModerator().getUserId());
			Tournament tournamentOnDB = tourneyReference.get().get().toObject(Tournament.class);			
			List<Match> teamMatches = new ArrayList<>();
			TeamFifaTournament teamTournamentSubDocument = new TeamFifaTournament(tournamentOnDB, teamMatches, 0, 0, 0, 0, 0, 0, 0, 0, TeamTournamentStatus.ACTIVE);
			
			boolean isAlreadyPartOfTournament = getFifaTournamentsSubcollectionFromTeam(team.getTeamId()).get().get()
																	.getDocuments()
																	.stream()
																	.map(document -> document.toObject(TeamFifaTournament.class))
																	.anyMatch(teamFifaTournament -> teamFifaTournament.getTeamTournament().getTournamentId().equals(tournament.getTournamentId()));
			if(!isAlreadyPartOfTournament) {				
				ApiFuture<String> futureTransaction = firestore.runTransaction(transaction -> {
					DocumentSnapshot teamModeratorSnapshot = transaction.get(teamModeratorReference).get();
					DocumentSnapshot tourneySnapshot = transaction.get(tourneyReference).get();
					double teamModeratorTokens = teamModeratorSnapshot.getDouble("userTokens");
					double tourneyNumberOfTeams = tourneySnapshot.getDouble("tournamentNumberOfTeams");
					if(tournament.getTournamentLimitNumberOfTeams()  > tourneyNumberOfTeams && teamModeratorTokens > tournament.getTournamentEntryFee()) {
						DocumentReference addedDocumentToTeamFifaTournamentsSubcollection = getFifaTournamentsSubcollectionFromTeam(team.getTeamId()).add(teamTournamentSubDocument).get();						
						String documentId = addedDocumentToTeamFifaTournamentsSubcollection.getId();
						List<Team> tournamentTeamList = tournamentOnDB.getTournamentTeams();
						tournamentTeamList.add(team);
						transaction.update(teamModeratorReference, "userTokens", teamModeratorTokens - tournament.getTournamentEntryFee());
						transaction.update(addedDocumentToTeamFifaTournamentsSubcollection, "teamTournamentId", documentId);
						transaction.update(tourneyReference, "tournamentTeams", tournamentTeamList);
						transaction.update(tourneyReference, "tournamentNumberOfTeams", FieldValue.increment(1));
						return documentId;
					}
					else if(teamModeratorTokens < tournament.getTournamentEntryFee()){
						return "Not enough tokens to join tournament";
					}
					return "Tournament is already full.";
				});
				String transactionResult = futureTransaction.get();
				if(!transactionResult.equals("Tournament is already full.") || !transactionResult.equals("Not enough tokens to join tournament")) {
					DocumentReference teamFifaTournamentDocumentReference = getFifaTournamentsSubcollectionFromTeam(team.getTeamId()).document(transactionResult);
					Stack<Team> tournamentTeamStack = tournamentOnDB.getTournamentTeamBracketStack();
					WriteBatch batch = firestore.batch();
					List<User> teamUsers = team.getTeamUsers();
					teamUsers
						.stream()
						.forEach(user -> {
							try {
								addTournamentToUser(user, team, tournament);
							} catch (InterruptedException | ExecutionException e) {
								e.printStackTrace();
							}
						});
					boolean isWrittenBatch = false;
					if(tournamentTeamStack.isEmpty()) {
						tournamentTeamStack.add(team);
					}
					else {
						Team localTeam = tournamentTeamStack.pop();
						batch.update(teamFifaTournamentDocumentReference, "teamTournamentId", transactionResult);
						batch.update(tourneyReference, "tournamentTeamBracketStack", tournamentTeamStack);
						batch.commit().get()
							.stream()
							.forEach(result -> System.out.println("Update Time: " +result.getUpdateTime()));
						addMatchToFifaTeams(localTeam, team, tournamentOnDB);
						isWrittenBatch = true;
					}
					if(!isWrittenBatch) {
						batch.update(teamFifaTournamentDocumentReference, "teamTournamentId", transactionResult);
						batch.update(tourneyReference, "tournamentTeamBracketStack", tournamentTeamStack);
						batch.commit().get()
						.stream()
						.forEach(result -> System.out.println("Update Time: " +result.getUpdateTime()));
					}
					return "Team added successfully to tournament.";
				}
				return transactionResult;
			}
			return "Team is already part of tournament.";
		}
		return "Not found.";
	}
	
	public String addTeamToCodTournament(Team team, Tournament tournament) throws InterruptedException, ExecutionException {
		if(isActiveTournament(tournament.getTournamentId()) && isActive(team.getTeamId())) {
			DocumentReference tourneyReference = getTournamentReference(tournament.getTournamentId());
			DocumentReference userTeamModeratorReference = firestore.collection("users").document(team.getTeamModerator().getUserId()); 
			Tournament tournamentOnDB = tourneyReference.get().get().toObject(Tournament.class);
			Team teamOnDB = getTeamReference(team.getTeamId()).get().get().toObject(Team.class);
			List<Match> teamMatches = new ArrayList<>();
			TeamCodTournament teamCodTournamentSubdocument = new TeamCodTournament(tournamentOnDB,teamMatches, 0, 0, 0, 0, 0, 0, TeamTournamentStatus.ACTIVE);
			
			boolean isAlreadyPartOfTournament = getCodTournamentsSubcollectionFromTeam(team.getTeamId()).get().get()
																	.getDocuments()
																	.stream()
																	.map(document -> document.toObject(TeamCodTournament.class))
																	.anyMatch(teamCodTournament -> teamCodTournament.getTeamCodTournament().getTournamentName().equals(tournament.getTournamentName())); 
			if(!isAlreadyPartOfTournament) {
				ApiFuture<String> futureTransaction = firestore.runTransaction(transaction -> {
					DocumentSnapshot teamModeratorSnapshot = transaction.get(userTeamModeratorReference).get();
					DocumentSnapshot tourneySnapshot = transaction.get(tourneyReference).get();
					double teamModeratorTokens = teamModeratorSnapshot.getDouble("userTokens");
					double tourneyNumberOfTeams = tourneySnapshot.getDouble("tournamentNumberOfTeams");
					if(tournament.getTournamentLimitNumberOfTeams()  > tourneyNumberOfTeams && teamModeratorTokens > tournament.getTournamentEntryFee()) {
						DocumentReference addedDocumentToTeamCodTournaments = getCodTournamentsSubcollectionFromTeam(team.getTeamId()).add(teamCodTournamentSubdocument).get();
						String documentId = addedDocumentToTeamCodTournaments.getId();
						List<Team> tournamentTeamList = tournamentOnDB.getTournamentTeams();			
						tournamentTeamList.add(teamOnDB);
						transaction.update(userTeamModeratorReference, "userTokens", teamModeratorTokens - tournament.getTournamentEntryFee());
						transaction.update(tourneyReference, "tournamentTeams", tournamentTeamList);
						transaction.update(tourneyReference, "tournamentNumberOfTeams", FieldValue.increment(1));
						transaction.update(addedDocumentToTeamCodTournaments, "teamCodTournamentId", documentId);
						return documentId;
					}
					else if(teamModeratorTokens < tournament.getTournamentEntryFee()){
						return "Not enough tokens to join tournament";
					}
					return "Tournament is already full.";
				});
				String transactionResult = futureTransaction.get();
				if(!transactionResult.equals("Not enough tokens to join tournament") || !transactionResult.equals("Tournament is already full.")) {					
					DocumentReference teamCodTournamentDocumentReference = getCodTournamentsSubcollectionFromTeam(team.getTeamId()).document(transactionResult);
					Stack<Team> tournamentTeamStack = tournamentOnDB.getTournamentTeamBracketStack();
					WriteBatch batch = firestore.batch();
					List<User> teamUsers = teamOnDB.getTeamUsers();
					teamUsers
					.stream()
					.forEach(user -> {
						try {
							addTournamentToUser(user, team, tournament);
						} catch (InterruptedException | ExecutionException e) {
							e.printStackTrace();
						}
					});
					boolean isWrittenBatch = false;
					if(tournamentTeamStack.isEmpty()) {
						tournamentTeamStack.add(team);
					}
					else {
						Team localTeam = tournamentTeamStack.pop();
						batch.update(teamCodTournamentDocumentReference, "teamCodTournamentId", transactionResult);
						batch.update(tourneyReference, "tournamentTeamBracketStack", tournamentTeamStack);
						batch.commit().get()
							.stream()
							.forEach(result -> System.out.println("Update Time: " + result.getUpdateTime()));	
						addMatchToCodTeams(localTeam, teamOnDB, tournamentOnDB);					
						isWrittenBatch = true;
					}
					if(!isWrittenBatch) {
						batch.update(teamCodTournamentDocumentReference, "teamCodTournamentId", transactionResult);
						batch.update(tourneyReference, "tournamentTeamBracketStack", tournamentTeamStack);
						batch.commit().get()
							.stream()
							.forEach(result -> System.out.println("Update Time: " + result.getUpdateTime()));
					}				
					return "Team added successfully to tournament.";
				}
				return transactionResult;
			}
			return "Team is already part of tournament.";
		}
		return "Not found.";
	}
	
	//Add Tournament to userTournament subcollection.
	public String addTournamentToUser(User user, Team team, Tournament tournament) throws InterruptedException, ExecutionException {
		if(isActiveUser(user.getUserId()) && isActive(team.getTeamId()) && isActiveTournament(tournament.getTournamentId())) {
			List<Match> userTournamentMatches = new ArrayList<>();
			UserTournament userTournament = new UserTournament(tournament, team, 0, 0, userTournamentMatches, TeamTournamentStatus.ACTIVE);
			DocumentReference userReference = firestore.collection("users").document(user.getUserId());
			User userOnDB = userReference.get().get().toObject(User.class);
			List<UserTournament> userTournamentList = userOnDB.getUserTournaments();
			userTournamentList.add(userTournament);
			WriteBatch batch = firestore.batch();
			batch.update(userReference, "userTournaments", userTournamentList);
			batch.commit().get()
				.stream()
				.forEach(result -> System.out.println("Update Time: " + result.getUpdateTime()));
			return "User tournament added.";
		}
		return "Not found.";
	}
	
	public String removeTeamFromCodTournament(Team team, Tournament tournament) throws InterruptedException, ExecutionException {
		if(isActiveTournament(tournament.getTournamentId()) && isActive(team.getTeamId()) && !tournament.getStartedTournamentStatus()) {
			DocumentReference userTeamModeratorReference = firestore.collection("users").document(team.getTeamModerator().getUserId());
			DocumentReference tournamentReference = getTournamentReference(tournament.getTournamentId());
			Tournament tournamentOnDB = tournamentReference.get().get().toObject(Tournament.class);
			List<Team> tournamentTeamList = tournamentOnDB.getTournamentTeams();
			boolean isPartOfTournament = tournamentTeamList
										.stream()
										.anyMatch(tournamentTeam -> tournamentTeam.getTeamName().equals(team.getTeamName()));
			if(isPartOfTournament) {
				ApiFuture<String> futureTransaction = firestore.runTransaction(transaction -> {
					DocumentSnapshot teamModeratorSnapshot = transaction.get(userTeamModeratorReference).get();
					DocumentSnapshot tourneySnapshot = transaction.get(tournamentReference).get();
					double userTokens = teamModeratorSnapshot.getDouble("userTokens");
					double tournamentNumberOfTeams = tourneySnapshot.getDouble("tournamentNumberOfTeams");
					List<Team> newTournamentTeamList = tournamentTeamList
							.stream()
							.filter(tournamentTeam -> !tournamentTeam.getTeamName().equals(team.getTeamName()))
							.collect(Collectors.toList());
					
					transaction.update(tournamentReference, "tournamentTeams", newTournamentTeamList);
					transaction.update(userTeamModeratorReference, "userTokens", userTokens + tournament.getTournamentEntryFee());
					transaction.update(tournamentReference, "tournamentNumberOfTeams", tournamentNumberOfTeams - 1 );
					return "Team removed from Tournament";
				});
				
				List<TeamCodTournament> teamCodTournamentList = getCodTournamentsSubcollectionFromTeam(team.getTeamId()).get().get()
														.getDocuments()
														.stream()
														.map(document -> document.toObject(TeamCodTournament.class))
														.filter(teamCodTournament -> teamCodTournament.getTeamCodTournament().getTournamentId().equals(tournament.getTournamentId()))
														.collect(Collectors.toList()); 
				
				WriteBatch batch = firestore.batch();
				List<User> teamUsers = team.getTeamUsers();
				teamUsers
					.stream()
					.forEach(user -> {
						try {
							removeTournamentFromUser(user, team, tournament);
						} catch (InterruptedException | ExecutionException e) {
							e.printStackTrace();
						}
					});
				TeamCodTournament teamCodTournament = teamCodTournamentList.get(0);
				List<Match> teamCodTournamentMatchesList = teamCodTournament.getTeamCodTournamentMatches();
				
				Stack<Team> tournamentTeamStack = tournamentOnDB.getTournamentTeamBracketStack();
				if(!teamCodTournamentMatchesList.isEmpty()) {
					Match teamMatchOnTournamentToArrange = teamCodTournamentMatchesList.get(0);
					Team teamToAssignMatchTo = teamMatchOnTournamentToArrange.getMatchAwayTeam().getTeamId().equals(team.getTeamId()) ? teamMatchOnTournamentToArrange.getMatchLocalTeam() : teamMatchOnTournamentToArrange.getMatchAwayTeam();
					if(tournamentTeamStack.isEmpty()) {
						tournamentTeamStack.push(teamToAssignMatchTo);
					}
					else {
						Team poppedTeam = tournamentTeamStack.pop();
						addMatchToCodTeams(poppedTeam, teamToAssignMatchTo, tournamentOnDB);
					}
				}
				tournamentTeamStack.pop();
				if(!teamCodTournamentList.isEmpty()) {
					DocumentReference invalidDocument = getCodTournamentsSubcollectionFromTeam(team.getTeamId()).document(teamCodTournamentList.get(0).getTeamCodTournamentId());
					batch.update(invalidDocument, "teamTournamentStatus", TeamTournamentStatus.INACTIVE);
				}
				batch.update(tournamentReference, "tournamentTeamBracketStack", tournamentTeamStack);
				batch.commit().get()
							.stream()
							.forEach(result -> System.out.println("Update Time: " + result.getUpdateTime()));	
							 
			}							
		}
		return "Not found.";
	}
	
	public String removeTeamFromFifaTournament(Team team, Tournament tournament) throws InterruptedException, ExecutionException {
		if(isActiveTournament(tournament.getTournamentId()) && isActive(team.getTeamId()) && !tournament.getStartedTournamentStatus()) {
			DocumentReference userTeamModeratorReference = firestore.collection("users").document(team.getTeamModerator().getUserId());
			DocumentReference tournamentReference = getTournamentReference(tournament.getTournamentId());
			Tournament tournamentOnDB = tournamentReference.get().get().toObject(Tournament.class);
			List<Team> tournamentTeamList = tournamentOnDB.getTournamentTeams();
			boolean isPartOfTournament = tournamentTeamList
										.stream()
										.anyMatch(tournamentTeam -> tournamentTeam.getTeamName().equals(team.getTeamName()));
			if(isPartOfTournament) {
				ApiFuture<String> futureTransaction = firestore.runTransaction(transaction -> {
					DocumentSnapshot teamModeratorSnapshot = transaction.get(userTeamModeratorReference).get();
					DocumentSnapshot tourneySnapshot = transaction.get(tournamentReference).get();
					double userTokens = teamModeratorSnapshot.getDouble("userTokens");
					double tournamentNumberOfTeams = tourneySnapshot.getDouble("tournamentNumberOfTeams");
					List<Team> newTournamentTeamList = tournamentTeamList
							.stream()
							.filter(tournamentTeam -> !tournamentTeam.getTeamName().equals(team.getTeamName()))
							.collect(Collectors.toList());
					
					transaction.update(tournamentReference, "tournamentTeams", newTournamentTeamList);
					transaction.update(userTeamModeratorReference, "userTokens", userTokens + tournament.getTournamentEntryFee());
					transaction.update(tournamentReference, "tournamentNumberOfTeams", tournamentNumberOfTeams - 1 );
					return "Team removed from Tournament";
				});
				List<TeamFifaTournament> teamFifaTournamentsList = getFifaTournamentsSubcollectionFromTeam(team.getTeamId()).get().get()
														.getDocuments()
														.stream()
														.map(document -> document.toObject(TeamFifaTournament.class))
														.filter(teamTournament -> teamTournament.getTeamTournament().getTournamentName().equals(tournament.getTournamentName()))
														.collect(Collectors.toList());
				WriteBatch batch = firestore.batch();
				List<User> teamUsers = team.getTeamUsers();
				teamUsers
					.stream()
					.forEach(user -> {
						try {
							removeTournamentFromUser(user, team, tournament);
						} catch (InterruptedException | ExecutionException e) {
							e.printStackTrace();
						}
					});
				TeamFifaTournament teamFifaTournament = teamFifaTournamentsList.get(0);
				List<Match> teamCodTournamentMatchesList = teamFifaTournament.getTeamTournamentMatches();
				Stack<Team> tournamentTeamStack = tournamentOnDB.getTournamentTeamBracketStack();
				if(!teamCodTournamentMatchesList.isEmpty()) {
					Match teamMatchToArrange = teamCodTournamentMatchesList.get(0);
					Team teamToAssignMatchTo = teamMatchToArrange.getMatchAwayTeam().getTeamId().equals(team.getTeamId()) ? teamMatchToArrange.getMatchLocalTeam() : teamMatchToArrange.getMatchAwayTeam();
					if(tournamentTeamStack.isEmpty()) {
						tournamentTeamStack.push(teamToAssignMatchTo);
					}
					else {
						Team poppedTeam = tournamentTeamStack.pop();
						addMatchToFifaTeams(poppedTeam, teamToAssignMatchTo, tournamentOnDB);
						
					}
				}
				tournamentTeamStack.pop();
				if(!teamFifaTournamentsList.isEmpty()) {
					DocumentReference invalidDocument = getFifaTournamentsSubcollectionFromTeam(team.getTeamId()).document(teamFifaTournament.getTeamTournamentId());
					batch.update(invalidDocument, "teamTournamentStatus", TeamTournamentStatus.INACTIVE);
				}
				batch.update(tournamentReference, "tournamentTeamBracketStack", tournamentTeamStack);
				batch.commit().get()
							.stream()
							.forEach(result -> System.out.println("Update Time: " + result.getUpdateTime()));	
							 
			}							
		}
		return "Not found.";
	}
	
	public String removeTournamentFromUser(User user, Team team, Tournament tournament) throws InterruptedException, ExecutionException {
		if(isActiveUser(user.getUserId()) && isActive(team.getTeamId()) && isActiveTournament(tournament.getTournamentId())) {
			DocumentReference userReference = firestore.collection("users").document(user.getUserId());
			User userOnDB = userReference.get().get().toObject(User.class);
			List<UserTournament> userTournaments = userOnDB.getUserTournaments();
			List<UserTournament> userTournamentsListWithTournament = userTournaments
																		.stream()
																		.filter(userTournament -> userTournament.getUserTournament().getTournamentId().equals(tournament.getTournamentId()))
																		.collect(Collectors.toList());
			if(!userTournamentsListWithTournament.isEmpty()) {				
				UserTournament userTournament = userTournamentsListWithTournament.get(0);
				userTournament.setUserTournamentStatus(TeamTournamentStatus.INACTIVE);
				userTournaments.add(userTournament);
				WriteBatch batch = firestore.batch();
				batch.update(userReference, "userTournaments", userTournaments);
				batch.commit().get()
							.stream()
							.forEach(result -> System.out.println("Update Time: " + result.getUpdateTime()));
				return "Removed Tournament from user.";
			}
		}
		return "Not found.";
	}
	
	public String addMatchToFifaTeams(Team localTeam, Team awayTeam, Tournament matchTournament) throws InterruptedException, ExecutionException {
		if(isActive(localTeam.getTeamId()) && isActive(awayTeam.getTeamId()) && isActiveTournament(matchTournament.getTournamentId())) {
			List<TeamFifaTournament> localTeamFifaTournamentList = getFifaTournamentsSubcollectionFromTeam(localTeam.getTeamId()).get().get()
																		.getDocuments()
																		.stream()
																		.map(document -> document.toObject(TeamFifaTournament.class))
																		.filter(teamFifaTournament -> teamFifaTournament.getTeamTournament().getTournamentId().equals(matchTournament.getTournamentId()))
																		.collect(Collectors.toList());
			List<TeamFifaTournament> awayTeamFifaTournamentList = getFifaTournamentsSubcollectionFromTeam(awayTeam.getTeamId()).get().get()
																		.getDocuments()
																		.stream()
																		.map(document -> document.toObject(TeamFifaTournament.class))
																		.filter(teamFifaTournament -> teamFifaTournament.getTeamTournament().getTournamentId().equals(matchTournament.getTournamentId()))
																		.collect(Collectors.toList());
			TeamFifaTournament localTeamTournament = localTeamFifaTournamentList.get(0);
			TeamFifaTournament awayTeamTournament = awayTeamFifaTournamentList.get(0);
			DocumentReference localTeamFifaTournamentReference = getFifaTournamentReferenceFromTeam(localTeam.getTeamId(), localTeamTournament.getTeamTournamentId());
			DocumentReference awayTeamFifaTournamentReference = getFifaTournamentReferenceFromTeam(awayTeam.getTeamId(), awayTeamTournament.getTeamTournamentId());
			List<Match> localTeamFifaTournamentMatchesList = localTeamTournament.getTeamTournamentMatches();
			List<Match> awayTeamFifaTournamentMatchesList = awayTeamTournament.getTeamTournamentMatches();
			Match match = new Match(matchTournament, localTeam, awayTeam, 0, 0, MatchStatus.ACTIVE);
			DocumentReference addedDocument = getTournamentReference(matchTournament.getTournamentId()).collection("tournamentMatches").add(match).get();
			String matchDocumentId = addedDocument.getId();
			match.setMatchId(matchDocumentId);
			localTeamFifaTournamentMatchesList.add(match);
			awayTeamFifaTournamentMatchesList.add(match);
			WriteBatch batch = firestore.batch();
			batch.update(localTeamFifaTournamentReference, "teamTournamentMatches", localTeamFifaTournamentMatchesList);
			batch.update(awayTeamFifaTournamentReference, "teamTournamentMatches", awayTeamFifaTournamentMatchesList);
			batch.update(addedDocument, "matchId", matchDocumentId);
			batch.commit().get()
						.stream()
						.forEach(result -> System.out.println("Update Time: " + result.getUpdateTime()));
			List<User> localTeamUsersList = localTeam.getTeamUsers();
			List<User> awayTeamUsersList = awayTeam.getTeamUsers();
			localTeamUsersList
					.stream()
					.forEach(user -> {
						try {
							addMatchToUserInTournament(user, matchTournament, match);
						} 
						catch (InterruptedException | ExecutionException e) {
							e.printStackTrace();
						}
					});
			awayTeamUsersList
					.stream()
					.forEach(user -> {
						try {
							addMatchToUserInTournament(user, matchTournament, match);
						} catch (InterruptedException e) {
							e.printStackTrace();
						} catch (ExecutionException e) {
							e.printStackTrace();
						}
					});
			
			return "Match added successfully.";
		}
		return "Not found.";
	}
	
	public String addMatchToCodTeams(Team localTeam, Team awayTeam, Tournament matchTournament) throws InterruptedException, ExecutionException {
		if(isActive(localTeam.getTeamId()) && isActive(awayTeam.getTeamId()) && isActiveTournament(matchTournament.getTournamentId())) {
			List<TeamCodTournament> localTeamCodTournamentsList = getCodTournamentsSubcollectionFromTeam(localTeam.getTeamId()).get().get()
																		.getDocuments()
																		.stream()
																		.map(document -> document.toObject(TeamCodTournament.class))
																		.filter(teamCodTournament -> teamCodTournament.getTeamCodTournament().getTournamentId().equals(matchTournament.getTournamentId()))
																		.collect(Collectors.toList());
			List<TeamCodTournament> awayTeamCodTournamentsList = getCodTournamentsSubcollectionFromTeam(awayTeam.getTeamId()).get().get()
																		.getDocuments()
																		.stream()
																		.map(document -> document.toObject(TeamCodTournament.class))
																		.filter(teamCodTournament -> teamCodTournament.getTeamCodTournament().getTournamentId().equals(matchTournament.getTournamentId()))
																		.collect(Collectors.toList());
			TeamCodTournament localTeamCodTournament = localTeamCodTournamentsList.get(0);
			TeamCodTournament awayTeamCodTournament = awayTeamCodTournamentsList.get(0);
			DocumentReference localTeamCodTournamentReference = getCodTournamentReferenceFromTeam(localTeam.getTeamId(), localTeamCodTournament.getTeamCodTournamentId());
			DocumentReference awayTeamCodTournamentReference = getCodTournamentReferenceFromTeam(awayTeam.getTeamId(), awayTeamCodTournament.getTeamCodTournamentId());
			List<Match> localTeamCodTournamentMatchesList = localTeamCodTournament.getTeamCodTournamentMatches();
			List<Match> awayTeamCodTournamentMatchesList = awayTeamCodTournament.getTeamCodTournamentMatches();
			Match match = new Match(matchTournament, localTeam, awayTeam, 0, 0, MatchStatus.ACTIVE);
			DocumentReference matchTourneyDocument = getTournamentReference(matchTournament.getTournamentId()).collection("tournamentMatches").add(match).get();
			String matchDocumentId = matchTourneyDocument.getId();
			localTeamCodTournamentMatchesList.add(match);
			awayTeamCodTournamentMatchesList.add(match);
			
			WriteBatch batch = firestore.batch();
			batch.update(localTeamCodTournamentReference, "teamCodTournamentMatches", localTeamCodTournamentMatchesList);
			batch.update(awayTeamCodTournamentReference, "teamCodTournamentMatches", awayTeamCodTournamentMatchesList);
			batch.update(matchTourneyDocument, "matchId", matchDocumentId);
			batch.commit().get()
					.stream()
					.forEach(result -> System.out.println("Update Time: " + result.getUpdateTime()));
			List<User> localTeamUsersList = localTeam.getTeamUsers();
			List<User> awayTeamUsersList = awayTeam.getTeamUsers();
			localTeamUsersList
					.stream()
					.forEach(user -> {
						try {
							addMatchToUserInTournament(user, matchTournament, match);
						} catch (InterruptedException e) {
							e.printStackTrace();
						} catch (ExecutionException e) {
							e.printStackTrace();
						}
					});
			awayTeamUsersList
					.stream()
					.forEach(user -> {
						try {
							addMatchToUserInTournament(user, matchTournament, match);
						} catch (InterruptedException e) {
							e.printStackTrace();
						} catch (ExecutionException e) {
							e.printStackTrace();
						}
					});
			
			return "Match added Successfully.";
		}
		return "Not found.";
	}
	
	public String addMatchToUserInTournament(User user, Tournament tournament, Match match) throws InterruptedException, ExecutionException {
		if(isActiveUser(user.getUserId())) {
			DocumentReference userReference = firestore.collection("users").document(user.getUserId());
			User userOnDB = userReference.get().get().toObject(User.class);
			List<UserTournament> userTournamentList = userOnDB
														.getUserTournaments()
														.stream()
														.filter(userTournament -> userTournament.getUserTournament().getTournamentId().equals(tournament.getTournamentId()))
														.collect(Collectors.toList());
			
			UserTournament userTournament = userTournamentList.get(0);	
			List<Match> userTournamentMatches = userTournament.getUserTournamentMatches();
			userTournamentMatches.add(match);
			WriteBatch batch = firestore.batch();
			batch.update(userReference, "userTournaments", userTournamentList);
			batch.commit()
				.get()
				.stream()
				.forEach(result -> System.out.println("Update Time: " + result.getUpdateTime()));
			return "Match added";
		}
		return "Not found.";
	}
	
	public Tournament activateTournament(Tournament tournament) throws InterruptedException, ExecutionException {
		if(isActiveTournament(tournament.getTournamentId())) {			
			DocumentReference tourneyReference = getTournamentReference(tournament.getTournamentId());
			WriteBatch batch = firestore.batch();
			batch.update(tourneyReference, "startedTournamentStatus", true);
			batch.commit().get()
					.stream()
					.forEach( result -> System.out.println("Update Time: " + result.getUpdateTime()));
			return tourneyReference.get().get().toObject(Tournament.class);
		}
		return null;
	}
}
