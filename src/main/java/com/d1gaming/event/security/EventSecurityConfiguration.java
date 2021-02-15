package com.d1gaming.event.security;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import com.d1gaming.event.tournament.TournamentService;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(
		securedEnabled = true,
		jsr250Enabled = true,
		prePostEnabled = true
	)
public class EventSecurityConfiguration extends WebSecurityConfigurerAdapter {

	@Autowired
	UserServiceDetailsImpl userDetailsService;
	
	@Autowired
	private TournamentService tournamentService;
	
	@Bean
	public JwtTokenFilter authenticationJwtTokenFilter() {
		return new JwtTokenFilter();
	}
	
	@Override
	protected void configure(AuthenticationManagerBuilder authenticationManagerBuilder) throws Exception {
		authenticationManagerBuilder.userDetailsService(username -> {
			try {
				return tournamentService
						.getUserDetailsByUserName(username)
						.orElseThrow(
								() -> new UsernameNotFoundException("User: '" + username + "', was not found")
						);
			}
			catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
			return null;
		});
	}
	
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
	
	@Override
	@Bean
	public AuthenticationManager authenticationManager() throws Exception {
		return super.authenticationManagerBean();
	}
	
	
	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http = http.cors()
				.and()
				.csrf().disable();
		
		http = http.sessionManagement()
				.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
				.and();
		
		http = http.exceptionHandling()
				.authenticationEntryPoint(
						(request, response, ex) -> {
							response.sendError(HttpServletResponse.SC_UNAUTHORIZED, ex.getMessage());
						}).and();
		
		http
			.authorizeRequests()
			.antMatchers("/servicesapi/getAllServices").permitAll()
			.antMatchers("/servicesapi/getServiceById").permitAll()
			.antMatchers("/tournamentsapi/**").permitAll()
			.anyRequest().authenticated();
		
		http.addFilterBefore(authenticationJwtTokenFilter(), 
				UsernamePasswordAuthenticationFilter.class);
	}
	
	@Bean
	public CorsFilter corsFilter() {
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowCredentials(true);
		config.setAllowedOrigins(Arrays.asList("http://localhost:4200"));
		config.addAllowedHeader("*");
		config.addAllowedMethod("*");
		source.registerCorsConfiguration("/**", config);
		return new CorsFilter(source);
	}
}
