package uk.co.vurt.hakken.server;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import uk.co.vurt.hakken.security.HashUtils;
import uk.co.vurt.hakken.security.auth.Authenticator;

@Controller
public class AuthenticationController {

	Authenticator authenticator;
	
	/**
	 * This method should only ever be accessed over SSL!
	 * 
	 * @param username
	 * @param password
	 * @return
	 */
	@RequestMapping(value = "/auth", method = RequestMethod.POST)
	@ResponseBody
	public Object authenticate(	@RequestParam(value = "username", required = true) String username, 
								@RequestParam(value = "password", required = true) String password){
		if(username != null && password != null){
			//Perform LDAP lookup using username & password to bind.
			//if authentication is successful, retrieve the user's shared secret from the database and send that to them as the response.
			if(authenticator.authenticate(username, password)){
				//TODO: Retrieve shared secret
				return HashUtils.SHARED_SECRET;
			} else {
				return new ResponseEntity<String>(HttpStatus.NOT_FOUND);
			}
		}
		return new ResponseEntity<String>(HttpStatus.BAD_REQUEST);
	}
}