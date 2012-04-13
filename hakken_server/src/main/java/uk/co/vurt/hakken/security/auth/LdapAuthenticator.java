package uk.co.vurt.hakken.security.auth;

import java.util.HashMap;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class LdapAuthenticator implements Authenticator {

	private static final Logger logger = LoggerFactory.getLogger(LdapAuthenticator.class);
			
	Hashtable<String, String> environment;
	
	public LdapAuthenticator(){
		environment = new Hashtable<String, String>();
		environment.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory"); //TODO: Parameterise this
		environment.put(Context.PROVIDER_URL, "ldap://ldap.wmfs.net:389/"); //TODO: Parameterise this
		environment.put(Context.SECURITY_AUTHENTICATION, "simple"); //TODO: Parameterise this
	}
	
	@Override
	public boolean authenticate(String username, String password) {
		environment.put(Context.SECURITY_PRINCIPAL, username);
		environment.put(Context.SECURITY_CREDENTIALS, password);
		
		try {
			DirContext context = new InitialDirContext(environment);
			//if the context is created successfully, then we have authenticated.
			//tidy up and then return true
			logger.debug("Authentication succeeded for " + username);
			context.close();
			context = null;
			return true;
		} catch (NamingException e) {
			logger.debug("Authentication failed", e);
		}
		
		return false;
	}

}