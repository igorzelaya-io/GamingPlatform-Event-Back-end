package com.d1gaming.event.teamTournament;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.d1gaming.library.team.Team;
import com.d1gaming.library.team.TeamStatus;
import com.d1gaming.library.team.TeamTournament;
import com.d1gaming.library.tournament.Tournament;
import com.d1gaming.library.tournament.TournamentStatus;
import com.google.api.core.ApiFuture;
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
	
	private final String TEAM_COLLECTION = "teams";
	
	private final String TEAM_TOURNAMENT_SUBCOLLECTION = "teamTournaments";
	
	private CollectionReference getTeamCollectionReference() {
		return firestore.collection(TEAM_COLLECTION);
	}
	
	private CollectionReference getTournamentsSubcollectionFromTeam(String teamId) {
		return firestore.collection(TEAM_COLLECTION).document(teamId).collection(TEAM_TOURNAMENT_SUBCOLLECTION);
	}
	
	private DocumentReference getTournamentReference(String tournamentId) {
		return firestore.collection("tournaments").document(tournamentId); 
	}
	
	private DocumentReference getTeamReference(String teamId) {
		return getTeamCollectionReference().document(teamId);
	}
	
	private DocumentReference getTournamentReferenceFromTeam(String teamId, String tournamentId) {
		return getTournamentsSubcollectionFromTeam(teamId).document(tournamentId);
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
	
	public List<Tournament> getAllTournamentsFromTeam(String teamId) throws InterruptedException, ExecutionException {
		if(isActive(teamId)) {
			ApiFuture<QuerySnapshot> queryForTournaments = getTournamentsSubcollectionFromTeam(teamId).get();
			return queryForTournaments.get().getDocuments()
											.stream()
											.map(document -> document.toObject(TeamTournament.class))
											.map(tournament -> tournament.getTeamTournament())
											.filter(tournament -> {
												try {
													return isActive(tournament.getTournamentId());
												} catch (InterruptedException | ExecutionException e) {
													e.printStackTrace();
												}
												return false; 
											})
											.collect(Collectors.toList());
		}
		return new ArrayList<>();
	}
	
	public Optional<Tournament> getTournamentFromTeamById(String teamId, String tournamentId) throws InterruptedException, ExecutionException {
		if(isActiveTournament(teamId)) {
			if(isActive(tournamentId)) {
				DocumentReference tournamentReference = getTournamentReferenceFromTeam(teamId, tournamentId);
				DocumentSnapshot tournamentSnapshot = tournamentReference.get().get();
				return Optional.of(tournamentSnapshot.toObject(Tournament.class));
			}
			return null;
		}
		return null; 
	}
	
	public String addTeamToTournament(Team team, Tournament tournament) throws InterruptedException, ExecutionException {
		if(isActiveTournament(tournament.getTournamentId()) && isActive(team.getTeamId())) {
			DocumentReference tourneyReference = getTournamentReference(tournament.getTournamentId());
			ApiFuture<WriteResult> addedDocument = getTeamCollectionReference().document(team.getTeamId())
															.collection(TEAM_TOURNAMENT_SUBCOLLECTION).document(tournament.getTournamentId()).set(tournament);
			System.out.println("Added Document: " + addedDocument.get().getUpdateTime());
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
		if(isActiveTournament(tournament.getTournamentId()) && isActive(team.getTeamId())) {
			DocumentReference tournamentReference = getTournamentReference(tournament.getTournamentId());
			List<Team> tournamentTeamList = tournament.getTournamentTeams();
			ApiFuture<WriteResult> deletedDocument = getTeamCollectionReference().document(team.getTeamId())
																				 .collection(TEAM_TOURNAMENT_SUBCOLLECTION).document(tournament.getTournamentId()).delete();
			System.out.println("Deleted Document: " + deletedDocument.get().getUpdateTime());
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
}
