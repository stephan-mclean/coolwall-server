/*
	This class holds the implementation for the 
	routes related to authentication.
	It handles the user login.

	Author: Stephan McLean.
*/

package com.coolwall.app;
import com.coolwall.app.models.User;

import spark.*;
import static spark.Spark.*;

import java.sql.*;
import java.util.HashMap;

import com.google.gson.Gson;

import org.mindrot.jbcrypt.BCrypt;

public class AuthenticationHandler {
	private Connection connection;

	public AuthenticationHandler(Connection connection) {
		this.connection = connection;
	}

	/*
		Check the users credentials & return the user
		if the credentials are correct.
	*/
	public User login(Request req, Response res) throws SQLException {
		String body = req.body();
		HashMap<String, String> user = new Gson().fromJson(body, HashMap.class);

		if(user != null && user.containsKey("email") && user.containsKey("password")) {
			String email = user.get("email");
			String password = user.get("password");

			String name = "";
			String token = "";
			int id = -1;

			// Try and select a user and check password
			String sql = "SELECT * from users WHERE email = ?";
			PreparedStatement preparedStatement = connection.prepareStatement(sql);
			preparedStatement.setString(1, email);

			ResultSet rs = preparedStatement.executeQuery();
			if(rs != null && rs.first()) {
				String hashedPw = rs.getString(2);
				if(BCrypt.checkpw(password, hashedPw)) {
					id = rs.getInt(3);
					name = rs.getString(4);
					token = rs.getString(5);
					return new User(id, name, email, token);	
				}
				else {
					halt(401, "Invalid username or password");
					return null;
				}
				
			}
			else {
				halt(401, "Invalid username or password");
				return null;
			}
		}
		else {
			halt(400, "You need to provide an email and password");
			return null;
		}
	}
}