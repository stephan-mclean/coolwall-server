/*
	This file handles Wall related functionality.

	TODO: Lots of error checking for each function - e.g can a user
	delete a wall if they're not moderator etc.
*/

package com.coolwall.app;

import com.coolwall.app.models.*;

import spark.*;
import static spark.Spark.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

import com.google.gson.Gson;

public class WallHandler {
	private Connection connection;

	public WallHandler(Connection connection) {
		this.connection = connection;
	}

	/*
		Returns a list of all the walls the current user is
		a member of
	*/
	public ArrayList<Wall> getWalls(Request req, Response res, User currentUser) throws SQLException {
		ArrayList<Wall> result = new ArrayList<Wall>();

		String sql = "SELECT walls.id, walls.title, walls.created, walls.numUsers, members.moderator FROM (SELECT * FROM walls_with_count WHERE id IN (SELECT wallId FROM wall_members WHERE wall_members.userId = ?)) walls left join (SELECT moderator, wallId from wall_members WHERE wall_members.userId = ?) members ON walls.id = members.wallId";
		PreparedStatement preparedStatement = connection.prepareStatement(sql);
		preparedStatement.setInt(1, currentUser.getId());
		preparedStatement.setInt(2, currentUser.getId());

		ResultSet rs = preparedStatement.executeQuery();
		while(rs.next()) {
			int id = rs.getInt(1);
			String title = rs.getString(2);
			Timestamp created = rs.getTimestamp(3);
			int numUsers = rs.getInt(4);
			int moderator = rs.getInt(5);
			result.add(new Wall(id, title, created, numUsers, moderator));
		}

		return result;
	}

	/*
		Get the wall with the ID sent in the Request object.

		TODO: Error handling to ensure user is part of the given wall.
	*/
	public Wall getWall(Request req, Response res, User currentUser) throws SQLException {
		int id = Integer.parseInt(req.params(":id"));
		if(checkWallMembership(id, currentUser.getId(), connection) > -1) {
			Wall result =  getWallWithId(id, currentUser, connection);
			ArrayList<Lane> lanes = getLanes(id);
			result.setLanes(lanes);
			return result;
		}
		else {
			halt(403, "You do not have access to this wall.");
			return null;
		}
	}

	public static int checkWallMembership(int wallId, int userId, Connection conn) throws SQLException {
		String sql = "SELECT * FROM wall_members WHERE wallId=? AND userId=?";
		PreparedStatement preparedStatement = conn.prepareStatement(sql);
		preparedStatement.setInt(1, wallId);
		preparedStatement.setInt(2, userId);

		ResultSet rs = preparedStatement.executeQuery();

		if(rs != null && rs.next()) {
			return rs.getInt(3);
		}
		else {
			return -1;
		}
	}

	/*
		Add a lane to the Wall & return the Lane
	*/
	public Lane addLane(Request req, Response res, User currentUser) throws SQLException {
		int wallId = Integer.parseInt(req.params(":id"));
		if(checkWallMembership(wallId, currentUser.getId(), connection) > -1) {
			String body = req.body();
			HashMap<String, String> lane = new Gson().fromJson(body, HashMap.class);

			if(lane != null && lane.containsKey("title") && 
				lane.get("title") != null && lane.get("title").length() > 0) {

				String title = lane.get("title");

				String sql = "INSERT INTO lanes (wallId, title) VALUES (?, ?)";
				PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
				preparedStatement.setInt(1, wallId);
				preparedStatement.setString(2, title);
				preparedStatement.executeUpdate();

				ResultSet generatedKeys = preparedStatement.getGeneratedKeys();
				int id = -1;
				if(generatedKeys.next())
					id = generatedKeys.getInt(1);

				return new Lane(id, title);
			}
			else {
				halt(400, "You need to provide a title for the lane.");
				return null;
			}
		}
		else {
			halt(403, "You do not have access to this wall.");
			return null;
		}
	}

	/*
		Delete the wall with the given ID

		TODO: Make sure user is a moderator, if not they can't delete the wall
	*/
	public String deleteWall(Request req, Response res, User currentUser) throws SQLException {
		int wallId = Integer.parseInt(req.params(":id"));

		if(checkWallMembership(wallId, currentUser.getId(), connection) == 1) {
			String sql = "DELETE FROM walls WHERE id=?";
			PreparedStatement preparedStatement = connection.prepareStatement(sql);
			preparedStatement.setInt(1, wallId);
			preparedStatement.executeUpdate();
			deleteWallLanes(wallId);
			deleteWallMembers(wallId);
			return "Success";
		}
		else {
			halt(403, "You do not have permission to delete this wall.");
			return "Failure";
		}
	}

	/*
		Update the Wall with the new data sent in the request.
	*/
	public String updateWall(Request req, Response res, User currentUser) throws SQLException {
		int wallId = Integer.parseInt(req.params(":id"));
		if(checkWallMembership(wallId, currentUser.getId(), connection) > -1) {
			String body = req.body();
			HashMap<String, String> wall = new Gson().fromJson(body, HashMap.class);

			if(wall != null && wall.containsKey("title") && wall.get("title").length() > 0) {
				String title = wall.get("title");

				String sql = "UPDATE walls SET title=? WHERE id=?";
				PreparedStatement preparedStatement = connection.prepareStatement(sql);
				preparedStatement.setString(1, title);
				preparedStatement.setInt(2, wallId);
				preparedStatement.executeUpdate();
				return "Wall Update Success";
			}
			else {
				halt(400, "You need to provide an updated wall title");
				return null;
			}
		}
		else {
			halt(403, "You do not have access to this wall.");
			return null;
		}
	}

	private void deleteWallMembers(int wallId) throws SQLException {
		String sql = "DELETE FROM wall_members WHERE wallId=?";
		PreparedStatement preparedStatement = connection.prepareStatement(sql);
		preparedStatement.setInt(1, wallId);
		preparedStatement.executeUpdate();
	}

	/*
		Delete all lanes with wallId & also delete all their content.
		TODO: Delete Lane cards
	*/
	private void deleteWallLanes(int wallId) throws SQLException {
		String sql = "SELECT * FROM lanes WHERE wallId=?";
		PreparedStatement preparedStatement = connection.prepareStatement(sql, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
		preparedStatement.setInt(1, wallId);
		ResultSet rs = preparedStatement.executeQuery();

		while(rs.next()) {
			int laneId = rs.getInt(1);
			LaneHandler.deleteLaneCards(laneId, connection);
			rs.deleteRow();
		}
		
	}

	private ArrayList<Lane> getLanes(int wallId) throws SQLException {
		ArrayList<Lane> result = new ArrayList<Lane>();
		
		String sql = "SELECT * FROM lanes WHERE wallId = ?";
		PreparedStatement preparedStatement = connection.prepareStatement(sql);
		preparedStatement.setInt(1, wallId);
		ResultSet rs = preparedStatement.executeQuery();

		while(rs.next()) {
			int id = rs.getInt(1);
			String title = rs.getString(3);
			String created = rs.getString(4);
			ArrayList<Card> cards = getCards(id);
			result.add(new Lane(id, title, created, cards));
		}

		return result;
	}

	private ArrayList<Card> getCards(int laneId) throws SQLException {
		ArrayList<Card> result = new ArrayList<Card>();

		String sql = "SELECT * FROM cards WHERE laneId = ?";
		PreparedStatement preparedStatement = connection.prepareStatement(sql);
		preparedStatement.setInt(1, laneId);
		ResultSet rs = preparedStatement.executeQuery();

		while(rs.next()) {
			int id = rs.getInt(1);
			int ordinal = rs.getInt(3);
			String title = rs.getString(4);
			String description = rs.getString(5);
			String created = rs.getString(6);
			String cover = rs.getString(7);
			int wallId = rs.getInt(8);
			result.add(new Card(id, ordinal, title, description, created, cover, wallId));
		}

		return result;
	}

	public static Wall getWallWithId(int id, User currentUser, Connection conn) throws SQLException {
		Wall result = null;

		String sql = "SELECT walls.id, walls.title, walls.created, walls.numUsers, members.moderator FROM (SELECT * FROM walls_with_count WHERE id = ?) walls left join (SELECT moderator, wallId from wall_members WHERE wall_members.userId = ?) members ON walls.id = members.wallId";
		PreparedStatement preparedStatement = conn.prepareStatement(sql);
		preparedStatement.setInt(1, id);
		preparedStatement.setInt(2, currentUser.getId());

		ResultSet rs = preparedStatement.executeQuery();
		if(rs.next()) {
			String title = rs.getString(2);
			Timestamp created = rs.getTimestamp(3);
			int numUsers = rs.getInt(4);
			int moderator = rs.getInt(5);
			result = new Wall(id, title, created, numUsers, moderator);
		}

		return result;
	}

	

	/*
		Create a new Wall & return it
	*/
	public Wall newWall(Request req, Response res, User currentUser) throws SQLException {
		String body = req.body();
		HashMap<String, String> wall = new Gson().fromJson(body, HashMap.class);

		if(wall != null && wall.containsKey("title") && wall.get("title").length() > 0) {
			String title = wall.get("title");
			String sql = "INSERT INTO walls (title) VALUES (?)";
			PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			preparedStatement.setString(1, title);
			preparedStatement.executeUpdate();

			ResultSet generatedKeys = preparedStatement.getGeneratedKeys();
			int id = -1;
			if(generatedKeys.next())
				id = generatedKeys.getInt(1);

			addWallMember(id, currentUser.getId(), 1);

			return getWallWithId(id, currentUser, connection);
		}
		else {
			halt(400, "You must provide a valid title for the new wall.");
			return null;
		}
	}

	public Member addMember(Request req, Response res, User currentUser) throws SQLException {
		int wallId = Integer.parseInt(req.params(":id"));
		Wall wall = getWallWithId(wallId, currentUser, connection);
		if(wall != null && checkWallMembership(wallId, currentUser.getId(), connection) > -1) {
			String body = req.body();
			HashMap<String, String> member = new Gson().fromJson(body, HashMap.class);

			if(member != null && member.containsKey("id") && 
				member.get("id") != null && member.get("id").length() > 0
				&& member.containsKey("moderator") && 
				member.get("moderator") != null && 
				member.get("moderator").length() > 0) {

				int memberId = Integer.parseInt(member.get("id"));
				User newMember = UserHandler.getUserWithId(memberId, connection);
				if(newMember != null && memberId != currentUser.getId()) {

					// Not already a member
					if(checkWallMembership(wallId, memberId, connection) < 0) {
						int moderator = Integer.parseInt(member.get("moderator"));
						addWallMember(wallId, memberId, moderator);

						User memberUser = UserHandler.getUserWithId(memberId, this.connection);
						Timestamp since = new Timestamp(new java.util.Date().getTime());

						// Create a notification for the user being added
						NotificationHandler.addNotification(memberId, currentUser.getName() + 
							" added you to the wall '" + wall.getTitle() + "'", null, connection);

						return new Member(memberUser, since, moderator);
					}
					else {
						halt(400, "User is already a member");
						return null;
					}
				}
				else {
					halt(400, "You cannot add that user.");
					return null;
				}
			}
			else {
				halt(400, "You must provide a valid member to add.");
				return null;
			}
		}
		else {
			halt(403, "You do not have access to this wall.");
			return null;
		}
	}

	public String deleteMember(Request req, Response res, User currentUser) throws SQLException {
		int wallId = Integer.parseInt(req.params(":id"));
		Wall wall = getWallWithId(wallId, currentUser, connection);

		int memberId = Integer.parseInt(req.params(":memberId"));
		/*
			Can only remove a Wall member if the current user is 
			a moderator or if they are removing themselves.
		*/
		if(wall != null && (checkWallMembership(wallId, currentUser.getId(), connection) == 1 || 
			memberId == currentUser.getId())) {
			String sql = "DELETE FROM wall_members WHERE wallId=? AND userId=?";
			PreparedStatement preparedStatement = connection.prepareStatement(sql);
			preparedStatement.setInt(1, wallId);
			preparedStatement.setInt(2, memberId);
			preparedStatement.executeUpdate();

			// Generate a notification
			if(memberId != currentUser.getId()) {
				NotificationHandler.addNotification(memberId, currentUser.getName() + 
					" removed you from the wall '" + wall.getTitle() + "'", null, connection);
			}

			return "Member delete success";

		}
		else {
			halt(400, "You do not have permission to remove this member");
			return "Failure";
		}
		
	}

	public ArrayList<Member> getMembers(Request req, Response res, User currentUser) throws SQLException {
		int wallId = Integer.parseInt(req.params(":id"));
		if(checkWallMembership(wallId, currentUser.getId(), connection) > -1) {
			ArrayList<Member> result = new ArrayList<Member>();

			String sql = "SELECT users.id, users.name, users.email, members.since, members.moderator FROM (SELECT * FROM wall_members WHERE wallId=?) members LEFT JOIN (SELECT id, name, email FROM users) users ON members.userId = users.id";
			PreparedStatement preparedStatement = connection.prepareStatement(sql);
			preparedStatement.setInt(1, wallId);

			ResultSet rs = preparedStatement.executeQuery();
			while(rs.next()) {
				int id = rs.getInt(1);
				String name = rs.getString(2);
				String email = rs.getString(3);
				Timestamp since = rs.getTimestamp(4);
				int moderator = rs.getInt(5);
				result.add(new Member(new User(id, name, email), since, moderator));
			}

			return result;
		}
		else {
			halt(403, "You do not have access to this wall.");
			return null;
		}
	}

	public ArrayList<User> searchMembers(Request req, Response res, User currentUser) throws SQLException {
		int wallId = Integer.parseInt(req.params(":id"));
		if(checkWallMembership(wallId, currentUser.getId(), connection) > -1) {
			String body = req.body();
			HashMap<String, String> search = new Gson().fromJson(body, HashMap.class);

			if(search != null && search.containsKey("term")) {

				String searchTerm = search.get("term");
				ArrayList<User> result = new ArrayList<User>();

				// Return empty result if empty search provided.
				if(searchTerm.length() > 0) {

					String sql = "SELECT id, name, email FROM users WHERE id IN (SELECT userId FROM wall_members WHERE wallId=?) AND name LIKE ?";
					PreparedStatement preparedStatement = connection.prepareStatement(sql);
					preparedStatement.setInt(1, wallId);
					preparedStatement.setString(2, "%" + searchTerm + "%");

					ResultSet rs = preparedStatement.executeQuery();
					while(rs.next()) {
						int id = rs.getInt(1);
						String name = rs.getString(2);
						String email = rs.getString(3);
						result.add(new User(id, name, email));
					}
				}

				return result;
			}
			else {
				halt(400, "You must provide a search term");
				return null;
			}
		}
		else {
			halt(403, "You do not have access to this wall.");
			return null;
		}
	}

	/*
		Create an entry in the wall_members table with the wallId, userId &
		moderator values.
	*/
	private void addWallMember(int wallId, int userId, int moderator) throws SQLException {
		String sql = "INSERT INTO wall_members (wallId, userId, moderator) VALUES (?, ?, ?)";
		PreparedStatement preparedStatement = connection.prepareStatement(sql);
		preparedStatement.setInt(1, wallId);
		preparedStatement.setInt(2, userId);
		preparedStatement.setInt(3, moderator);
		preparedStatement.executeUpdate();
	}

}