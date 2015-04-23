package com.coolwall.app.models;

import java.sql.Timestamp;

public class Attachment {
	private int id, cover;
	private String fileName, url, description;
	private Timestamp created;

	public Attachment(int id, String url, Timestamp created) {
		this.id = id;
		this.url = url;
		this.created = created;
	}

	public Attachment(int id, String fileName, String url, Timestamp created, int cover, String description) {
		this(id, url, created);
		this.fileName = fileName;
		this.cover = cover;
		this.description = description;
	}

	public int getId() {
		return this.id;
	}

	public String getFileName() {
		return this.fileName;
	}

	public String getUrl() {
		return this.url;
	}

	public Timestamp getCreated() {
		return this.created;
	}

	public String getDescription() {
		return this.description;
	}

	public int getCover() {
		return this.cover;
	}
	
}