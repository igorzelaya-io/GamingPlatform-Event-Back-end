package com.d1gaming.event.firebaseconfig;

import java.io.IOException;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;

@Service
public class EventFirestoreInitialize {

	@Value("classpath:static/d1gamingapp-firebase-adminsdk-2rph3-8ad7753499.json")
	Resource resourceFile;
	
	@PostConstruct
	public FirestoreOptions initialize() throws IOException {
        return FirestoreOptions.getDefaultInstance()
        										.toBuilder().setProjectId("d1gamingapp")
        										.setCredentials(GoogleCredentials.fromStream(resourceFile.getInputStream()))
        										.build();
	}
	
	@Bean
	public Firestore getFirestore() throws IOException {
		return initialize().getService();
	}
}
