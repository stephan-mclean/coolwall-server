package com.coolwall.app.mock;

import java.util.HashMap;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import spark.utils.IOUtils;

public class MockRequest {

	/*
		Perform a network request with:
		HTTP Method: requestMethod,
		HTTP Headers: requestHeaders, 
		HTTP body: requestBody and
		the route: path

		This method returns a MockResponse object which contains the
		HTTP response status and response body.
	*/
	public static MockResponse makeRequest(String requestMethod, HashMap<String, String> requestHeaders, String requestBody, String path) {
		MockResponse response = null;
		HttpURLConnection connection = null;
		try {
			URL url = new URL("http://localhost:4567" + path);
			connection = (HttpURLConnection) url.openConnection();
			connection.setDoOutput(true);
			connection.setRequestMethod(requestMethod);

			if(requestHeaders != null) {
				for(String header : requestHeaders.keySet()) {
					connection.setRequestProperty(header, requestHeaders.get(header));
				}
			}

			connection.connect();

			if(requestBody != null) {
				OutputStream os = connection.getOutputStream();
		        os.write(requestBody.getBytes());
		        os.flush();
			}

			String body = IOUtils.toString(connection.getInputStream());
			response = new MockResponse(connection.getResponseCode(), body);


		}
		catch(IOException e) {
			//e.printStackTrace();
			try {
				response = new MockResponse(connection.getResponseCode(), null);
			}
			catch(IOException e1) {
				return null;
			}
		}

		return response;
	}
	
}