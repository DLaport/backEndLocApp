package fr.upmc.gestionusers.model;

import java.io.Serializable;

public class Credentials implements Serializable {
	private static final long serialVersionUID = 1L;
	private String identifier;
    private String password;
	public String getIdentifier() {
		return identifier;
	}
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
}
