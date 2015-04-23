package com.coolwall.app.models;

import java.util.ArrayList;

public class Lane {
	private int id, wallId;
	private String title, created;
	private ArrayList<Card> cards;

	public Lane(int id, String title, String created, ArrayList<Card> cards) {
		this.id = id; this.title = title;
		this.created = created;
		this.cards = cards;
	}

	public Lane(int id, int wallId, String title, String created) {
		this.id = id; this.wallId = wallId;
		this.title = title;
		this.created = created;
	}

	public Lane(int id, String title) {
		this.id = id;
		this.title = title;
		this.cards = new ArrayList<Card>();
	}

	public int getId() {
		return this.id;
	}

	public int getWallId() {
		return this.wallId;
	}

	public String getTitle() {
		return this.title;
	}

	public String getCreated() {
		return this.created;
	}

	public ArrayList<Card> getCards() {
		return this.cards;
	}

	public void setCards(ArrayList<Card> cards) {
		this.cards = cards;
	}
}