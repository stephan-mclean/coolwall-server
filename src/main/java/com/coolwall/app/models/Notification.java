package com.coolwall.app.models;

import java.sql.Timestamp;

public class Notification {
	private int id, read;
	private String title, subTitle;
	private Timestamp created;

	public Notification(int id, int read, String title, String subTitle, Timestamp created) {
		this.id = id;
		this.read = read;
		this.title = title;
		this.subTitle = subTitle;
		this.created = created;
	}

	public int getId() {
		return this.id;
	}	

	public int getRead() {
		return this.read;
	}

	public String getTitle() {
		return this.title;
	}

	public String getSubtitle() {
		return this.subTitle;
	}

	public Timestamp getCreated() {
		return this.created;
	}
}