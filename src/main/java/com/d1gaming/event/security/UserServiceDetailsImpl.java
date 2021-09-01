package com.d1gaming.event.security;

import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import com.d1gaming.event.tournament.TournamentService;
import com.d1gaming.library.user.User;
import com.d1gaming.library.user.UserDetailsImpl;

@Component
public class UserServiceDetailsImpl implements UserDetailsService {

	@Autowired
	private TournamentService tournamentService; 

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		User user = null;
		try {
			user = tournamentService.getUserByUserName(username);
		}
		catch(InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		if(user == null) {
			throw new UsernameNotFoundException("User with username " + username + " not found.");
		}
		return UserDetailsImpl.build(user);
	}





}