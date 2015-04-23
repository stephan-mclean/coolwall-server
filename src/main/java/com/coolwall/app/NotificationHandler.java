package com.coolwall.app;
import com.coolwall.app.models.*;

import spark.*;
import static spark.Spark.*;

import java.sql.*;
import java.util.ArrayList;

public class NotificationHandler {
	private Connection connection;

	public NotificationHandler(Connection connection) {
		this.connection = connection;
	}

	public static void addNotification(ArrayList<Member> users, String title, String subTitle, Connection conn) throws SQLException {
		for(Member m : users) {
			addNotification(m.getUser().getId(), title, subTitle, conn);
		}
	}

	public static void addNotification(int userId, String title, String subTitle, Connection conn) throws SQLException {
		String sql = "INSERT INTO notifications (userId, title, subTitle) VALUES (?, ?, ?)";
		PreparedStatement preparedStatement = conn.prepareStatement(sql);

		preparedStatement.setInt(1, userId);
		preparedStatement.setString(2, title);
		preparedStatement.setString(3, subTitle);
		preparedStatement.executeUpdate();
	}

	public ArrayList<Notification> getNotifications(Request req, Response res, User currentUser) throws SQLException {
		ArrayList<Notification> result = new ArrayList<Notification>();

		String sql = "SELECT * FROM notifications WHERE userId=? AND beenRead != 1";
		PreparedStatement preparedStatement = connection.prepareStatement(sql);
		preparedStatement.setInt(1, currentUser.getId());

		ResultSet rs = preparedStatement.executeQuery();
		while(rs != null && rs.next()) {
			int id = rs.getInt(1);
			String title = rs.getString(3);
			String subTitle = rs.getString(4);
			int read = rs.getInt(5);
			Timestamp created = rs.getTimestamp(6);

			result.add(new Notification(id, read, title, subTitle, created));
		}

		return result;
	}

	public String markAllRead(Request req, Response res, User currentUser) throws SQLException {
		int userId = currentUser.getId();

		String sql = "UPDATE notifications SET beenRead=1 WHERE userId=?";
		PreparedStatement preparedStatement = connection.prepareStatement(sql);
		preparedStatement.setInt(1, userId);

		preparedStatement.executeUpdate();

		return "Notification update success";

	}
	
}