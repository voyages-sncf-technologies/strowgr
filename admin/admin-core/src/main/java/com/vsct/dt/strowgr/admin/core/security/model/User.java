/*
 *
 *  * This file is part of the Hesperides distribution.
 *  * (https://github.com/voyages-sncf-technologies/hesperides)
 *  * Copyright (c) 2016 VSCT.
 *  *
 *  * Hesperides is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as
 *  * published by the Free Software Foundation, version 3.
 *  *
 *  * Hesperides is distributed in the hope that it will be useful, but
 *  * WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  * General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */

package com.vsct.dt.strowgr.admin.core.security.model;

import java.security.Principal;

/**
 * Created by william_montaz on 12/11/2014.
 */
public class User implements Principal {
    /**
     * Convenient 'untracked user' information
     */
    public static final User UNTRACKED = new User(Platform.PRODUCTION.value(),"untracked", false, true);

    private final String  username;
    private final boolean prodUser;
    private final boolean techUser;
    private final String  platformValue;

    public User(final String  platformValue, final String username, boolean prodUser, boolean techUser) {
        this.username = username;
        this.prodUser = prodUser;
        this.techUser = techUser;
        this.platformValue	=	platformValue;
    }

	public String getUsername() {
        return username;
    }

    /**
     * @TODO: Duplicate: remove one of them..
     */
    @Override
	public String getName() {
        return username;
    }

    public boolean isProdUser() {
        return prodUser;
    }

    public boolean isTechUser() {
        return techUser;
    }

	public String getPlatformValue() {
		return platformValue;
	}

	@Override
	public String toString() {
		return "User [username=" + username + ", prodUser=" + prodUser + ", techUser=" + techUser + ", platformValue="
				+ platformValue + "]";
	}
    
    
    
    
}
