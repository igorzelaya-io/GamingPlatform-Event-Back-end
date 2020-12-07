package com.d1gaming.event.tournament;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;

import com.d1gaming.library.tournament.Tournament;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;

public class TournamentService {

	private final String TOURNAMENTS_COLLECTION = "tournaments";
	
	@Autowired
	private Firestore firestore;
	
	private CollectionReference getTournamentsCollection() {
		return firestore.collection(this.TOURNAMENTS_COLLECTION);
	}
	
	public Optional<Tournament> getTournamentById(String tournamentId) throws InterruptedException, ExecutionException {
		DocumentReference reference = getTournamentsCollection().document(tournamentId);
		if(!reference.get().get().exists()) {
			return null;
		}
		DocumentSnapshot snapshot = reference.get().get();
		return Optional.of(snapshot.toObject(Tournament.class));
	}
	
//	public List<Tournament> getAllTournaments(){
//		
//	}
	
}
