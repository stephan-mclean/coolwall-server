package com.coolwall.app;

import com.coolwall.app.mock.MockRequest;
import com.coolwall.app.mock.MockResponse;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;

import com.google.gson.Gson;

/*
    This class tests the user related functionality
    of the server.
    It tests the user creation with valid & invalid input.
    It tests the login with valid & invalid input.
    It tests getting the current user with valid & invalid input.
*/
public class User extends TestCase {
    private String userName = "SparkUser01";
    private String userEmail = "sparktest01@coolwall.com";
    private String userPassword = "sparkuser01";
    public static String token;
    public static HashMap<String, String> authHeader;
    
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public User( String testName ) {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(new User("testRegister"));
        suite.addTest(new User("testLogin"));
        suite.addTest(new User("testGetCurrentUser"));
        suite.addTest(new User("testInvalidLogin"));
        suite.addTest(new User("testInvalidGetCurrentUser"));
        suite.addTest(new User("testSearchUsers"));
        return suite;
    }

    /*
        Attempt to add a new user to the database.
    */
    public void testRegister() {
        
        HashMap<String, String> userDetails = new HashMap<String, String>();
        userDetails.put("email", userEmail);
        userDetails.put("name", userName);
        userDetails.put("password", userPassword);


        MockResponse response = MockRequest.makeRequest("POST", null, 
            new Gson().toJson(userDetails), "/register");

        assertNotNull(response);

        assertEquals(200, response.getStatus());

        HashMap<String, String> responseBody = response.getJson();
        assertNotNull(responseBody);
        assertEquals(userName, responseBody.get("name"));
        assertEquals(userEmail, responseBody.get("email"));
        assertNotNull(responseBody.get("id"));
        assertNotNull(responseBody.get("token"));

        token = responseBody.get("token");
        authHeader = new HashMap<String, String>();
        authHeader.put("Authorization", token);
    } 

    //public void testInvalidRegister() {
      //  assertTrue(true);
    //}

    /*
        Attempt to login the newly created user from the first test.
    */
    public void testLogin() {
        HashMap<String, String> userDetails = new HashMap<String, String>();
        userDetails.put("email", userEmail);
        userDetails.put("password", userPassword);


        MockResponse response = MockRequest.makeRequest("POST", null, 
            new Gson().toJson(userDetails), "/login");

        assertNotNull(response);

        assertEquals(200, response.getStatus());

        HashMap<String, String> responseBody = response.getJson();
        assertNotNull(responseBody);
        assertEquals(userName, responseBody.get("name"));
        assertEquals(userEmail, responseBody.get("email"));
        assertNotNull(responseBody.get("id"));
        assertNotNull(responseBody.get("token"));
    }

    /*
        Login a user and ensure that the logged in user
        is returned when accessing the /currentUser route
        from the server.
    */
    public void testGetCurrentUser() {

        MockResponse currentUserResponse = MockRequest.makeRequest(
            "GET", authHeader, null, "/currentUser");

        assertNotNull(currentUserResponse);
        assertEquals(200, currentUserResponse.getStatus());

        HashMap<String, String> currentUserBody = currentUserResponse.getJson();
        assertNotNull(currentUserBody);
        assertEquals(userName, currentUserBody.get("name"));
        assertEquals(userEmail, currentUserBody.get("email"));
        assertEquals(token, currentUserBody.get("token"));
    }

    /*
        Test the login route with:
            No credentials,
            Valid email, incorrect password
            Invalid email & password
    */
    public void testInvalidLogin() {
        // No credentials
        MockResponse noCredsResponse = MockRequest.makeRequest("POST", null, null, "/login");
        assertNotNull(noCredsResponse);
        assertEquals(400, noCredsResponse.getStatus());

        // Valid email
        HashMap<String, String> validEmailDetails = new HashMap<String, String>();
        validEmailDetails.put("email", userEmail);
        validEmailDetails.put("password", "foo");

        MockResponse validEmailResponse = MockRequest.makeRequest("POST", null, 
            new Gson().toJson(validEmailDetails), "/login");

        assertNotNull(validEmailResponse);
        assertEquals(401, validEmailResponse.getStatus());

        // Invalid email & password
        HashMap<String, String> invalidEmailDetails = new HashMap<String, String>();
        invalidEmailDetails.put("email", "foo");
        invalidEmailDetails.put("password", "bar");

        MockResponse invalidEmailResponse = MockRequest.makeRequest("POST", null, 
            new Gson().toJson(invalidEmailDetails), "/login");

        assertNotNull(invalidEmailResponse);
        assertEquals(401, invalidEmailResponse.getStatus());

    }

    public void testInvalidGetCurrentUser() {
        MockResponse currentUserResponse = MockRequest.makeRequest(
            "GET", null, null, "/currentUser");

        assertNotNull(currentUserResponse);
        assertEquals(200, currentUserResponse.getStatus());
        assertEquals("null", currentUserResponse.getBody());
    }

    public void testSearchUsers() {
        HashMap<String, String> search = new HashMap<String, String>();
        search.put("term", userName);

        MockResponse searchResponse = MockRequest.makeRequest(
            "GET", authHeader, new Gson().toJson(search), "/searchUsers");

        assertNotNull(searchResponse);
        ArrayList<Map<String, String>> searchResults = 
        searchResponse.getJsonArray();

        assertEquals(1, searchResults.size());
        assertEquals(userName, searchResults.get(0).get("name"));
    }

}
