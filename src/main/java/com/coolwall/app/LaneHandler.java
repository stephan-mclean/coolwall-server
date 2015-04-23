package com.coolwall.app;

import com.coolwall.app.models.*;

import spark.*;
import static spark.Spark.*;

import java.sql.*;
import java.util.HashMap;

import com.google.gson.Gson;

public class LaneHandler {
	private Connection connection;

	public LaneHandler(Connection connection) {
		this.connection = connection;
	}

	public Card addCard(Request req, Response res, User currentUser) throws SQLException {
		int laneId = Integer.parseInt(req.params(":id"));
		Lane lane = getLaneWithId(laneId);
		if(lane != null) {
			Wall parent = WallHandler.getWallWithId(lane.getWallId(), currentUser, connection);
			if(parent != null && WallHandler.checkWallMembership(parent.getId(), currentUser.getId(), connection) > -1) {
				String body = req.body();
				HashMap<String, String> card = new Gson().fromJson(body, HashMap.class);

				if(card != null && card.containsKey("title") && 
					card.get("title").length() > 0 && card.containsKey("ordinal")
					&& card.get("ordinal").length() > 0) {

					String title = card.get("title");
					int ordinal = Integer.parseInt(card.get("ordinal"));

					String sql = "INSERT INTO cards (laneId, ordinal, title, wallId) VALUES (?, ?, ?, ?)";
					PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
					preparedStatement.setInt(1, laneId);
					preparedStatement.setInt(2, ordinal);
					preparedStatement.setString(3, title);
					preparedStatement.setInt(4, parent.getId());
					preparedStatement.executeUpdate();

					ResultSet generatedKeys = preparedStatement.getGeneratedKeys();
					int id = -1;
					if(generatedKeys.next())
						id = generatedKeys.getInt(1);

					return new Card(id, ordinal, title, parent.getId());
				}
				else {
					halt(400, "You must provide valid card details");
					return null;
				}
			}
			else {
				halt(400, "You do not have access to the lanes wall.");
				return null;
			}
			
		}
		else {
			halt(400, "Cannot add a card to that lane, the lane was not found");
			return null;
		}
	}

	public String updateLane(Request req, Response res, User currentUser) throws SQLException {
		int laneId = Integer.parseInt(req.params(":id"));
		Lane lane = getLaneWithId(laneId);
		if(lane != null) {
			Wall laneParent = WallHandler.getWallWithId(lane.getWallId(), currentUser, connection);
			if(laneParent != null && 
				WallHandler.checkWallMembership(laneParent.getId(), currentUser.getId(), connection) > -1) {
				
				String body = req.body();
				HashMap<String, String> laneUpdate = new Gson().fromJson(body, HashMap.class);

				if(laneUpdate != null && laneUpdate.containsKey("title") &&
					laneUpdate.get("title").length() > 0) {
					String title = laneUpdate.get("title");

					String sql = "UPDATE lanes SET title=? WHERE id=?";
					PreparedStatement preparedStatement = connection.prepareStatement(sql);
					preparedStatement.setString(1, title);
					preparedStatement.setInt(2, laneId);
					preparedStatement.executeUpdate();
					return "Lane Update Success";
				}
				else {
					halt(400, "You must provide valid details to update the lane");
					return "Failure";
				}
			}
			else {
				halt(400, "You do not have access to the lanes wall");
				return "Failure";
			}
		}
		else {
			halt(400, "Cannot update that lane, the lane was not found");
			return "Failure";
		}
	}

	public String deleteLane(Request req, Response res, User currentUser) throws SQLException {
		int laneId = Integer.parseInt(req.params(":id"));
		Lane lane = getLaneWithId(laneId);
		if(lane != null) {
			Wall laneParent = WallHandler.getWallWithId(lane.getWallId(), currentUser, connection);
			if(laneParent != null && 
				WallHandler.checkWallMembership(laneParent.getId(), currentUser.getId(), connection) > -1) {
				deleteLaneWithId(laneId);
				return "Success";
			}
			else {
				halt(400, "Cannot delete lane, you do not have access to that lanes wall");
				return "Failure";
			}
		}
		else {
			halt(400, "Cannot delete that lane, the lane was not found");
			return "Failure";
		}
	}

	/*
		Private helper method to delete a single lane & all it's content
	*/
	private void deleteLaneWithId(int laneId) throws SQLException {
		String deleteLaneSql = "DELETE FROM lanes WHERE id=?";

		PreparedStatement laneStatement = connection.prepareStatement(deleteLaneSql);
		laneStatement.setInt(1, laneId);
		laneStatement.executeUpdate();
		deleteLaneCards(laneId, connection);
	}

	public static void deleteLaneCards(int laneId, Connection conn) throws SQLException {
		String sql = "SELECT * FROM cards WHERE laneId=?";
		PreparedStatement preparedStatement = conn.prepareStatement(sql, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
		preparedStatement.setInt(1, laneId);
		ResultSet rs = preparedStatement.executeQuery();

		while(rs.next()) {
			int cardId = rs.getInt(1);
			CardHandler.deleteCardContent(cardId, conn);
			rs.deleteRow(); // Delete the actual card
		}
	}

	private Lane getLaneWithId(int laneId) throws SQLException {
		Lane result = null;

		String sql = "SELECT * FROM lanes WHERE id=?";
		PreparedStatement preparedStatement = connection.prepareStatement(sql);
		preparedStatement.setInt(1, laneId);

		ResultSet rs = preparedStatement.executeQuery();
		if(rs != null && rs.next()) {
			int wallId = rs.getInt(2);
			String title = rs.getString(3);
			String created = rs.getString(4);

			result = new Lane(laneId, wallId, title, created);
		}

		return result;
	}
}