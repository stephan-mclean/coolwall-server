package com.coolwall.app.models;

public class Card {
	private int id, ordinal, wallId;
	private String title, description, created, cover;

	public Card(int id, int ordinal, String title, int wallId) {
		this.id = id; this.ordinal = ordinal;
		this.title = title;
		this.wallId = wallId;
	}

	public Card(int id, int ordinal, String title, String description, String created, String cover, int wallId) {
		this(id, ordinal, title, wallId);
		this.description = description;
		this.created = created;
		this.cover = cover;
	}

	public int getId() {
		return this.id;
	}

	public int getWallId() {
		return this.wallId;
	}

	public int getOrdinal() {
		return this.ordinal;
	}

	public String getTitle() {
		return this.title;
	}

	public String getDescription() {
		return this.description;
	}

	public String getCreated() {
		return this.created;
	}

	public String getCover() {
		return this.cover;
	}
}