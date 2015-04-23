package com.coolwall.app.models;

import java.sql.Timestamp;

public class Member {
	private User user;
	private Timestamp since;
	private int moderator;

	public Member(User user, Timestamp since) {
		this.user = user;
		this.since = since;
	}

	public Member(User user, Timestamp since, int moderator) {
		this(user, since);
		this.moderator = moderator;
	}

	public User getUser() {
		return this.user;
	}

	public Timestamp getSince() {
		return this.since;
	}

	public int getModerator() {
		return this.moderator;
	}
}