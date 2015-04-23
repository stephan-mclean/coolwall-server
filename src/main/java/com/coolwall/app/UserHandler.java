package com.coolwall.app;

import com.coolwall.app.models.User;

import spark.*;

import java.sql.*;
import java.util.HashMap;
import java.util.ArrayList;

import com.google.gson.Gson;

import org.mindrot.jbcrypt.BCrypt;

public class UserHandler {
	private Connection connection;

	public UserHandler(Connection connection) {
		this.connection = connection;
	}

	public User getCurrentUser(Request req) throws SQLException {
		User result = null;

		String token = req.headers("Authorization");
		String sql = "SELECT * FROM users WHERE token = ?";
		PreparedStatement preparedStatement = connection.prepareStatement(sql);
		preparedStatement.setString(1, token);

		ResultSet rs = preparedStatement.executeQuery();
		if(rs.next()) {
			int id = rs.getInt(3);
			String name = rs.getString(4);
			String email = rs.getString(1);
			result = new User(id, name, email, token);
		}

		return result;
	}

	/*
		This method can be used by other classes to 
		retrieve a particular user. Need to pass in connection
		as method is static 
		
		TODO: Error Handling
	*/
	public static User getUserWithId(int id, Connection conn) throws SQLException {
		User result = null;

		String sql = "SELECT * FROM users WHERE id = ?";
		PreparedStatement preparedStatement = conn.prepareStatement(sql);
		preparedStatement.setInt(1, id);

		ResultSet rs = preparedStatement.executeQuery();
		if(rs.next()) {
			String name = rs.getString(4);
			String email = rs.getString(1);
			result = new User(id, name, email);
		}

		return result;
	}

	public ArrayList<User> searchUsers(Request req, Response res, User currentUser) throws SQLException {
		String body = req.body();
		HashMap<String, String> search = new Gson().fromJson(body, HashMap.class);
		String searchTerm = search.get("term");

		ArrayList<User> result = new ArrayList<User>();

		String sql = "SELECT id, name, email FROM users WHERE name LIKE ?";
		PreparedStatement preparedStatement = connection.prepareStatement(sql);
		preparedStatement.setString(1, "%" + searchTerm + "%");

		ResultSet rs = preparedStatement.executeQuery();
		while(rs.next()) {
			int id = rs.getInt(1);
			String name = rs.getString(2);
			String email = rs.getString(3);
			result.add(new User(id, name, email));
		}

		return result;
	}

	public User register(Request req, Response res) throws SQLException {
		String body = req.body();
		HashMap<String, String> user = new Gson().fromJson(body, HashMap.class);

		String email = user.get("email");
		String name = user.get("name");
		String password = user.get("password");

		String hashedPW = BCrypt.hashpw(password, BCrypt.gensalt());
		String token = BCrypt.hashpw(password + email, BCrypt.gensalt());

		/* Insert user into DB */
		String sql = "INSERT INTO users (email, password, name, token) VALUES (?, ?, ?, ?)";
		PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
		preparedStatement.setString(1, email);
		preparedStatement.setString(2, hashedPW);
		preparedStatement.setString(3, name);
		preparedStatement.setString(4, token);
		preparedStatement.executeUpdate();

		ResultSet generatedKeys = preparedStatement.getGeneratedKeys();
		int id = -1;
		if(generatedKeys.next())
			id = generatedKeys.getInt(1);

		return new User(id, name, email, token);
	}

}