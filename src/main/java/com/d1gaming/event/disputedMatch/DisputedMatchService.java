package com.d1gaming.event.disputedMatch;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.d1gaming.library.match.DisputedMatch;
import com.d1gaming.library.match.DisputedMatchStatus;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteBatch;

@Service
public class DisputedMatchService {

	@Autowired
	private Firestore firestore;
	
	public String disputeMatch(DisputedMatch disputedMatch) throws InterruptedException, ExecutionException {
		DocumentReference disputedMatchReference = firestore.collection("disputedMatches").add(disputedMatch).get();
		String documentId = disputedMatchReference.getId();
		disputedMatchReference.update("disputedMatchDocumentId", documentId).get();
		WriteBatch batch = firestore.batch();
		if(disputedMatch.getDisputedMatchChallengeId() != null) {
			DocumentReference matchReference = firestore.collection("challenges").document(disputedMatch.getDisputedMatchChallengeId())
														.collection("challengeMatches").document(disputedMatch.getDisputedMatchMatchId());
			batch.update(matchReference, "hasImage", true);
			batch.commit().get();
			return "Match created successfully.";
		}
		DocumentReference matchReference = firestore.collection("tournaments").document(disputedMatch.getDisputedMatchTournamentId())
													.collection("tournamentMatches").document(disputedMatch.getDisputedMatchMatchId());
		batch.update(matchReference, "hasImage", true);
		batch.commit().get();
		return "Match created successfully.";
	}
	
	public DisputedMatch getDisputedMatchFromChallenge(String challengeId, String matchId) throws InterruptedException, ExecutionException {
		QuerySnapshot snapshot = firestore.collection("disputedMatches").whereEqualTo("disputedMatchMatchId", matchId).whereEqualTo("disputedMatchChallengeId", challengeId).get().get();
		if(!snapshot.isEmpty()) {
			DisputedMatch match = snapshot
									.getDocuments()
									.stream()
									.map(document -> document.toObject(DisputedMatch.class))
									.collect(Collectors.toList()).get(0);
			return match;
		}
		return null; 
	}
	
	public DisputedMatch getDisputedMatchFromTournament(String tournamentId, String matchId) throws InterruptedException, ExecutionException {
		QuerySnapshot snapshot = firestore.collection("disputedMatches").whereEqualTo("disputedMatchMatchId", matchId).whereEqualTo("disputedMatchTournamentId", tournamentId).get().get();
		if(!snapshot.isEmpty()) {
			DisputedMatch match = snapshot
									.getDocuments()
									.stream()
									.map(document -> document.toObject(DisputedMatch.class))
									.collect(Collectors.toList()).get(0);
			return match;
		}
		return null;
	}
	
	public List<DisputedMatch> getAllDisputedMatches() throws InterruptedException, ExecutionException{
       return firestore.collection("disputedMatches").get().get().getDocuments()
									.stream()
									.map(document -> document.toObject(DisputedMatch.class))
									.filter(disputedMatch -> disputedMatch.getDisputedMatchStatus().equals(DisputedMatchStatus.DISPUTED))
									.collect(Collectors.toList());
	}
	
}
