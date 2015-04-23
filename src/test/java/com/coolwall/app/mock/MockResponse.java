package com.coolwall.app.mock;

import com.google.gson.Gson;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;

public class MockResponse {
	private final String body;
	private final int status;

	public MockResponse(int status, String body) {
		this.body = body;
		this.status = status;
	}

	public String getBody() {
		return this.body;
	}

	public int getStatus() {
		return this.status;
	}

	/*
		Return the request body as a HashMap
		Example a user JSON object: {'name': 'Stephan McLean', 'id': '13'}
		would be returned as a Java HashMap representing the JSON
	*/
	public HashMap<String, String> getJson() {
		return new Gson().fromJson(this.body, HashMap.class);
	}

	/*
		As above but returns an ArrayList of HashMaps 
		instead of just a HashMap. Used when the response body
		is a list of JSON objects.
	*/
	public ArrayList<Map<String, String>> getJsonArray() {
		return new Gson().fromJson(this.body, ArrayList.class);
	}
	
}