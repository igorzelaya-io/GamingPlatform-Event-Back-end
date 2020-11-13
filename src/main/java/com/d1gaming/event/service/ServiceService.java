package com.d1gaming.event.service;

import com.d1gaming.library.service.Service;

public class ServiceService {

	private final String SERVICES_COLLECTION = "services";
	
	public Service getServiceById() {
		
		return null;
	}
	
	public String postService(Service service) {
		
		return "Service created successfully.";
	}

	public String deleteService() {
		return "Service deleted successfully.";
	}
	
	public String updateService() {
		return "Service updated successfully.";
	}

}
