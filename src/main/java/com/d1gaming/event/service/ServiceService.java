package com.d1gaming.event.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;

import com.d1gaming.library.service.D1Service;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;

public class ServiceService {

	@Autowired
	private Firestore firestore;
	
	private final String SERVICES_COLLECTION = "services";
	
	private CollectionReference getServicesCollection() {
		return firestore.collection(SERVICES_COLLECTION);
	}
	
	public Optional<D1Service> getServiceById(String serviceId) throws InterruptedException, ExecutionException {
		DocumentReference reference = getServicesCollection().document(serviceId);
		if(!reference.get().get().exists()) {
			return null;
		}
		DocumentSnapshot snapshot = reference.get().get();
		return Optional.of(snapshot.toObject(D1Service.class));
	}
	
	public List<D1Service> getAllServices() throws InterruptedException, ExecutionException{
		//asynchronously retrieve all documents
		ApiFuture<QuerySnapshot> future = getServicesCollection().get();
		// future.get() blocks on response
		List<QueryDocumentSnapshot> documents = future.get().getDocuments();
		List<D1Service> servicesLs = new ArrayList<>();
		documents.forEach(document -> {
			servicesLs.add(document.toObject(D1Service.class));
		});
		return servicesLs;
	}
	
	public String postService(D1Service service) {
		
		return "Service created successfully.";
	}

	public String deleteService() {
		return "Service deleted successfully.";
	}
	
	public String updateService() {
		return "Service updated successfully.";
	}
}
