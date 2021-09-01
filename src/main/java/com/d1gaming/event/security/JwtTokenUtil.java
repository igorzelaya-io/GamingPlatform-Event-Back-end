package com.d1gaming.event.security;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.d1gaming.library.user.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;

@Component
public class JwtTokenUtil {

	@Value("${d1gaming.app.jwtSecret}")
	private String jwtSecret;
	
	private final String jwtIssuer = "d1gaming.com";
	
	private final Logger logger = LoggerFactory.getLogger(JwtTokenUtil.class);
	
	public String generateAccessToken(User user) {
		return Jwts.builder()
				.setSubject(user.getUserName())
				.setIssuer(jwtIssuer)
				.setIssuedAt(new Date(System.currentTimeMillis()))
				.setExpiration(new Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000))
				.signWith(SignatureAlgorithm.HS512, jwtSecret)
				.compact();
	}
	
	public String getUserNameFromJwtToken(String token) {
		 Claims claims = Jwts.parser()
				 .setSigningKey(jwtSecret)
				 .parseClaimsJws(token)
				 .getBody();
		 return claims.getSubject().split(",")[0];
	}
	
	public Date getJwtExpirationDate(String token) {
		Claims claims = Jwts.parser()
				.setSigningKey(jwtSecret)
				.parseClaimsJws(token)
				.getBody();
		return claims.getExpiration();
	}
	
	
	public boolean validate(String token) {
		try {
			Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token);
			return true;
		}
		
		catch(SignatureException e) {
			logger.error("Invalid JWT signature - {}",e.getMessage());
		}
		
		catch(MalformedJwtException e) {
			logger.error("Invalid JWT token - {}", e.getMessage());
		}
		
		catch(ExpiredJwtException e) {
			logger.error("Expired JWT token - {}",e.getMessage());
		}
		
		catch(UnsupportedJwtException e) {
			logger.error("Unsupported JWT token - {}", e.getMessage());		
		}
		
		catch(IllegalArgumentException e) {
			logger.error("JWT claims string is empty - {}", e.getMessage());
		}
		return false;
	}
}

