package com.coolwall.app.models;

public class User {
	private int id;
	private String name, email, password, token;

	public User(int id, String name, String email, String token) {
		/* Constructor used to send current user token to Front end */
		this.id = id;
		this.name = name;
		this.email = email;
		this.token = token;
	}

	public User(int id, String name, String email) {
		this.id = id; this.name = name;
		this.email = email;
	}

	public int getId() {
		return this.id;
	}

	public String getName() {
		return this.name;
	}

	public String getEmail() {
		return this.email;
	}

	public String getToken() {
		return this.token;
	}

	public String toString() {
		return this.name;
	}
}