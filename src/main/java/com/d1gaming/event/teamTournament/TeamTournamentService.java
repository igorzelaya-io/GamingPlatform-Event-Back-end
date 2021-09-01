 package com.d1gaming.event.teamTournament;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.d1gaming.event.tournament.TournamentService;
import com.d1gaming.library.match.DisputedMatch;
import com.d1gaming.library.match.DisputedMatchStatus;
import com.d1gaming.library.match.Match;
import com.d1gaming.library.match.MatchStatus;
import com.d1gaming.library.team.Team;
import com.d1gaming.library.team.TeamCodTournament;
import com.d1gaming.library.team.TeamFifaTournament;
import com.d1gaming.library.team.TeamStatus;
import com.d1gaming.library.team.TeamTournamentStatus;
import com.d1gaming.library.tournament.Tournament;
import com.d1gaming.library.tournament.TournamentFormat;
import com.d1gaming.library.tournament.TournamentStatus;
import com.d1gaming.library.user.User;
import com.d1gaming.library.user.UserStatus;
import com.d1gaming.library.user.UserTournament;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteBatch;
import com.google.cloud.firestore.WriteResult;

@Service
public class TeamTournamentService {

	@Autowired
	private Firestore firestore;
	
	@Autowired
	private TournamentService tournamentService;
	
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
		Tournament tournamentOnDB = tournamentSnapshot.toObject(Tournament.class);
		if(tournamentSnapshot.exists() && (tournamentOnDB.getTournamentStatus().equals(TournamentStatus.ACTIVE) 
											|| tournamentOnDB.getTournamentStatus().equals(TournamentStatus.IN_PROGRESS)
											|| tournamentOnDB.getTournamentStatus().equals(TournamentStatus.TERMINATED))) {
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
			return tournamentService.getAllTournamentsById(
															queryForDocuments.getDocuments()
															.stream()
															.map(document -> document.toObject(TeamCodTournament.class))
															.map(teamCodTournament -> teamCodTournament.getTeamCodTournamentId())
															.collect(Collectors.toList())
														);
							
		}
		return new ArrayList<>();
	}
	
	public List<Tournament> getAllFifaTournamentsFromTeam(String teamId) throws InterruptedException, ExecutionException {
		if(isActive(teamId)) {
			QuerySnapshot queryForTournaments = getFifaTournamentsSubcollectionFromTeam(teamId).get().get();
			return this.tournamentService.getAllTournamentsById(
															queryForTournaments.getDocuments()
															.stream()
															.map(document -> document.toObject(TeamFifaTournament.class))
															.map(teamFifaTournament -> teamFifaTournament.getTeamTournamentId())
															.collect(Collectors.toList())
														);
		}
		return new ArrayList<>();
	}
	
	public List<Match> getAllInactiveFifaMatchesFromTournament(String teamId, String tournamentId) throws InterruptedException, ExecutionException{
		if(isActive(teamId) && isActiveTournament(tournamentId)) {
			return getFifaTournamentsSubcollectionFromTeam(teamId).get().get()
															.getDocuments()
															.stream()
															.map(document -> document.toObject(TeamFifaTournament.class))
															.filter(teamFifaTournament -> teamFifaTournament.getTeamTournamentId().equals(tournamentId))
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
															.filter(teamCodTournament -> teamCodTournament.getTeamTournamentId().equals(tournamentId))
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
															.filter(teamFifaTournament -> teamFifaTournament.getTeamTournamentId().equals(tournamentId))
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
					.filter(teamCodTournament -> teamCodTournament.getTeamTournamentId().equals(tournamentId))
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
																	.filter(teamCodTournament -> teamCodTournament.getTeamTournamentId().equals(tournamentId))
																	.collect(Collectors.toList());
			return Optional.of(this.tournamentService.getTournamentById(
																teamCodTournaments.get(0).getTeamTournamentId()
																		).get());
		}
		return null; 
	}
	
	public Optional<Tournament> getCodTournamentFromTeamById(String teamId, String tournamentId) throws InterruptedException, ExecutionException {
		if(isActiveTournament(teamId) && isActive(tournamentId)) {
			List<TeamFifaTournament> teamFifaTournaments = getTeamReference(teamId).collection("teamFifaTournaments").get().get()
																	.getDocuments()
																	.stream()
																	.map(document -> document.toObject(TeamFifaTournament.class))
																	.filter(teamFifaTournament -> teamFifaTournament.getTeamTournamentId().equals(tournamentId))
																	.collect(Collectors.toList());
			return Optional.of(
								this.tournamentService.getTournamentById(teamFifaTournaments.get(0).getTeamTournamentId()).get()
							);
		}
		return null;
	}
	
	
	public String addTeamToFifaTournament(Team team, Tournament tournament) throws InterruptedException, ExecutionException {
		if(isActiveTournament(tournament.getTournamentId()) && isActive(team.getTeamId())) {
			DocumentReference tourneyReference = getTournamentReference(tournament.getTournamentId());
			DocumentReference teamModeratorReference = firestore.collection("users").document(team.getTeamModerator().getUserId());
			Team teamOnDB = getTeamReference(team.getTeamId()).get().get().toObject(Team.class);
			Tournament tournamentOnDB = tourneyReference.get().get().toObject(Tournament.class);			
			List<Match> teamMatches = new ArrayList<>();
			TeamFifaTournament teamTournamentSubdocument = new TeamFifaTournament(tournamentOnDB.getTournamentId(), teamMatches, 0, 0, 0, 0, 0, 0, 0, 0, TeamTournamentStatus.ACTIVE);
			
			boolean isAlreadyPartOfTournament = getFifaTournamentsSubcollectionFromTeam(team.getTeamId()).get().get()
																	.getDocuments()
																	.stream()
																	.map(document -> document.toObject(TeamFifaTournament.class))
																	.anyMatch(teamFifaTournament -> teamFifaTournament.getTeamTournamentId().equals(tournament.getTournamentId()));
			if(!isAlreadyPartOfTournament) {				
				User userTeamModerator = teamModeratorReference.get().get().toObject(User.class);
				boolean isTourneyAdmin = userTeamModerator.getUserId().equals(tournament.getTournamentModeratorId());
				int teamModeratorTokens = userTeamModerator.getUserTokens();
				int currentNumberOfTeamsInTournament = tournamentOnDB.getTournamentNumberOfTeams();
				if((tournament.getTournamentLimitNumberOfTeams() > currentNumberOfTeamsInTournament && teamModeratorTokens >= tournament.getTournamentEntryFee()) || isTourneyAdmin) {
					WriteBatch batch = firestore.batch();
					DocumentReference addedDocumentToTeamFifaTournamentsSubcollection = getFifaTournamentsSubcollectionFromTeam(team.getTeamId()).add(teamTournamentSubdocument).get();
					String documentId = addedDocumentToTeamFifaTournamentsSubcollection.getId();
					List<Team> tournamentTeamList = tournamentOnDB.getTournamentTeams();
					if(tournamentOnDB.getTournamentFormat().equals(TournamentFormat.PvP)) {
						List<Team> tournamentLeaderboardForLeague = tournamentOnDB.getTournamentLeaderboardForLeague();
						tournamentLeaderboardForLeague.add(teamOnDB);
					}
					tournamentTeamList.add(teamOnDB);
					List<User> teamUsers = team.getTeamUsers();
					teamUsers
						.stream()
						.forEach(user -> {
							try {
								addTournamentToUser(user, team, tournamentOnDB);
							} catch (InterruptedException | ExecutionException e) {
								e.printStackTrace();
							}
						});
					Stack<Team> tournamentTeamStack = tournamentOnDB.getTournamentTeamBracketStack();
					boolean isWrittenBatch = false;
					if(tournamentTeamStack.isEmpty()) {
						tournamentTeamStack.add(teamOnDB);
					}
					else {
						Team localTeam = tournamentTeamStack.pop();
						WriteBatch matchBatch = firestore.batch();
						matchBatch.update(addedDocumentToTeamFifaTournamentsSubcollection, "teamTournamentId", documentId);
						matchBatch.update(tourneyReference, "tournamentTeamBracketStack", tournamentTeamStack);
						matchBatch.commit().get();
						addMatchToFifaTeams(localTeam, teamOnDB, tournamentOnDB);
						isWrittenBatch = true;
					}
					if(!isWrittenBatch) {
						batch.update(addedDocumentToTeamFifaTournamentsSubcollection, "teamTournamentId", documentId);
						batch.update(tourneyReference, "tournamentTeamBracketStack", tournamentTeamStack);
					}
					batch.update(tourneyReference, "tournamentTeams", tournamentTeamList);
					batch.update(tourneyReference, "tournamentNumberOfTeams", FieldValue.increment(1));
					if(!isTourneyAdmin) {						
						batch.update(teamModeratorReference, "userTokens", FieldValue.increment(-tournament.getTournamentEntryFee()));
					}
					batch.commit().get();
					return "Team added successfully to tournament.";
				
				}
				return "Tournament is already full."; 	
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
			TeamCodTournament teamCodTournamentSubdocument = new TeamCodTournament(tournamentOnDB.getTournamentId(), 0 , teamMatches, 0, 0, 0, 0, 0, TeamTournamentStatus.ACTIVE);
			
			boolean isAlreadyPartOfTournament = getCodTournamentsSubcollectionFromTeam(team.getTeamId()).get().get()
																	.getDocuments()
																	.stream()
																	.map(document -> document.toObject(TeamCodTournament.class))
																	.anyMatch(teamCodTournament -> teamCodTournament.getTeamTournamentId().equals(tournament.getTournamentName())); 
			if(!isAlreadyPartOfTournament) {
				User userTeamModerator = userTeamModeratorReference.get().get().toObject(User.class);
				boolean isTourneyAdmin = userTeamModerator.getUserId().equals(tournament.getTournamentModeratorId());
				int teamModeratorTokens = userTeamModerator.getUserTokens();
				int tournamentCurrentNumberOfTeams = tournamentOnDB.getTournamentNumberOfTeams();
				if((teamModeratorTokens >= tournament.getTournamentEntryFee() && tournamentCurrentNumberOfTeams < tournament.getTournamentLimitNumberOfTeams()) || isTourneyAdmin) {
					DocumentReference addedDocumentToTeamCodTournaments = getCodTournamentsSubcollectionFromTeam(team.getTeamId()).add(teamCodTournamentSubdocument).get();
					String documentId = addedDocumentToTeamCodTournaments.getId();
					List<Team> tournamentTeamList = tournamentOnDB.getTournamentTeams();
					tournamentTeamList.add(teamOnDB);
					List<User> teamUsers = teamOnDB.getTeamUsers();
					teamUsers
						.stream()
						.forEach(user -> {
							try {
								addTournamentToUser(user, team, tournamentOnDB);
							} catch (InterruptedException | ExecutionException e) {
								e.printStackTrace();
							}
						});
					
					WriteBatch batch = firestore.batch();
					if(tournamentOnDB.getTournamentFormat().equals(TournamentFormat.PvP)) {
						List<Team> tournamentLeaderboardForLeague = tournamentOnDB.getTournamentLeaderboardForLeague();
						tournamentLeaderboardForLeague.add(teamOnDB);
						batch.update(tourneyReference, "tournamentLeaderboardForLeague", tournamentLeaderboardForLeague);
					}
					Stack<Team> tournamentTeamStack = tournamentOnDB.getTournamentTeamBracketStack();
					boolean isWrittenBatch = false;
					if(tournamentTeamStack.isEmpty()) {
						tournamentTeamStack.add(teamOnDB);
					}
					else {
						Team localTeam = tournamentTeamStack.pop();
						WriteBatch matchBatch = firestore.batch();
						matchBatch.update(addedDocumentToTeamCodTournaments, "teamCodTournamentId", documentId);
						matchBatch.update(tourneyReference, "tournamentTeamBracketStack", tournamentTeamStack);
						matchBatch.commit().get();
						addMatchToCodTeams(localTeam, teamOnDB, tournamentOnDB);
						isWrittenBatch = true;
					}
					if(!isWrittenBatch) {
						batch.update(addedDocumentToTeamCodTournaments, "teamCodTournamentId", documentId);
						batch.update(tourneyReference, "tournamentTeamBracketStack", tournamentTeamStack);
					}
					if(!isTourneyAdmin) {
						batch.update(userTeamModeratorReference, "userTokens", FieldValue.increment(-tournament.getTournamentEntryFee()));
					}
					batch.update(tourneyReference, "tournamentNumberOfTeams", FieldValue.increment(1));
					batch.update(tourneyReference, "tournamentTeams", tournamentTeamList);
					batch.commit().get();
					return "Team added successfully to tournament.";
				}
				return "Tournament is already full.";
			}
			return "Team is already part of tournament.";
		}
		return "Not found.";
	}
	
	//Add Tournament to userTournament.
	public String addTournamentToUser(User user, Team team, Tournament tournament) throws InterruptedException, ExecutionException {
		if(isActiveUser(user.getUserId()) && isActive(team.getTeamId()) && isActiveTournament(tournament.getTournamentId())) {
			List<Match> userTournamentMatches = new ArrayList<>();
			UserTournament userTournament = new UserTournament(tournament.getTournamentId(), team, 0, 0, userTournamentMatches, TeamTournamentStatus.ACTIVE);
			DocumentReference userReference = firestore.collection("users").document(user.getUserId());
			User userOnDB = userReference.get().get().toObject(User.class);
			List<UserTournament> userTournamentList = userOnDB.getUserTournaments();
			userTournamentList.add(userTournament);
			WriteBatch batch = firestore.batch();
			batch.update(userReference, "userTournaments", userTournamentList);
			batch.commit().get();
			return "User tournament added.";
		}
		return "Not found.";
	}
	
	public String removeTeamFromCodTournament(Team team, Tournament tournament) throws InterruptedException, ExecutionException {
		//&& !tournament.getStartedTournamentStatus()
		if(isActiveTournament(tournament.getTournamentId()) && isActive(team.getTeamId()) ) {
			DocumentReference userTeamModeratorReference = firestore.collection("users").document(team.getTeamModerator().getUserId());
			DocumentReference tournamentReference = getTournamentReference(tournament.getTournamentId());
			Tournament tournamentOnDB = tournamentReference.get().get().toObject(Tournament.class);
			Team teamOnDB = getTeamReference(team.getTeamId()).get().get().toObject(Team.class);
			List<Team> tournamentTeamList = tournamentOnDB.getTournamentTeams();
			boolean isPartOfTournament = tournamentTeamList
										.stream()
										.anyMatch(tournamentTeam -> tournamentTeam.getTeamName().equals(team.getTeamName()));
			if(isPartOfTournament) {
				List<TeamCodTournament> teamCodTournamentList = getCodTournamentsSubcollectionFromTeam(team.getTeamId()).get().get()
														.getDocuments()
														.stream()
														.map(document -> document.toObject(TeamCodTournament.class))
														.filter(teamCodTournament -> teamCodTournament.getTeamTournamentId().equals(tournament.getTournamentId()))
														.collect(Collectors.toList()); 
				
				List<Team> newTournamentTeamList = tournamentTeamList
						.stream()
						.filter(tournamentTeam -> !tournamentTeam.getTeamName().equals(team.getTeamName()))
						.collect(Collectors.toList());				
				
				WriteBatch batch = firestore.batch();
				List<User> teamUsers = teamOnDB.getTeamUsers();
				teamUsers
					.stream()
					.forEach(user -> {
						try {
							removeTournamentFromUser(user, teamOnDB, tournament);
						} catch (InterruptedException | ExecutionException e) {
							e.printStackTrace();
						}
					});
				TeamCodTournament teamCodTournament = teamCodTournamentList.get(0);
				List<Match> teamCodTournamentMatchesList = teamCodTournament.getTeamCodTournamentMatches();
				Stack<Team> tournamentTeamStack = tournamentOnDB.getTournamentTeamBracketStack();
				if(!teamCodTournamentMatchesList.isEmpty()) {
					Match teamMatchOnTournamentToArrange = teamCodTournamentMatchesList.get(0);
					getTournamentReference(tournament.getTournamentId()).collection("tournamentMatches").document(teamMatchOnTournamentToArrange.getMatchId()).delete();
					Team teamToAssignMatchTo = teamMatchOnTournamentToArrange.getMatchAwayTeam().getTeamId().equals(team.getTeamId()) ? teamMatchOnTournamentToArrange.getMatchLocalTeam() : teamMatchOnTournamentToArrange.getMatchAwayTeam();
					removeMatchFromFifaTeams(teamMatchOnTournamentToArrange, teamToAssignMatchTo, tournamentOnDB);
					if(tournamentTeamStack.isEmpty()) {
						tournamentTeamStack.push(teamToAssignMatchTo);
					}
					else {
						Team poppedTeam = tournamentTeamStack.pop();
						addMatchToCodTeams(poppedTeam, teamToAssignMatchTo, tournamentOnDB);
					}
				}
				else {					
					tournamentTeamStack.pop();
				}
				if(!teamCodTournamentList.isEmpty()) {
					getCodTournamentsSubcollectionFromTeam(team.getTeamId()).document(teamCodTournament.getTeamCodTournamentId()).delete();
				}
				batch.update(tournamentReference, "tournamentTeams", newTournamentTeamList);
				batch.update(tournamentReference, "tournamentNumberOfTeams", FieldValue.increment(-1));
				batch.update(userTeamModeratorReference, "userTokens", FieldValue.increment(tournament.getTournamentEntryFee()));
				batch.update(tournamentReference, "tournamentTeamBracketStack", tournamentTeamStack);
				batch.commit().get();
				return "Team removed successfully.";
			}							
		}
		return "Not found.";
	}
	
	public String removeTeamFromFifaTournament(Team team, Tournament tournament) throws InterruptedException, ExecutionException {
		if(isActiveTournament(tournament.getTournamentId()) && isActive(team.getTeamId()) && !tournament.isStartedTournament()) {
			DocumentReference userTeamModeratorReference = firestore.collection("users").document(team.getTeamModerator().getUserId());
			DocumentReference tournamentReference = getTournamentReference(tournament.getTournamentId());
			Tournament tournamentOnDB = tournamentReference.get().get().toObject(Tournament.class);
			Team teamOnDB = getTeamReference(team.getTeamId()).get().get().toObject(Team.class);
			List<Team> tournamentTeamList = tournamentOnDB.getTournamentTeams();
			boolean isPartOfTournament = tournamentTeamList
										.stream()
										.anyMatch(tournamentTeam -> tournamentTeam.getTeamName().equals(team.getTeamName()));
			if(isPartOfTournament) {
				List<Team> newTournamentTeamList = tournamentTeamList
						.stream()
						.filter(tournamentTeam -> !tournamentTeam.getTeamName().equals(team.getTeamName()))
						.collect(Collectors.toList());
				
					
				List<TeamFifaTournament> teamFifaTournamentsList = getFifaTournamentsSubcollectionFromTeam(team.getTeamId()).get().get()
														.getDocuments()
														.stream()
														.map(document -> document.toObject(TeamFifaTournament.class))
														.filter(teamTournament -> teamTournament.getTeamTournamentId().equals(tournament.getTournamentId()))
														.collect(Collectors.toList());
				WriteBatch batch = firestore.batch();
				List<User> teamUsers = teamOnDB.getTeamUsers();
				teamUsers
					.stream()
					.forEach(user -> {
						try {
							removeTournamentFromUser(user, teamOnDB, tournament);
						} catch (InterruptedException | ExecutionException e) {
							e.printStackTrace();
						}
					});
				Stack<Team> tournamentTeamStack = tournamentOnDB.getTournamentTeamBracketStack();
				TeamFifaTournament teamFifaTournament = teamFifaTournamentsList.get(0);
				
				List<Match> teamCodTournamentMatchesList = teamFifaTournament.getTeamTournamentMatches();
				if(!teamCodTournamentMatchesList.isEmpty()) {
					Match teamMatchToArrange = teamCodTournamentMatchesList.get(0);
					Team teamToAssignMatchTo = teamMatchToArrange.getMatchAwayTeam().getTeamId().equals(team.getTeamId()) ? teamMatchToArrange.getMatchLocalTeam() : teamMatchToArrange.getMatchAwayTeam();
					removeMatchFromCodTeams(teamMatchToArrange, teamToAssignMatchTo, tournamentOnDB);
					if(tournamentTeamStack.isEmpty()) {
						tournamentTeamStack.push(teamToAssignMatchTo);
					}
					else {
						Team poppedTeam = tournamentTeamStack.pop();
						addMatchToFifaTeams(poppedTeam, teamToAssignMatchTo, tournamentOnDB);	
					}
				}
				else {					
					tournamentTeamStack.pop();
				}
				if(!teamFifaTournamentsList.isEmpty()) {
					getFifaTournamentsSubcollectionFromTeam(team.getTeamId()).document(teamFifaTournament.getTeamTournamentId()).delete();
				}
				batch.update(tournamentReference, "tournamentTeams", newTournamentTeamList);
				batch.update(tournamentReference, "tournamentNumberOfTeams", FieldValue.increment(-1));
				batch.update(userTeamModeratorReference, "userTokens", FieldValue.increment(tournament.getTournamentEntryFee()));
				batch.update(tournamentReference, "tournamentTeamBracketStack", tournamentTeamStack);
				batch.commit().get();
				return "Team removed from tournament";
			}							
		}
		return "Not found.";
	}
	
	public void removeMatchFromCodTeams(Match match, Team team, Tournament tournament) throws InterruptedException, ExecutionException {
		Team teamOnDB = getTeamReference(team.getTeamId()).get().get().toObject(Team.class);
		List<TeamCodTournament> teamCodTournamentsWithTournament = getTeamReference(team.getTeamId()).collection("teamCodTournaments").get().get()
																		.getDocuments()
																		.stream()
																		.map(document -> document.toObject(TeamCodTournament.class))
																		.filter(teamCodTournament -> teamCodTournament.getTeamTournamentId().equals(tournament.getTournamentId()))
																		.collect(Collectors.toList());
		if(!teamCodTournamentsWithTournament.isEmpty()) {			
			TeamCodTournament teamCodTournament = teamCodTournamentsWithTournament.get(0);
			DocumentReference teamCodTournamentReference = getTeamReference(team.getTeamId()).collection("teamCodTournaments").document(teamCodTournament.getTeamCodTournamentId());
			List<Match> newTeamCodTournamentMatches = teamCodTournament.getTeamCodTournamentMatches()
																		.stream()
																		.filter(teamMatch -> !teamMatch.getMatchId().equals(match.getMatchId()))
																		.collect(Collectors.toList());
			WriteBatch batch = firestore.batch();
			batch.update(teamCodTournamentReference, "teamCodTournamentMatches", newTeamCodTournamentMatches);
			batch.commit().get();
			List<User> teamUsers = teamOnDB.getTeamUsers();
			teamUsers
				.stream()
				.forEach(user -> {
					try {
						removeMatchFromUserTournament(match, user, tournament);
					} catch (InterruptedException | ExecutionException e) {
						e.printStackTrace();
					}
				});
		}
	}
	
	public void removeMatchFromFifaTeams(Match match, Team team, Tournament tournament) throws InterruptedException, ExecutionException {
		Team teamOnDB = getTeamReference(team.getTeamId()).get().get().toObject(Team.class);
		List<TeamFifaTournament> teamFifaTournamentWithTournament = getFifaTournamentsSubcollectionFromTeam(team.getTeamId()).get().get()
																		.getDocuments()
																		.stream()
																		.map(document -> document.toObject(TeamFifaTournament.class))
																		.filter(teamFifaTournament -> teamFifaTournament.getTeamTournamentId().equals(tournament.getTournamentId()))
																		.collect(Collectors.toList());
		if(!teamFifaTournamentWithTournament.isEmpty()) {
			TeamFifaTournament teamFifaTournament = teamFifaTournamentWithTournament.get(0);
			DocumentReference teamFifaTournamentReference = getFifaTournamentsSubcollectionFromTeam(team.getTeamId()).document(teamFifaTournament.getTeamTournamentId());
			List<Match> newTeamFifaTournamentMatches = teamFifaTournament.getTeamTournamentMatches()
																		.stream()
																		.filter(teamMatch -> teamMatch.getMatchId().equals(match.getMatchId()))
																		.collect(Collectors.toList());
			WriteBatch batch = firestore.batch();
			batch.update(teamFifaTournamentReference, "teamTournamentMatches", newTeamFifaTournamentMatches);
			batch.commit().get();
			List<User> teamUsers = teamOnDB.getTeamUsers();
				teamUsers
					.stream()
					.forEach(user -> {
						try {
							removeMatchFromUserTournament(match, user, tournament);
						} catch (InterruptedException | ExecutionException e) {
							e.printStackTrace();
						}
					});
		}
		
	}
	
	public void removeMatchFromUserTournament(Match match, User user, Tournament tournament) throws InterruptedException, ExecutionException {
		DocumentReference userReference = firestore.collection("users").document(user.getUserId());
		List<UserTournament> userTournamentList = user.getUserTournaments();
		List<UserTournament> userTournamentsWithTournament = user.getUserTournaments()
																.stream()
																.filter(userTournament -> userTournament.getUserTournamentId().equals(tournament.getTournamentId()))
																.collect(Collectors.toList());
		if(!userTournamentsWithTournament.isEmpty()) {
			UserTournament userTournament = userTournamentsWithTournament.get(0);
			int indexOfUserTournament = userTournamentList.indexOf(userTournament);
			userTournamentList.remove(indexOfUserTournament);
			List<Match> userTournamentMatches = userTournament.getUserTournamentMatches();
			List<Match> newUserTournamentMatches = userTournamentMatches
														.stream()
														.filter(userMatch -> !userMatch.getMatchId().equals(match.getMatchId()))
														.collect(Collectors.toList());
			userTournament.setUserTournamentMatches(newUserTournamentMatches);
			userTournamentList.add(userTournament);
			WriteBatch batch = firestore.batch();
			batch.update(userReference,"userTournaments", userTournamentList);
			batch.commit().get();
		}
	}
	
	public String removeTournamentFromUser(User user, Team team, Tournament tournament) throws InterruptedException, ExecutionException {
		if(isActiveUser(user.getUserId()) && isActive(team.getTeamId()) && isActiveTournament(tournament.getTournamentId())) {
			DocumentReference userReference = firestore.collection("users").document(user.getUserId());
			User userOnDB = userReference.get().get().toObject(User.class);
			List<UserTournament> userTournaments = userOnDB.getUserTournaments();
			List<UserTournament> userTournamentsListWithTournament = userTournaments
																		.stream()
																		.filter(userTournament -> userTournament.getUserTournamentId().equals(tournament.getTournamentId()))
																		.collect(Collectors.toList());
			if(!userTournamentsListWithTournament.isEmpty()) {				
				List<UserTournament> userTournamentsWithoutTournament = userTournaments
																		.stream()
																		.filter(userTournament -> !userTournament.getUserTournamentId().equals(tournament.getTournamentId()))
																		.collect(Collectors.toList());
						
				WriteBatch batch = firestore.batch();
				batch.update(userReference, "userTournaments", userTournamentsWithoutTournament);
				batch.commit().get();
				return "Removed Tournament from user.";
			}
		}
		return "Not found.";
	}
	
	public Match addMatchToFifaTeams(Team localTeam, Team awayTeam, Tournament matchTournament) throws InterruptedException, ExecutionException {
		if(isActive(localTeam.getTeamId()) && isActive(awayTeam.getTeamId()) && isActiveTournament(matchTournament.getTournamentId())) {
			Team localTeamOnDB = getTeamReference(localTeam.getTeamId()).get().get().toObject(Team.class);
			Team awayTeamOnDB = getTeamReference(awayTeam.getTeamId()).get().get().toObject(Team.class);
			List<TeamFifaTournament> localTeamFifaTournamentList = getFifaTournamentsSubcollectionFromTeam(localTeam.getTeamId()).get().get()
																		.getDocuments()
																		.stream()
																		.map(document -> document.toObject(TeamFifaTournament.class))
																		.filter(teamFifaTournament -> teamFifaTournament.getTeamTournamentId().equals(matchTournament.getTournamentId()))
																		.collect(Collectors.toList());
			List<TeamFifaTournament> awayTeamFifaTournamentList = getFifaTournamentsSubcollectionFromTeam(awayTeam.getTeamId()).get().get()
																		.getDocuments()
																		.stream()
																		.map(document -> document.toObject(TeamFifaTournament.class))
																		.filter(teamFifaTournament -> teamFifaTournament.getTeamTournamentId().equals(matchTournament.getTournamentId()))
																		.collect(Collectors.toList());
			TeamFifaTournament localTeamTournament = localTeamFifaTournamentList.get(0);
			TeamFifaTournament awayTeamTournament = awayTeamFifaTournamentList.get(0);
			DocumentReference localTeamFifaTournamentReference = getFifaTournamentReferenceFromTeam(localTeam.getTeamId(), localTeamTournament.getTeamTournamentId());
			DocumentReference awayTeamFifaTournamentReference = getFifaTournamentReferenceFromTeam(awayTeam.getTeamId(), awayTeamTournament.getTeamTournamentId());
			List<Match> localTeamFifaTournamentMatchesList = localTeamTournament.getTeamTournamentMatches();
			List<Match> awayTeamFifaTournamentMatchesList = awayTeamTournament.getTeamTournamentMatches();
			Match match = new Match(matchTournament, localTeamOnDB, awayTeamOnDB, 0, 0, MatchStatus.ACTIVE, matchTournament.getTournamentGame().equals("Fifa") ? "Fifa" : "Call Of Duty");
			DocumentReference addedDocument = getTournamentReference(matchTournament.getTournamentId()).collection("tournamentMatches").add(match).get();
			String matchDocumentId = addedDocument.getId();
			match.setMatchId(matchDocumentId);
			localTeamFifaTournamentMatchesList.add(match);
			awayTeamFifaTournamentMatchesList.add(match);
			WriteBatch batch = firestore.batch();
			batch.update(localTeamFifaTournamentReference, "teamTournamentMatches", localTeamFifaTournamentMatchesList);
			batch.update(awayTeamFifaTournamentReference, "teamTournamentMatches", awayTeamFifaTournamentMatchesList);
			batch.update(addedDocument, "matchId", matchDocumentId);
			batch.commit().get();
			List<User> localTeamUsersList = localTeamOnDB.getTeamUsers();
			List<User> awayTeamUsersList = awayTeamOnDB.getTeamUsers();
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
			
			return match;
		}
		return null;
	}
	
	public Match addMatchToCodTeams(Team localTeam, Team awayTeam, Tournament matchTournament) throws InterruptedException, ExecutionException {
		if(isActive(localTeam.getTeamId()) && isActive(awayTeam.getTeamId()) && isActiveTournament(matchTournament.getTournamentId())) {
			Team localTeamOnDB = getTeamReference(localTeam.getTeamId()).get().get().toObject(Team.class);
			Team awayTeamOnDB = getTeamReference(awayTeam.getTeamId()).get().get().toObject(Team.class);
			List<TeamCodTournament> localTeamCodTournamentsList = getCodTournamentsSubcollectionFromTeam(localTeam.getTeamId()).get().get()
																		.getDocuments()
																		.stream()
																		.map(document -> document.toObject(TeamCodTournament.class))
																		.filter(teamCodTournament -> teamCodTournament.getTeamTournamentId().equals(matchTournament.getTournamentId()))
																		.collect(Collectors.toList());
			List<TeamCodTournament> awayTeamCodTournamentsList = getCodTournamentsSubcollectionFromTeam(awayTeam.getTeamId()).get().get()
																		.getDocuments()
																		.stream()
																		.map(document -> document.toObject(TeamCodTournament.class))
																		.filter(teamCodTournament -> teamCodTournament.getTeamTournamentId().equals(matchTournament.getTournamentId()))
																		.collect(Collectors.toList());
			TeamCodTournament localTeamCodTournament = localTeamCodTournamentsList.get(0);
			TeamCodTournament awayTeamCodTournament = awayTeamCodTournamentsList.get(0);
			DocumentReference localTeamCodTournamentReference = getCodTournamentReferenceFromTeam(localTeam.getTeamId(), localTeamCodTournament.getTeamCodTournamentId());
			DocumentReference awayTeamCodTournamentReference = getCodTournamentReferenceFromTeam(awayTeam.getTeamId(), awayTeamCodTournament.getTeamCodTournamentId());
			List<Match> localTeamCodTournamentMatchesList = localTeamCodTournament.getTeamCodTournamentMatches();
			List<Match> awayTeamCodTournamentMatchesList = awayTeamCodTournament.getTeamCodTournamentMatches();
			Match match = new Match(matchTournament, localTeamOnDB, awayTeamOnDB, 0, 0, MatchStatus.ACTIVE, matchTournament.getTournamentGame().equals("Fifa") ? "Fifa" : "Call Of Duty");
			DocumentReference matchTourneyDocument = getTournamentReference(matchTournament.getTournamentId()).collection("tournamentMatches").add(match).get();
			String matchDocumentId = matchTourneyDocument.getId();
			match.setMatchId(matchDocumentId);
			localTeamCodTournamentMatchesList.add(match);
			awayTeamCodTournamentMatchesList.add(match);
			
			WriteBatch batch = firestore.batch();
			batch.update(localTeamCodTournamentReference, "teamCodTournamentMatches", localTeamCodTournamentMatchesList);
			batch.update(awayTeamCodTournamentReference, "teamCodTournamentMatches", awayTeamCodTournamentMatchesList);
			batch.update(matchTourneyDocument, "matchId", matchDocumentId);
			batch.commit().get();
			List<User> localTeamUsersList = localTeamOnDB.getTeamUsers();
			List<User> awayTeamUsersList = awayTeamOnDB.getTeamUsers();
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
			
			return match;
		}
		return null;
	}
	
	public String addMatchToUserInTournament(User user, Tournament tournament, Match match) throws InterruptedException, ExecutionException {
		if(isActiveUser(user.getUserId())) {
			DocumentReference userReference = firestore.collection("users").document(user.getUserId());
			User userOnDB = userReference.get().get().toObject(User.class);
			List<UserTournament> userTournamentList = userOnDB
														.getUserTournaments()
														.stream()
														.filter(userTournament -> userTournament.getUserTournamentId().equals(tournament.getTournamentId()))
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
		
	public String uploadCodMatchResult(Match match, String tournamentId, Team team) throws InterruptedException, ExecutionException {
		if(isActiveTournament(tournamentId)) {
			Tournament tournamentOnDB = getTournamentReference(tournamentId).get().get().toObject(Tournament.class);
			DocumentReference tournamentReference = getTournamentReference(tournamentId);
			Match matchOnDB = tournamentReference.collection("tournamentMatches").document(match.getMatchId()).get().get().toObject(Match.class);
			if(!matchOnDB.getMatchStatus().equals(MatchStatus.DISPUTED)) {
				boolean isLocalTeam = match.getMatchLocalTeam().getTeamId().equals(team.getTeamId()); 
				if(!isValidMatchUploadedStatus(tournamentOnDB, match, matchOnDB)) {
					match.setMatchStatus(MatchStatus.DISPUTED);
					match.setDisputedMatch(true);
					if(isLocalTeam) {
						match.setLocalTeamUploaded(true);
					}
					else {
						match.setAwayTeamUploaded(true);
					}
					tournamentReference.collection("tournamentMatches").document(match.getMatchId()).set(match).get();
					return "Match disputed";
				}
				if(!match.isLocalTeamUploaded() && !match.isAwayTeamUploaded()) {
					if(isLocalTeam) {
						match.setLocalTeamUploaded(true);
					}
					else {
						match.setAwayTeamUploaded(true);
					}
					tournamentReference.collection("tournamentMatches").document(match.getMatchId()).set(match).get();
					return "Results recorded";
				}
				if(isLocalTeam) {
					match.setLocalTeamUploaded(true);
				}
				else {
					match.setAwayTeamUploaded(true);
				}
			}
			if(matchOnDB.getMatchStatus().equals(MatchStatus.DISPUTED)) {
				addResolvedStatusToDisputedMatch(match.getMatchId());
				match.setDisputedMatch(false);
				match.setHasImage(false);
			}
			match.setMatchStatus(MatchStatus.INACTIVE);
			WriteResult resultFromReplacement = getTournamentReference(tournamentId).collection("tournamentMatches").document(match.getMatchId()).set(match).get();
			System.out.println("Replaced Document: " + resultFromReplacement.getUpdateTime());
			addInvalidStatusToTeamCodTournamentMatch(tournamentId, match.getMatchLocalTeam(), match);
			addInvalidStatusToTeamCodTournamentMatch(tournamentId, match.getMatchAwayTeam(), match);
			Stack<Team> tournamentTeamStack = tournamentOnDB.getTournamentTeamBracketStack();
			Team winningTeam = match.getMatchWinningTeam();
			Team losingTeam = winningTeam.getTeamId().equals(match.getMatchLocalTeam().getTeamId()) ? match.getMatchAwayTeam() : match.getMatchLocalTeam();
			addResultToTeams(winningTeam, losingTeam, tournamentOnDB);
			if(tournamentOnDB.getTournamentFormat().equals(TournamentFormat.PvP)) {				
				
				List<Team> tournamentLeaderboardForLeague = tournamentOnDB.getTournamentLeaderboardForLeague()
																.stream()
																.filter(tournamentTeam -> !tournamentTeam.getTeamId().equals(losingTeam.getTeamId()))
																.collect(Collectors.toList());
				WriteBatch batch = firestore.batch();
				batch.update(tournamentReference, "tournamentLeaderboardForLeague", tournamentLeaderboardForLeague);
				batch.commit().get();
				if(tournamentLeaderboardForLeague.size() == 1){
					terminateTournament(tournamentOnDB, winningTeam, losingTeam);
					return "Tournament terminated.";
				}
			}
			if(tournamentTeamStack.isEmpty()) {
				tournamentTeamStack.add(winningTeam);
			}
			else {
				Team poppedTeam = tournamentTeamStack.pop();
				addMatchToCodTeams(poppedTeam, winningTeam, tournamentOnDB);
			}
			WriteBatch batch = firestore.batch();
			batch.update(tournamentReference, "tournamentTeamBracketStack", tournamentTeamStack);
			batch.commit().get();
			return "Match results uploaded successfully.";
		}
		return "Not found.";
	}
	
	//Update Tournament results.
	public String uploadFifaMatchResult(Match match, String tournamentId, Team team) throws InterruptedException, ExecutionException {
		if(isActiveTournament(tournamentId)) {
			DocumentReference tournamentReference = getTournamentReference(tournamentId);
			Tournament tournamentOnDB = getTournamentReference(tournamentId).get().get().toObject(Tournament.class);
			Match matchOnDB = tournamentReference.collection("tournamentMatches").document(match.getMatchId()).get().get().toObject(Match.class);
			if(!matchOnDB.getMatchStatus().equals(MatchStatus.DISPUTED)) {
				boolean isLocalTeam = match.getMatchLocalTeam().getTeamId().equals(team.getTeamId()); 
				if(!isValidMatchUploadedStatus(tournamentOnDB, match, matchOnDB)) {
					match.setMatchStatus(MatchStatus.DISPUTED);
					match.setDisputedMatch(true);
					if(isLocalTeam) {
						match.setLocalTeamUploaded(true);
					}
					else {
						match.setAwayTeamUploaded(true);
					}
					tournamentReference.collection("tournamentMatches").document(match.getMatchId()).set(match).get();
					return "Match disputed";
				}
				if(!match.isLocalTeamUploaded() && !match.isAwayTeamUploaded()) {
					if(isLocalTeam) {
						match.setLocalTeamUploaded(true);
					}
					else {
						match.setAwayTeamUploaded(true);
					}
					tournamentReference.collection("tournamentMatches").document(match.getMatchId()).set(match).get();
					return "Results recorded";
				}
				if(isLocalTeam) {
					match.setLocalTeamUploaded(true);
				}
				else {
					match.setAwayTeamUploaded(true);
				}
			}
			if(matchOnDB.getMatchStatus().equals(MatchStatus.DISPUTED)) {
				addResolvedStatusToDisputedMatch(match.getMatchId());
				match.setDisputedMatch(false);
				match.setHasImage(false);
			}
			match.setMatchStatus(MatchStatus.INACTIVE);
			WriteResult resultFromReplacement = getTournamentReference(tournamentId).collection("tournamentMatches").document(match.getMatchId()).set(match).get();
			System.out.println("Replaced Document: " + resultFromReplacement.getUpdateTime());
			addInvalidStatusToTeamFifaTournamentMatch(tournamentId, match.getMatchAwayTeam(), match);
			addInvalidStatusToTeamFifaTournamentMatch(tournamentId, match.getMatchLocalTeam(), match);
			Stack<Team> tournamentTeamStack = tournamentOnDB.getTournamentTeamBracketStack();
			Team winningTeam = match.getMatchWinningTeam();
			Team losingTeam = winningTeam.getTeamId().equals(match.getMatchLocalTeam().getTeamId()) ? match.getMatchAwayTeam() : match.getMatchLocalTeam();
			addResultToTeams(winningTeam, losingTeam, tournamentOnDB);
			if(tournamentOnDB.getTournamentFormat().equals(TournamentFormat.PvP)) {
				List<Team> tournamentLeaderboardForLeague = tournamentOnDB.getTournamentLeaderboardForLeague()
																	.stream()
																	.filter(tournamentTeam -> !tournamentTeam.getTeamId().equals(losingTeam.getTeamId()))
																	.collect(Collectors.toList());
				WriteBatch batch = firestore.batch();
				batch.update(tournamentReference, "tournamentLeaderboardForLeague", tournamentLeaderboardForLeague);
				batch.commit().get();
			
				if(tournamentLeaderboardForLeague.size() == 1) {
					terminateTournament(tournamentOnDB, winningTeam, losingTeam);
					return "Tournament terminated.";
				}
			}
			if(tournamentTeamStack.isEmpty()) {
				tournamentTeamStack.add(winningTeam);
			}
			else {
				Team poppedTeam = tournamentTeamStack.pop();
				addMatchToFifaTeams(poppedTeam, winningTeam, tournamentOnDB);
			}
			WriteBatch batch = firestore.batch();
			batch.update(tournamentReference, "tournamentTeamBracketStack", tournamentTeamStack);
			batch.commit().get();
			return "Match results uploaded successfully.";
		}
		return "Not found.";
	}
	
	private void addResolvedStatusToDisputedMatch(String matchId) throws InterruptedException, ExecutionException {
		QuerySnapshot snapshot = firestore.collection("disputedMatches").whereEqualTo("disputedMatchMatchId", matchId).get().get();
		DisputedMatch disputedMatch = snapshot.toObjects(DisputedMatch.class).get(0);
		disputedMatch.setDisputedMatchStatus(DisputedMatchStatus.RESOLVED);
		firestore.collection("disputedMatches").document(disputedMatch.getDisputedMatchDocumentId()).set(disputedMatch);
	}
	
	private boolean isValidMatchUploadedStatus(Tournament tournamentOnDB, Match match, Match matchOnDB) {
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
	
	private void addResultToTeams(Team winningTeam, Team losingTeam, Tournament tournament) throws InterruptedException, ExecutionException {
		DocumentReference winningTeamReference = getTeamReference(winningTeam.getTeamId());
		DocumentReference losingTeamReference = getTeamReference(losingTeam.getTeamId());
		WriteBatch batch = firestore.batch();
		if(tournament.getTournamentGame().equals("Call Of Duty")) {
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
								addResultToUser(user, true, tournament);
							} catch (InterruptedException | ExecutionException e) {
								e.printStackTrace();
							}
						});
		losingTeamUsers
						.stream()
						.forEach(user -> {
							try {
								addResultToUser(user, false, tournament);
							} catch (InterruptedException | ExecutionException e) {
								e.printStackTrace();
							}
						});
	}
	
	private void addResultToUser(User user, boolean isWinningUser, Tournament tournament) throws InterruptedException, ExecutionException {
		DocumentReference userReference = firestore.collection("users").document(user.getUserId());
		WriteBatch batch = firestore.batch();
		if(tournament.getTournamentGame().equals("Fifa")) {
			batch.update(userReference, isWinningUser ? "userCodTotalWs" : "userCodTotalLs", FieldValue.increment(1));
		}
		else {
			batch.update(userReference, isWinningUser ? "userFifaTotalWs" : "userFifaTotalLs", FieldValue.increment(1));
		}
		batch.commit().get();
	}
	
	private void terminateTournament(Tournament tournament, Team tournamentWinningTeam, Team tournamentLosingTeam) throws InterruptedException, ExecutionException {
		DocumentReference tournamentReference = getTournamentReference(tournament.getTournamentId());
		DocumentReference winningTeamModeratorReference = firestore.collection("users").document(tournamentWinningTeam.getTeamModerator().getUserId());
		WriteBatch batch = firestore.batch();
		batch.update(tournamentReference, "tournamentWinningTeam", tournamentWinningTeam);
		batch.update(tournamentReference, "tournamentStatus", TournamentStatus.TERMINATED);
		batch.update(winningTeamModeratorReference, "userCash", tournament.getTournamentCashPrize());
		batch.commit().get();
	}
	
	public void addInvalidStatusToTeamCodTournamentMatch(String tournamentId, Team team, Match match) throws InterruptedException, ExecutionException {
		if(isActive(team.getTeamId())) {
			Team teamOnDB = getTeamReference(team.getTeamId()).get().get().toObject(Team.class);
			List<TeamCodTournament> teamCodTournamentsListWithTournament = getTeamReference(team.getTeamId()).collection("teamCodTournaments").get().get()
																						.getDocuments()
																						.stream()
																						.map(document -> document.toObject(TeamCodTournament.class))
																						.filter(tournament -> tournament.getTeamTournamentId().equals(tournamentId))
																						.collect(Collectors.toList());
			if(!teamCodTournamentsListWithTournament.isEmpty()) {
				TeamCodTournament teamCodTournament = teamCodTournamentsListWithTournament.get(0);
				List<Match> teamCodTournamentMatches = teamCodTournament.getTeamCodTournamentMatches();
				List<Match> teamCodTournamentMatchesListWithMatch = teamCodTournament.getTeamCodTournamentMatches()
																						.stream()
																						.filter(teamMatch -> teamMatch.getMatchId().equals(match.getMatchId()))
																						.collect(Collectors.toList());
				
				if(!teamCodTournamentMatchesListWithMatch.isEmpty()) {
					Match matchOnDB = teamCodTournamentMatchesListWithMatch.get(0);
					int indexOfMatch = teamCodTournamentMatches.indexOf(matchOnDB);
					teamCodTournamentMatches.remove(indexOfMatch);
					teamCodTournamentMatches.add(match);
					DocumentReference teamCodTournamentReference = getTeamReference(team.getTeamId()).collection("teamCodTournaments").document(teamCodTournament.getTeamCodTournamentId());
					WriteBatch batch = firestore.batch();
					batch.update(teamCodTournamentReference, "teamCodTournamentMatches", teamCodTournamentMatches);
					batch.commit().get();
				}
			}
			List<User> teamUsers = teamOnDB.getTeamUsers();
			teamUsers
				.stream()
				.forEach(user -> {
					try {
						addInvalidStatusToUserMatch(user, tournamentId, match);
					} catch (InterruptedException | ExecutionException e) {
						e.printStackTrace();
					}
				});
		}
	}
	
	public void addInvalidStatusToTeamFifaTournamentMatch(String tournamentId, Team team, Match match) throws InterruptedException, ExecutionException {
		if(isActive(team.getTeamId())) {
			Team teamOnDB = getTeamReference(team.getTeamId()).get().get().toObject(Team.class);
			List<TeamFifaTournament> teamFifaTournamentsListWithTournament = getFifaTournamentsSubcollectionFromTeam(team.getTeamId()).get().get()
																					.getDocuments()
																					.stream()
																					.map(document -> document.toObject(TeamFifaTournament.class))
																					.filter(tournament -> tournament.getTeamTournamentId().equals(tournamentId))
																					.collect(Collectors.toList());
			if(!teamFifaTournamentsListWithTournament.isEmpty()) {
				TeamFifaTournament teamFifaTournament = teamFifaTournamentsListWithTournament.get(0);
				List<Match> teamFifaTournamentMatches = teamFifaTournament.getTeamTournamentMatches();
				List<Match> teamFifaTournamentMatchesListWithMatch = teamFifaTournament.getTeamTournamentMatches()	
																						.stream()
																						.filter(teamMatch -> teamMatch.getMatchId().equals(match.getMatchId()))
																						.collect(Collectors.toList());
				if(!teamFifaTournamentMatchesListWithMatch.isEmpty()) {
					Match matchOnDB = teamFifaTournamentMatchesListWithMatch.get(0);
					int indexOfMatch = teamFifaTournamentMatches.indexOf(matchOnDB);
					teamFifaTournamentMatches.remove(indexOfMatch);
					teamFifaTournamentMatches.add(match);
					DocumentReference teamFifaTournamentReference = getFifaTournamentsSubcollectionFromTeam(team.getTeamId()).document(teamFifaTournament.getTeamTournamentId());
					WriteBatch batch = firestore.batch();
					batch.update(teamFifaTournamentReference, "teamTournamentMatches", teamFifaTournamentMatches);
					batch.commit().get();
				}
			}
			List<User> teamUsers = teamOnDB.getTeamUsers();
			teamUsers
				.stream()
				.forEach(user -> {
					try {
						addInvalidStatusToUserMatch(user, tournamentId, match);
					} catch (InterruptedException | ExecutionException e) {
						e.printStackTrace();
					}
				});
		}
	}
	
	public void addInvalidStatusToUserMatch(User user, String tournamentId, Match match) throws InterruptedException, ExecutionException {
		DocumentReference userReference = firestore.collection("users").document(user.getUserId());
		User userOnDB = firestore.collection("users").document(user.getUserId()).get().get().toObject(User.class);
		List<UserTournament> userTournamentList = userOnDB.getUserTournaments();
		List<UserTournament> userTournamentsWithTournament = userOnDB.getUserTournaments()
																.stream()
																.filter(userTournament -> userTournament.getUserTournamentId().equals(tournamentId))
																.collect(Collectors.toList());
		if(!userTournamentsWithTournament.isEmpty()) {
			UserTournament userTournament = userTournamentsWithTournament.get(0);
			int indexOfUserTournament = userTournamentList.indexOf(userTournament);
			userTournamentList.remove(indexOfUserTournament);
			List<Match> userTournamentMatches = userTournament.getUserTournamentMatches();
			
			List<Match> userTournamentMatchesWithMatch = userTournamentMatches
														.stream()
														.filter(userMatch -> userMatch.getMatchId().equals(match.getMatchId()))
														.collect(Collectors.toList());
			Match matchOnDB = userTournamentMatchesWithMatch.get(0);
			int indexOfMatch = userTournamentMatches.indexOf(matchOnDB);
			userTournamentMatches.remove(indexOfMatch);
			userTournamentMatches.add(match);
			userTournament.setUserTournamentMatches(userTournamentMatches);
			userTournamentList.add(userTournament);
			WriteBatch batch = firestore.batch();
			batch.update(userReference,"userTournaments", userTournamentList);
			batch.commit().get();
		}
	}
}
