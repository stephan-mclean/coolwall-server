package com.coolwall.app;

import com.coolwall.app.models.*;

import spark.*;
import static spark.Spark.*;

import java.sql.*;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;

import java.io.IOException;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.File;


import com.google.gson.Gson;

public class CardHandler {
	private Connection connection;

	public CardHandler(Connection connection) {
		this.connection = connection;
	}

	/* 
		Save the new data for a card.
	*/
	public String updateCard(Request req, Response res, User currentUser) throws SQLException {
		int cardId = Integer.parseInt(req.params(":id"));
		Card card = getCardWithId(cardId);
		if(card != null) {
			Wall cardParent = WallHandler.getWallWithId(card.getWallId(), currentUser, connection);
			if(cardParent != null && WallHandler.checkWallMembership(cardParent.getId(), currentUser.getId(), connection) > -1) {
				String body = req.body();
				HashMap<String, String> update = new Gson().fromJson(body, HashMap.class);
				if(update != null && update.keySet().size() > 0) {	
					String title = null;
					String description = null;
					String laneId = null;
					String ordinal = null;
					
					if(update.containsKey("title")) {
						title = update.get("title");
					}

					if(update.containsKey("description")) {
						description = update.get("description");
					}

					if(update.containsKey("laneId")) {
						laneId = update.get("laneId");
					}

					if(update.containsKey("ordinal")) {
						ordinal = update.get("ordinal");
					}

					String sql = "UPDATE cards SET title=COALESCE(?, title), description=COALESCE(?, description), laneId=COALESCE(?, laneId), ordinal=COALESCE(?, ordinal) WHERE id=?";
					PreparedStatement preparedStatement = connection.prepareStatement(sql);
					preparedStatement.setString(1, title);
					preparedStatement.setString(2, description);
					preparedStatement.setString(3, laneId);
					preparedStatement.setString(4, ordinal);
					preparedStatement.setInt(5, cardId);
					preparedStatement.executeUpdate();
					return "Card Update Success";
				}
				else {
					halt(400, "You must provide valid details to update the card");
					return "Failure";
				}
			}
			else {
				halt(400, "You do not have access to the cards wall");
				return "Failure";
			}
		}
		else {
			halt(400, "Cannot update the card, the card was not found");
			return "Failure";
		}
	}

	/*
		Delete the card with the given ID and all of it's content.
	*/
	public String deleteCard(Request req, Response res, User currentUser) throws SQLException {
		int cardId = Integer.parseInt(req.params(":id"));
		Card card = getCardWithId(cardId);
		if(card != null) {
			Wall cardParent = WallHandler.getWallWithId(card.getWallId(), currentUser, connection);
			if(cardParent != null && WallHandler.checkWallMembership(cardParent.getId(), currentUser.getId(), connection) > -1) {
				String sql = "DELETE FROM cards WHERE id=?";
				PreparedStatement preparedStatement = connection.prepareStatement(sql);
				preparedStatement.setInt(1, cardId);
				preparedStatement.executeUpdate();

				deleteCardContent(cardId, connection);

				return "Card Delete Success";
			}
			else {
				halt(400, "You do not have access to the cards wall");
				return "Failure";
			}
		}
		else {
			halt(400, "Cannot delete the card, the card was not found");
			return "Failure";
		}
	}

	/*
		Add a comment to the card
	*/
	public Comment addComment(Request req, Response res, User currentUser) throws SQLException {
		int cardId = Integer.parseInt(req.params(":id"));
		Card card = getCardWithId(cardId);
		if(card != null) {
			Wall cardParent = WallHandler.getWallWithId(card.getWallId(), currentUser, connection);
			if(cardParent != null && WallHandler.checkWallMembership(cardParent.getId(), currentUser.getId(), connection) > -1) {
				String body = req.body();
				HashMap<String, String> comment = new Gson().fromJson(body, HashMap.class);

				if(comment != null && comment.containsKey("text") && comment.get("text").length() > 0) {

					String text = comment.get("text");
					int userId = currentUser.getId();

					String sql = "INSERT INTO card_comments (cardId, userId, text) VALUES (?, ?, ?)";

					PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
					preparedStatement.setInt(1, cardId);
					preparedStatement.setInt(2, userId);
					preparedStatement.setString(3, text);
					preparedStatement.executeUpdate();

					ResultSet generatedKeys = preparedStatement.getGeneratedKeys();
					int id = -1;
					Timestamp created = new Timestamp(new java.util.Date().getTime());
					if(generatedKeys.next()) {
						id = generatedKeys.getInt(1);
					}

					// Don't include token in comment user
					User commentUser = new User(userId, currentUser.getName(), currentUser.getEmail());

					NotificationHandler.addNotification(getCardMembers(cardId), 
						currentUser.getName() + " commented on the card '"  + 
						card.getTitle() + "'" + " in wall '" + cardParent.getTitle() + "'", 
						text, connection); 

					return new Comment(id, text, created, commentUser);
				}
				else {
					halt(400, "You must provide a valid comment to add");
					return null;
				}
			}
			else {
				halt(400, "You do not have access to the cards wall");
				return null;
			}
		}
		else {
			halt(400, "Cannot add a comment to the card, the card was not found");
			return null;
		}
	}

	public Member addMember(Request req, Response res, User currentUser) throws SQLException {
		int cardId = Integer.parseInt(req.params(":id"));
		Card card = getCardWithId(cardId);
		if(card != null) {
			Wall cardParent = WallHandler.getWallWithId(card.getWallId(), currentUser, connection);
			if(cardParent != null && WallHandler.checkWallMembership(cardParent.getId(), currentUser.getId(), connection) > -1) {
				String body = req.body();
				HashMap<String, String> member = new Gson().fromJson(body, HashMap.class);

				if(member != null && member.containsKey("id") && member.get("id").length() > 0) {
					int memberId = Integer.parseInt(member.get("id"));
					User cardMember = UserHandler.getUserWithId(memberId, this.connection);

					if(cardMember != null) {

						String sql = "INSERT INTO card_members (cardId, userId) VALUES (?, ?)";
						PreparedStatement preparedStatement = connection.prepareStatement(sql);
						preparedStatement.setInt(1, cardId);
						preparedStatement.setInt(2, memberId);
						preparedStatement.executeUpdate();

						
						Timestamp since = new Timestamp(new java.util.Date().getTime());

						if(memberId != currentUser.getId()) {
							NotificationHandler.addNotification(memberId, 
								currentUser.getName() + " added you to the card '" + 
								card.getTitle() + "'" + "in wall '" + cardParent.getTitle() + "'", null, connection);
						}

						return new Member(cardMember, since);
					}
					else {
						halt(400, "Cannot add member, the user does not exist");
						return null;
					}
				}
				else {
					halt(400, "You must provide valid user details for the member to add");
					return null;
				}
			}
			else {
				halt(400, "You do not have access to the cards wall");
				return null;
			}
		}
		else {
			halt(400, "Cannot add a member to the card, the card was not found");
			return null;
		}
	}

	public String deleteMember(Request req, Response res, User currentUser) throws SQLException {
		int cardId = Integer.parseInt(req.params(":id"));
		Card card = getCardWithId(cardId);

		if(card != null) {
			Wall cardParent = WallHandler.getWallWithId(card.getWallId(), currentUser, connection);
			if(cardParent != null && WallHandler.checkWallMembership(cardParent.getId(), currentUser.getId(), connection) > -1) {
				int memberId = Integer.parseInt(req.params(":memberId"));	
					
				// Can only remove yourself from a card
				if(memberId == currentUser.getId()) {
					String sql = "DELETE FROM card_members WHERE cardId=? AND userId=?";
					PreparedStatement preparedStatement = connection.prepareStatement(sql);
					preparedStatement.setInt(1, cardId);
					preparedStatement.setInt(2, memberId);
					preparedStatement.executeUpdate();
					return "Member delete success";
				}
				else {
					halt(400, "You do not have permission to remove that member");
					return "Failure";
				}
				
			}
			else {
				halt(400, "You do not have access to the cards wall");
				return "Failure";
			}
		}
		else {
			halt(400, "Cannot remove card member, no card was found");
			return "Failure";
		}
		
	}

	public Attachment addAttachment(Request req, Response res, User currentUser) throws SQLException {
		int cardId = Integer.parseInt(req.params(":id"));
		Card card = getCardWithId(cardId);

		if(card != null) {
			Wall cardParent = WallHandler.getWallWithId(card.getWallId(), currentUser, connection);
			if(cardParent != null && WallHandler.checkWallMembership(cardParent.getId(), currentUser.getId(), connection) > -1) {
				String body = req.body();
				HashMap<String, String> attachment = new Gson().fromJson(body, HashMap.class);

				if(attachment != null && attachment.keySet().size() > 0) {

					String description = null;

					if(attachment.containsKey("description")) {
						description = attachment.get("description");
					}

					if(attachment.containsKey("data")) {
						String rawData = attachment.get("data"); // Base 64
						String fileName = null;

						if(attachment.containsKey("fileName")) {
							fileName = attachment.get("fileName");
						}
						else {
							fileName = new Date().getTime() + "";
						}
						
						// Convert string to image and write to disk.
						// Then store URL & filename in DB
						writeImageToDisk(rawData, fileName);
						String url = "http://192.168.0.100:4567/" + fileName;
						updateCardCover(cardId, url); // Set cover URL to new attachment
						resetCardAttachmentsCover(cardId); // Clear 'cover' variable for all other attachments
						// The cover variable will be set in this function
						NotificationHandler.addNotification(getCardMembers(cardId), 
						currentUser.getName() + " added a new attachment to the card '"  + card.getTitle() + "'", 
						fileName, connection); 

						return storeAttachmentDetails(cardId, url, fileName, description); 
					}
					else if(attachment.containsKey("url")) {
						// Add URL, no file data
						String url = attachment.get("url");
						updateCardCover(cardId, url);
						resetCardAttachmentsCover(cardId);

						NotificationHandler.addNotification(getCardMembers(cardId), 
						currentUser.getName() + " added a new attachment to the card '"  + card.getTitle() + "'", 
						null, connection); 

						return storeAttachmentDetails(cardId, url, null, description);
					}
					else {
						halt(400, "You must provide valid attachment details to add");
						return null;
					}
				}
				else {
					halt(400, "You must provide valid attachment details to add");
					return null;
				}
			}
			else {
				halt(400, "Cannot add an attachment, you do not have access to the cards wall");
				return null;
			}
		}
		else {
			halt(400, "Cannot add an attachment, the card was not found");
			return null;
		}
	}

	public ArrayList<Member> getMembers(Request req, Response res, User currentUser) throws SQLException {
		int cardId = Integer.parseInt(req.params(":id"));
		Card card = getCardWithId(cardId);
		if(card != null) {
			Wall cardParent = WallHandler.getWallWithId(card.getWallId(), currentUser, connection);
			if(cardParent != null && 
				WallHandler.checkWallMembership(cardParent.getId(), currentUser.getId(), connection) > -1) {
				return getCardMembers(cardId);
			}
			else {
				halt(400, "Could not get card members, you do not have access to this cards wall");
				return null;
			}
		}
		else {
			halt(400, "Could not get card members, no card was found");
			return null;
		}	
	}

	public ArrayList<Comment> getComments(Request req, Response res, User currentUser) throws SQLException {
		int cardId = Integer.parseInt(req.params(":id"));
		Card card = getCardWithId(cardId);
		if(card != null) {
			Wall cardParent = WallHandler.getWallWithId(card.getWallId(), currentUser, connection);
			if(cardParent != null && 
				WallHandler.checkWallMembership(cardParent.getId(), currentUser.getId(), connection) > -1) {
				ArrayList<Comment> result = new ArrayList<Comment>();

				String sql = "SELECT comments.id, comments.text, comments.created, users.id AS 'userId', users.name, users.email FROM (SELECT * FROM card_comments WHERE cardId = ?) comments LEFT JOIN (SELECT id, name, email FROM users) users ON comments.userId = users.id";
				PreparedStatement preparedStatement = connection.prepareStatement(sql);
				preparedStatement.setInt(1, cardId);

				ResultSet rs = preparedStatement.executeQuery();
				while(rs.next()) {
					int id = rs.getInt(1);
					String text = rs.getString(2);
					Timestamp created = rs.getTimestamp(3);
					int userId = rs.getInt(4);
					String userName = rs.getString(5);
					String userEmail = rs.getString(6);
					result.add(new Comment(id, text, created,
						new User(userId, userName, userEmail)));
				}
				return result;
			}
			else {
				halt(400, "Could not get card comments, you do not have access to this cards wall");
				return null;
			}
		}
		else {
			halt(400, "Could not get card comments, no card was found");
			return null;
		}
	}

	public ArrayList<Attachment> getAttachments(Request req, Response res, User currentUser) throws SQLException {
		int cardId = Integer.parseInt(req.params(":id"));
		Card card = getCardWithId(cardId);
		if(card != null) {
			Wall cardParent = WallHandler.getWallWithId(card.getWallId(), currentUser, connection);
			if(cardParent != null && 
				WallHandler.checkWallMembership(cardParent.getId(), currentUser.getId(), connection) > -1) {
				ArrayList<Attachment> result = new ArrayList<Attachment>();

				String sql = "SELECT * FROM card_attachments WHERE cardId = ?";
				PreparedStatement preparedStatement = connection.prepareStatement(sql);
				preparedStatement.setInt(1, cardId);

				ResultSet rs = preparedStatement.executeQuery();
				while(rs.next()) {
					int id = rs.getInt(1);
					String url = rs.getString(3);
					String fileName = rs.getString(4);
					Timestamp created = rs.getTimestamp(5);
					int cover = rs.getInt(6);
					String description = rs.getString(7);

					result.add(new Attachment(id, fileName, url, created, cover, description));
				}

				return result;
			}
			else {
				halt(400, "Could not get card attachments, you do not have access to this cards wall");
				return null;
			}
		}
		else {
			halt(400, "Could not get card attachments, no card was found");
			return null;
		}
	}

	/*
		Set the cards cover URL
	*/
	private void updateCardCover(int cardId, String coverUrl) throws SQLException {
		String sql = "UPDATE cards SET cover=? WHERE id=?";
		PreparedStatement preparedStatement = connection.prepareStatement(sql);
		preparedStatement.setString(1, coverUrl);
		preparedStatement.setInt(2, cardId);
		preparedStatement.executeUpdate();
	}

	/*
		
	*/
	private void resetCardAttachmentsCover(int cardId) throws SQLException {
		String sql = "UPDATE card_attachments SET cover=0 WHERE cardId=?";
		PreparedStatement preparedStatement = connection.prepareStatement(sql);
		preparedStatement.setInt(1, cardId);
		preparedStatement.executeUpdate();
	}

	/*
		Decode the Base64 image string into bytes and
		store on the disk.

		TODO: Remove hard coding of file location.
				Improve error handling
				Return a result code. (boolean)
	*/
	private void writeImageToDisk(String imageData, String fileName) {
		String location = "/Users/Stephan/Documents/4th Year Project/coolwall-spark-backend/resources/" +
							fileName;
		OutputStream stream = null;
		try {
			Base64.Decoder decoder = Base64.getDecoder();
			byte [] imageBytes = decoder.decode(imageData);
			stream = new FileOutputStream(location);
			stream.write(imageBytes);
			stream.close();
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}

	private Attachment storeAttachmentDetails(int cardId, String url, String fileName, String description) throws SQLException {
		String sql = "INSERT INTO card_attachments (cardId, url, fileName, cover, description) VALUES (?, ?, ?, 1, ?)";
		PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
		preparedStatement.setInt(1, cardId);
		preparedStatement.setString(2, url);
		preparedStatement.setString(3, fileName);
		preparedStatement.setString(4, description);
		preparedStatement.executeUpdate();


		ResultSet generatedKeys = preparedStatement.getGeneratedKeys();
		int id = -1;
		Timestamp created = new Timestamp(new java.util.Date().getTime());
		if(generatedKeys.next()) {
			id = generatedKeys.getInt(1);
		}

		return new Attachment(id, fileName, url, created, 1, description);
	}

	private Card getCardWithId(int cardId) throws SQLException {
		Card result = null;

		String sql = "SELECT * FROM cards WHERE id=?";
		PreparedStatement preparedStatement = connection.prepareStatement(sql);
		preparedStatement.setInt(1, cardId);

		ResultSet rs = preparedStatement.executeQuery();
		if(rs != null && rs.next()) {
			int ordinal = rs.getInt(3);
			String title = rs.getString(4);
			int wallId = rs.getInt(8);
			result = new Card(cardId, ordinal, title, wallId);
		}

		return result;
	}

	private ArrayList<Member> getCardMembers(int cardId) throws SQLException {
		ArrayList<Member> result = new ArrayList<Member>();

		String sql = "SELECT users.id, users.name, users.email, members.since FROM (SELECT * FROM card_members WHERE cardId=?) members LEFT JOIN (SELECT id, name, email FROM users) users ON members.userId = users.id";
		PreparedStatement preparedStatement = connection.prepareStatement(sql);
		preparedStatement.setInt(1, cardId);

		ResultSet rs = preparedStatement.executeQuery();
		while(rs.next()) {
			int id = rs.getInt(1);
			String name = rs.getString(2);
			String email = rs.getString(3);
			Timestamp since = rs.getTimestamp(4);
			result.add(new Member(new User(id, name, email), since));
		}
		return result;
	}

	/*
		Delete the attachment specified by the ID and return
		the new cover attachment for the card if available, 
		otherwise return null.
	*/
	public Attachment deleteAttachment(Request req, Response res, User currentUser) throws SQLException {
		Attachment replacement = null;
		int attachmentId = Integer.parseInt(req.params(":id"));
		String sql = "SELECT * FROM card_attachments WHERE id=?";
		PreparedStatement preparedStatement = connection.prepareStatement(sql, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
		preparedStatement.setInt(1, attachmentId);
		ResultSet rs = preparedStatement.executeQuery();

		while(rs.next()) {
			int id = rs.getInt(1);
			int cardId = rs.getInt(2);
			String fileName = rs.getString(4);
			deleteAttachmentFile(fileName);
			int cover = rs.getInt(6);
			rs.deleteRow();

			if(cover == 1) {
				// Attachment is current card cover
				replacement = deleteAttachmentUpdateCover(cardId);
				if(replacement != null) {
					updateCardCover(cardId, replacement.getUrl());
				}
				else {
					updateCardCover(cardId, null);
				}
			}
		}

		return replacement;
	}

	public String deleteComment(Request req, Response res, User currentUser) throws SQLException {
		int commentId = Integer.parseInt(req.params(":id"));
		String sql = "SELECT * FROM card_comments WHERE id=?";
		PreparedStatement preparedStatement = connection.prepareStatement(sql, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
		preparedStatement.setInt(1, commentId);
		ResultSet rs = preparedStatement.executeQuery();

		if(rs != null && rs.next()) {
			int userId = rs.getInt(3);
			if(userId == currentUser.getId()) {
				rs.deleteRow();
				return "Comment delete success";
			}
			else {
				halt(400, "You cannot delete that comment");
				return "Failure";
			}
		}
		else {
			halt(400, "No comment found to delete");
			return "Failure";
		}
	}

	/*
		After deleting the attachment which was the cards cover pic, 
		select a new attachment if possible and set it as the cover.
	*/
	private Attachment deleteAttachmentUpdateCover(int cardId) throws SQLException {
		String sql = "SELECT * FROM card_attachments WHERE cardId=? ORDER BY created DESC LIMIT 1";
		PreparedStatement preparedStatement = connection.prepareStatement(sql, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
		preparedStatement.setInt(1, cardId);
		ResultSet rs = preparedStatement.executeQuery();
		if(rs != null && rs.first()) {
			rs.updateInt(6, 1);
			int attachmentId = rs.getInt(1);
			String url = rs.getString(3);
			String fileName = rs.getString(4);
			Timestamp created = rs.getTimestamp(5);
			String description = rs.getString(7);
			return new Attachment(attachmentId, fileName, url, created, 1, description);
		}

		// None found return null
		return null;
	}

	private static void deleteAttachmentFile(String fileName) {
		if(fileName != null) {
			String fileLocation = "/Users/Stephan/Documents/4th Year Project/coolwall-spark-backend/resources/" +
							fileName;
			
			File file = new File(fileLocation);
			file.delete();	
		}
	}

	/*
		Delete all content associated with a card.
		This method is public and static as it is used
		by the Lane handler when deleting a lane.
	*/
	public static void deleteCardContent(int cardId, Connection conn) throws SQLException {
		deleteCardMembers(cardId, conn);
		deleteCardComments(cardId, conn);
		deleteCardAttachments(cardId, conn);
	}

	private static void deleteCardMembers(int cardId, Connection conn) throws SQLException {
		String sql = "DELETE FROM card_members WHERE cardId=?";
		PreparedStatement preparedStatement = conn.prepareStatement(sql);
		preparedStatement.setInt(1, cardId);
		preparedStatement.executeUpdate();
	}

	private static void deleteCardComments(int cardId, Connection conn) throws SQLException {
		String sql = "DELETE FROM card_comments WHERE cardId=?";
		PreparedStatement preparedStatement = conn.prepareStatement(sql);
		preparedStatement.setInt(1, cardId);
		preparedStatement.executeUpdate();
	}

	private static void deleteCardAttachments(int cardId, Connection conn) throws SQLException {
		String sql = "SELECT * FROM card_attachments WHERE cardId=?";
		PreparedStatement preparedStatement = conn.prepareStatement(sql, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
		preparedStatement.setInt(1, cardId);
		ResultSet rs = preparedStatement.executeQuery();

		while(rs.next()) {
			int id = rs.getInt(1);
			String fileName = rs.getString(4);
			deleteAttachmentFile(fileName);
			rs.deleteRow();
		}
	}

}