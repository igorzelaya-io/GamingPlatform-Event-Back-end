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
	
	public Optional<Match> getTeamMatchFromTournament(String matchId, String tournamentId) throws InterruptedException, ExecutionException{
		List<Match> tournamentMatchesList = getTournamentReference(tournamentId).collection("tournamentMatches").get().get()
													   .getDocuments()
													   .stream()
													   .map(document -> document.toObject(Match.class))
													   .filter(match -> match.getMatchId().equals(matchId))
													   .collect(Collectors.toList());
		return Optional.of(tournamentMatchesList.get(0));
	}
	
	
	//TODO:
//	public List<Tournament> getAllActiveFifaTournamentsFromTeam(){
//		
//	}
//	//TODO: 
//	public List<Tournament> getAllActiveCodTournamentsFromTeam(){
//		
//	}
	
	
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
			DocumentReference tournamentReference = getFifaTournamentReferenceFromTeam(teamId, tournamentId);
			DocumentSnapshot tournamentSnapshot = tournamentReference.get().get();
			
			return Optional.of(tournamentSnapshot.toObject(TeamFifaTournament.class).getTeamTournament());
		}
		return null; 
	}
	
	public Optional<Tournament> getCodTournamentFromTeamById(String teamId, String tournamentId) throws InterruptedException, ExecutionException {
		if(isActiveTournament(teamId) && isActive(tournamentId)) {
			DocumentReference tournamentReference = getCodTournamentReferenceFromTeam(teamId, tournamentId);
			DocumentSnapshot tournamentSnapshot = tournamentReference.get().get();
			return Optional.of(tournamentSnapshot.toObject(TeamCodTournament.class).getTeamCodTournament());
		}
		return null; 
	}
	
	public String addTeamToFifaTournament(Team team, Tournament tournament) throws InterruptedException, ExecutionException {
		if(isActiveTournament(tournament.getTournamentId()) && isActive(team.getTeamId())) {
			DocumentReference tourneyReference = getTournamentReference(tournament.getTournamentId());
			Tournament tournamentOnDB = tourneyReference.get().get().toObject(Tournament.class);			
			List<Match> teamMatches = new ArrayList<>();
			TeamFifaTournament teamTournamentSubDocument = new TeamFifaTournament(tournamentOnDB, teamMatches, 0, 0, 0, 0, 0, 0, 0, 0, TeamTournamentStatus.ACTIVE);
			List<Team> tournamentTeamList = tournamentOnDB.getTournamentTeams();
			
			boolean isAlreadyPartOfTournament = getFifaTournamentsSubcollectionFromTeam(team.getTeamId()).get().get()
																	.getDocuments()
																	.stream()
																	.map(document -> document.toObject(TeamFifaTournament.class))
																	.anyMatch(teamFifaTournament -> teamFifaTournament.getTeamTournament().getTournamentName().equals(tournament.getTournamentName()));
			if(!isAlreadyPartOfTournament) {				
				DocumentReference addedDocumentToFifaTournamentsSubcollection = getFifaTournamentsSubcollectionFromTeam(team.getTeamId()).add(teamTournamentSubDocument).get();
				String documentId = addedDocumentToFifaTournamentsSubcollection.getId();
				tournamentTeamList.add(team);	
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
				Stack<Team> tournamentTeamStack = tournamentOnDB.getTournamentTeamBracketStack();
				WriteBatch batch = firestore.batch();
				if(tournamentTeamStack.isEmpty()) {
					tournamentTeamStack.add(team);
				}
				else {
					Team localTeam = tournamentTeamStack.pop();
					addMatchToFifaTeams(localTeam, team, tournamentOnDB);
				}
				batch.update(tourneyReference, "tournamentTeamBracketStack", tournamentTeamStack);
				batch.update(tourneyReference, "tournamentTeams", tournamentTeamList);
				batch.update(tourneyReference, "tournamentNumberOfTeams", FieldValue.increment(1));
				batch.update(addedDocumentToFifaTournamentsSubcollection, "teamTournamentId", documentId); 
				batch.commit().get()
					.stream()
					.forEach(result -> 
						System.out.println("Update Time: " +result.getUpdateTime()));
				return "Team added successfully to tournament.";
			}
			return "Team is already part of tournament.";
		}
		return "Not found.";
	}
	
	public String addTeamToCodTournament(Team team, Tournament tournament) throws InterruptedException, ExecutionException {
		if(isActiveTournament(tournament.getTournamentId()) && isActive(team.getTeamId())) {
			DocumentReference tourneyReference = getTournamentReference(tournament.getTournamentId());
			Tournament tournamentOnDB = tourneyReference.get().get().toObject(Tournament.class);
			Team teamOnDB = getTeamReference(team.getTeamId()).get().get().toObject(Team.class);
			List<Match> teamMatches = new ArrayList<>();
			TeamCodTournament teamCodTournamentSubdocument = new TeamCodTournament(tournamentOnDB,teamMatches, 0, 0, 0, 0, 0, 0, TeamTournamentStatus.ACTIVE);
			List<Team> tournamentTeamList = tournamentOnDB.getTournamentTeams();			
			
			boolean isAlreadyPartOfTournament = getCodTournamentsSubcollectionFromTeam(team.getTeamId()).get().get()
																	.getDocuments()
																	.stream()
																	.map(document -> document.toObject(TeamCodTournament.class))
																	.anyMatch(teamCodTournament -> teamCodTournament.getTeamCodTournament().getTournamentName().equals(tournament.getTournamentName())); 
			if(!isAlreadyPartOfTournament) {				
				DocumentReference addedDocumentToTeamCodTournaments = getCodTournamentsSubcollectionFromTeam(team.getTeamId()).add(teamCodTournamentSubdocument).get();
				String documentId = addedDocumentToTeamCodTournaments.getId();
				tournamentTeamList.add(teamOnDB);
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
				Stack<Team> tournamentTeamStack = tournamentOnDB.getTournamentTeamBracketStack();
				WriteBatch batch = firestore.batch();
				if(tournamentTeamStack.isEmpty()) {
					tournamentTeamStack.add(team);
				}
				else {
					Team localTeam = tournamentTeamStack.pop();
					addMatchToCodTeams(localTeam, teamOnDB, tournamentOnDB);					
				}
				batch.update(tourneyReference, "tournamentTeamBracketStack", tournamentTeamStack);
				batch.update(addedDocumentToTeamCodTournaments, "teamCodTournamentId", documentId);
				batch.update(tourneyReference, "tournamentTeams", tournamentTeamList);
				batch.update(tourneyReference, "tournamentNumberOfTeams", FieldValue.increment(1));
				batch.commit().get()
						.stream()
						.forEach(result -> System.out.println("Update Time: " + result.getUpdateTime()));
				return "Team added successfully to tournament.";
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
			DocumentReference userTournamentDocument = firestore.collection("users").document(user.getUserId()).collection("userTournaments").add(userTournament).get();
			String userTournamentDocumentId = userTournamentDocument.getId();
			WriteBatch batch = firestore.batch();
			batch.update(userTournamentDocument, "userTournamentId", userTournamentDocumentId);
			batch.commit()
				.get()
				.stream()
				.forEach(result -> System.out.println("Update Time: " + result.getUpdateTime()));
			return "User tournament added.";
		}
		return "Not found.";
	}
	
	public String removeTeamFromCodTournament(Team team, Tournament tournament) throws InterruptedException, ExecutionException {
		if(isActiveTournament(tournament.getTournamentId()) && isActive(team.getTeamId()) && !tournament.getStartedTournamentStatus()) {
			DocumentReference tournamentReference = getTournamentReference(tournament.getTournamentId());
			Tournament tournamentOnDB = tournamentReference.get().get().toObject(Tournament.class);
			List<Team> tournamentTeamList = tournamentOnDB.getTournamentTeams();
			boolean isPartOfTournament = tournamentTeamList
										.stream()
										.anyMatch(tournamentTeam -> tournamentTeam.getTeamName().equals(team.getTeamName()));
			if(isPartOfTournament) {
				List<Team> newTournamentTeamList = tournamentTeamList
														.stream()
														.filter(tournamentTeam -> !tournamentTeam.getTeamName().equals(team.getTeamName()))
														.collect(Collectors.toList());
				
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
							removeTeamFromUser(user, team, tournament);
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
				batch.update(tournamentReference, "tournamentTeams", newTournamentTeamList);
				batch.update(tournamentReference, "tournamentNumberOfTeams", FieldValue.increment(-1));
				batch.commit().get()
							.stream()
							.forEach(result -> System.out.println("Update Time: " + result.getUpdateTime()));	
							 
			}							
		}
		return "Not found.";
	}
	
	public String removeTeamFromFifaTournament(Team team, Tournament tournament) throws InterruptedException, ExecutionException {
		if(isActiveTournament(tournament.getTournamentId()) && isActive(team.getTeamId()) && !tournament.getStartedTournamentStatus()) {
			DocumentReference tournamentReference = getTournamentReference(tournament.getTournamentId());
			Tournament tournamentOnDB = tournamentReference.get().get().toObject(Tournament.class);
			List<Team> tournamentTeamList = tournamentOnDB.getTournamentTeams();
			boolean isPartOfTournament = tournamentTeamList
										.stream()
										.anyMatch(tournamentTeam -> tournamentTeam.getTeamName().equals(team.getTeamName()));
			if(isPartOfTournament) {
				List<Team> newTournamentTeamList = tournamentTeamList
														.stream()
														.filter(tournamentTeam -> !tournamentTeam.getTeamName().equals(team.getTeamName()))
														.collect(Collectors.toList());
				
				List<TeamFifaTournament> teamFifaTournaments = getFifaTournamentsSubcollectionFromTeam(team.getTeamId()).get().get()
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
							removeTeamFromUser(user, team, tournament);
						} catch (InterruptedException | ExecutionException e) {
							e.printStackTrace();
						}
					});
				TeamFifaTournament teamFifaTournament = teamFifaTournaments.get(0);
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
				if(!teamFifaTournaments.isEmpty()) {
					DocumentReference invalidDocument = getFifaTournamentsSubcollectionFromTeam(team.getTeamId()).document(teamFifaTournament.getTeamTournamentId());
					batch.update(invalidDocument, "teamTournamentStatus", TeamTournamentStatus.INACTIVE);
				}
				batch.update(tournamentReference, "tournamentTeamBracketStack", tournamentTeamStack);
				batch.update(tournamentReference, "tournamentTeams", newTournamentTeamList);
				batch.update(tournamentReference, "tournamentNumberOfTeams", FieldValue.increment(-1));
				batch.commit().get()
							.stream()
							.forEach(result -> System.out.println("Update Time: " + result.getUpdateTime()));	
							 
			}							
		}
		return "Not found.";
	}
	
	public String removeTeamFromUser(User user, Team team, Tournament tournament) throws InterruptedException, ExecutionException {
		if(isActiveUser(user.getUserId()) && isActive(team.getTeamId()) && isActiveTournament(tournament.getTournamentId())) {
			List<UserTournament> userTournaments = firestore.collection("users").document("users").collection("userTournaments").get().get()
																		.getDocuments()
																		.stream()
																		.map(document -> document.toObject(UserTournament.class))
																		.filter(userTournament -> userTournament.getUserTournament().getTournamentId().equals(tournament.getTournamentId()))
																		.collect(Collectors.toList());
			if(!userTournaments.isEmpty()) {				
				UserTournament userTournament = userTournaments.get(0);
				DocumentReference userTournamentReference = firestore.collection("users").document(user.getUserId()).collection("userTournaments").document(userTournament.getUserTournamentId());
				WriteBatch batch = firestore.batch();
				batch.update(userTournamentReference, "userTournamentStatus", TeamTournamentStatus.INACTIVE);
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
			DocumentReference tourneyReference = getTournamentReference(matchTournament.getTournamentId());
			
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
			batch.update(tourneyReference, "matchId", matchDocumentId);
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
			List<UserTournament> userTournamentList = firestore.collection("users").document(user.getUserId()).collection("userTournaments").get().get()
																		.getDocuments()
																		.stream()
																		.map(document -> document.toObject(UserTournament.class))
																		.filter(userTournament -> userTournament.getUserTournament().getTournamentId().equals(tournament.getTournamentId()))
																		.collect(Collectors.toList());
			
			UserTournament userTournament = userTournamentList.get(0);
			DocumentReference userTournamentReference = firestore.collection("users").document(user.getUserId()).collection("userTournaments").document(userTournament.getUserTournamentId());
			List<Match> userTournamentMatches = userTournament.getUserTournamentMatches();
			userTournamentMatches.add(match);
			WriteBatch batch = firestore.batch();
			batch.update(userTournamentReference, "userTournament", userTournament);
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
