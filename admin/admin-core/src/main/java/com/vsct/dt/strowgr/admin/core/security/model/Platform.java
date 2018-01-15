package com.vsct.dt.strowgr.admin.core.security.model;

public enum Platform {

	PRODUCTION("production");
	
	private String value;
	
	Platform(String value) {
		this.value	=	value;
	}
	
	public String value() {
		return this.value;
	}
}
