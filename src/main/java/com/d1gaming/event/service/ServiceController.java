package com.d1gaming.event.service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.d1gaming.library.service.D1Service;

@RestController
@RequestMapping(path = "/servicesapi")
@CrossOrigin(origins = "localhost:4200")
public class ServiceController {
	
	@Autowired
	private ServiceService serviceService;
	
	@GetMapping(value = "/services/search", params = "serviceId")
	public ResponseEntity<?> getServiceById(@RequestParam(required = true) String serviceId) throws InterruptedException, ExecutionException{
		if(serviceId == null){
			return new ResponseEntity<>("Bad Input", HttpStatus.BAD_REQUEST);
		}
		Optional<D1Service> service = serviceService.getServiceById(serviceId);
		if(service == null) {
			return new ResponseEntity<>(service,HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<>(service.get(), HttpStatus.OK);
	}
	
	@GetMapping(value = "/services")
	public ResponseEntity<?> getAllServices() throws InterruptedException, ExecutionException{
		List<D1Service> serviceLs = serviceService.getAllServices();
		if(serviceLs.isEmpty()) {
			return new ResponseEntity<>(serviceLs, HttpStatus.NO_CONTENT);
		}
		return new ResponseEntity<>(serviceLs, HttpStatus.OK);
	
	}
	
}
