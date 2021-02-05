package com.d1gaming.event.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(
		securedEnabled = true,
		jsr250Enabled = true,
		prePostEnabled = true)
public class EventSecurityConfiguration extends WebSecurityConfigurerAdapter {

	@Autowired
	private EventEntryPoint entryPoint;
	
	
	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.cors().
		and()
			.csrf().disable()
			.exceptionHandling().authenticationEntryPoint(entryPoint)
		.and()
			.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
		.and()
			.authorizeRequests()
			.antMatchers("/").permitAll()
			.antMatchers("/servicesapi/**").permitAll()
			.antMatchers("/tournamentsapi/**").permitAll()
			.anyRequest().authenticated();
	}
}
