package com.coolwall.app.models;

import java.sql.Timestamp;

public class Comment {
	private int id;
	private String text;
	private User user;
	private Timestamp created;

	public Comment(int id, String text, Timestamp created, User user) {
		this.id = id; 
		this.text = text;
		this.created = created; 
		this.user = user;
	}

	public int getId() {
		return this.id;
	}

	public String getText() {
		return this.text;
	}

	public User getUser() {
		return this.user;
	}

	public Timestamp getCreated() {
		return this.created;
	}
}