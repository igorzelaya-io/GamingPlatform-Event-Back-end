package com.d1gaming.event.service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.d1gaming.library.service.D1Service;

@RestController
@RequestMapping(path="/servicesapi")
@CrossOrigin(origins="localhost:4200")
@PreAuthorize("permitAll()")
public class ServiceController {
	
	@Autowired
	private ServiceService serviceService;
	
	@GetMapping(value = "/services/search", params = "serviceId")
	public ResponseEntity<D1Service> getServiceById(@RequestParam(required = true) String serviceId) throws InterruptedException, ExecutionException{
		if(serviceId == null){
			return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
		}
		Optional<D1Service> service = serviceService.getServiceById(serviceId);
		if(service == null) {
			return new ResponseEntity<>(null,HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<D1Service>(service.get(), HttpStatus.OK);
	}
	
	@GetMapping(value = "/services")
	public ResponseEntity<List<D1Service>> getAllServices() throws InterruptedException, ExecutionException{
		List<D1Service> serviceLs = serviceService.getAllServices();
		if(serviceLs.isEmpty()) {
			return new ResponseEntity<List<D1Service>>(serviceLs, HttpStatus.NO_CONTENT);
		}
		return new ResponseEntity<List<D1Service>>(serviceLs, HttpStatus.OK);
	
	}
	
}
