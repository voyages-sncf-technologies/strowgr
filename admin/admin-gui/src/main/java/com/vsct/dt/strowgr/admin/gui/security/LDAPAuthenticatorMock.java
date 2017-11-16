package com.vsct.dt.strowgr.admin.gui.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.vsct.dt.strowgr.admin.gui.security.model.User;

import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import io.dropwizard.auth.basic.BasicCredentials;

/**
 * first Draft: test only : To delete 
 * @author VSC
 *
 */
public class LDAPAuthenticatorMock implements Authenticator<BasicCredentials, User> {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(LDAPAuthenticatorMock.class);

    @Override
    public Optional<User> authenticate(BasicCredentials credentials) throws AuthenticationException {
    	
    	LOGGER.info("Authenticate for login: {}", credentials.getUsername());
    	System.out.println("authenticate for login:" + credentials.getUsername());
    	
        if ("acl-login".equals(credentials.getUsername()) &&  "acl-prod".equals(credentials.getPassword())) {
            return Optional.of(new User(credentials.getUsername(), true , false));
        } else if ("acl-login".equals(credentials.getUsername()) &&  "acl-tech".equals(credentials.getPassword())) {
                return Optional.of(new User(credentials.getUsername(), false , true));
        }
    	//return Optional.of(new User(credentials.getUsername(), true , false));

        return Optional.absent();
    }

}
