package com.d1gaming.event.challenge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.d1gaming.library.challenge.Challenge;
import com.d1gaming.library.user.User;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteBatch;
import com.google.cloud.firestore.WriteResult;

@Service
public class ChallengeService {

	private final String CHALLENGES_COLLECTION = "challenges";
	
	@Autowired
	private Firestore firestore;
	
	
	//Get CollectionReference for Challenges collection.
	private CollectionReference getChallengesCollection() {
		return firestore.collection(this.CHALLENGES_COLLECTION);
	}
	
	
	//Get a challenge by its ID.
	public Challenge getChallengeById(String challengeId) throws InterruptedException, ExecutionException {
		DocumentReference reference = getChallengesCollection().document(challengeId);
		//If document does not exist, return null.
		if(!reference.get().get().exists()) {
			return null;
		}
		return reference.get().get().toObject(Challenge.class);
	}
	
	
	//Get all Challenges available in collection.
	public List<Challenge> getAllChallenges() throws InterruptedException, ExecutionException{
		//Retrieve all documents asynchronously.
		ApiFuture<QuerySnapshot> snapshot = getChallengesCollection().get();
		List<QueryDocumentSnapshot> ls = snapshot.get().getDocuments();
		//If there are no documents, return null.
		if(!ls.isEmpty()) {
			List<Challenge> userLs = new ArrayList<>();
			ls.forEach((obj) -> {
				userLs.add(obj.toObject(Challenge.class));
			});
			return userLs;
		}
		return null;
	}
	
	//Delete Challenge from collection by its ID.
	public String deleteChallengeById(String challengeId) throws InterruptedException, ExecutionException {
		DocumentReference reference = getChallengesCollection().document(challengeId);
		DocumentSnapshot snapshot = getChallengesCollection().document(challengeId).get().get();
		//Evaluate if challenge exists in collection.
		if(!snapshot.exists()) {
			return "Challenge not found.";
		}
		WriteBatch batch = firestore.batch();
		batch.delete(reference);
		List<WriteResult> results = batch.commit().get();
		results.forEach(result -> System.out.println("Update Time: " + result.getUpdateTime()));
		return "Challenge with id '" + snapshot.toObject(Challenge.class).getChallengeId() + "' was deleted.";
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
			List<WriteResult> results = batch.commit().get();
			results.forEach(result -> System.out.println("Update Time: " + result.getUpdateTime()));
			return "User updated Successfully.";
		}
		return "Challenge not found";
	}
	
	public String updateField(String challengeId, String challengeField, String replaceValue) throws InterruptedException, ExecutionException {
		DocumentReference reference = getChallengesCollection().document(challengeId);
		if(reference.get().get().exists()) {
			WriteBatch batch = firestore.batch();
			batch.update(reference, challengeField, replaceValue);
			List<WriteResult> results = batch.commit().get();
			results.forEach(result -> System.out.println("Update Time : " + result.getUpdateTime()));
			return "Field updated successfully";
		}
		return "Challenge not found.";
	}
	
	//Create a challenge in the challenges collection, User being the user posting the challenge.
	public String postOneVOneChallenge(String userId, Challenge challenge) throws InterruptedException, ExecutionException {
		final String USERS = "users";
		String response = "Could not create challenge.";
		final DocumentReference reference = firestore.collection(USERS).document(userId);
		//If document exists in challenge collection.
		if(reference.get().get().exists()) {
			//Transaction to get() and set().
			ApiFuture<String> futureTransaction = firestore.runTransaction(transaction -> {
				DocumentSnapshot snapshot = transaction.get(reference).get();
				User user = snapshot.toObject(User.class);
				double userCash = snapshot.getDouble("userCash");
				//Evaluate if user holds enough cash to host challenge.
				if(userCash >= challenge.getChallengeCashPrize()) {
					challenge.setChallengeUserAdmin(snapshot.toObject(User.class));
					Map<String,Object> map = new HashMap<>();
					map.put(user.getUserId(), user);
					//Assign only player to team 1.
					challenge.setChallengeHostPlayers(map);
					//Create a new Challenge with an auto-generated ID.
					DocumentReference ref = firestore.collection(CHALLENGES_COLLECTION).add(challenge).get();
					//Assign autogeneratedId to challengeId field.
					ref.get().get().toObject(Challenge.class).setChallengeId(ref.getId());
					return "Created challenge with ID: '" + ref.getId() + "'";
				}
				else {
					throw new Exception("Not enough cash.");
				}
			});
			response = futureTransaction.get();
			return response;
		}		
	return response;
	}
	
	public String postChallenge(Map<String,Object> userMap, String userAdminId, Challenge challenge) throws InterruptedException, ExecutionException {
		final String USERS = "users";
		final DocumentReference reference = firestore.collection(USERS).document(userAdminId);
		String response = "Could not create challenge.";
		//If user is present on users collection.
		if(reference.get().get().exists()) {
			//Transaction to get() and set().
			ApiFuture<String> futureTransaction = firestore.runTransaction(transaction -> {
				DocumentSnapshot snapshot = transaction.get(reference).get();
				User user = snapshot.toObject(User.class);
				double userCash = snapshot.getDouble("userCash");
				if(userCash >= challenge.getChallengeCashPrize()) {
					challenge.setChallengeUserAdmin(user);
					userMap.put(userAdminId, user);
					challenge.setChallengeHostPlayers(userMap);
					DocumentReference ref = firestore.collection(CHALLENGES_COLLECTION).add(challenge).get();
					ref.get().get().toObject(Challenge.class).setChallengeId(ref.getId());
					return "Created challenge with ID: '" + ref.getId() + "'";
				}
				else {
					throw new Exception("Not enough cash.");
				}
			});
			response = futureTransaction.get();
			return response;
		}
		return response;
	}
	

}
