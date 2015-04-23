package com.coolwall.app.models;

import java.util.ArrayList;
import java.sql.Timestamp;

public class Wall {
	private int id, numMembers, moderator;
	private String title;
	private Timestamp created;
	private ArrayList lanes;

	public Wall(int id, String title, Timestamp created, int numMembers, int moderator) {
		this.id = id; this.title = title;
		this.created = created;
		this.numMembers = numMembers;
		this.moderator = moderator;
	}

	public int getId() {
		return this.id;
	}

	public String getTitle() {
		return this.title;
	}

	public Timestamp getCreated() {
		return this.created;
	}

	public int getNumMembers() {
		return this.numMembers;
	}

	public int getModerator() {
		return this.moderator;
	}

	public ArrayList<Lane> getLanes() {
		return this.lanes;
	}

	public void setLanes(ArrayList<Lane> lanes) {
		this.lanes = lanes;
	}
}