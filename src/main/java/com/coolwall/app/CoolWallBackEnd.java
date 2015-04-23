/*
	CoolWall Spark Back end main file.

    Author: Stephan McLean
    Date: 15th March 2015.
*/

package com.coolwall.app;
import com.coolwall.app.models.User;

import static spark.Spark.*; // Spark Routing methods
import spark.*; // Spark Classes

import java.sql.*;
import java.util.ArrayList;

public class CoolWallBackEnd {
    /* Classes to delegate work to */
    private AuthenticationHandler authHandler;
    private UserHandler userHandler;
    private WallHandler wallHandler;
    private LaneHandler laneHandler;
    private CardHandler cardHandler;
    private NotificationHandler notificationHandler;

    private User currentUser;

    /* DB Related variables */
    private Connection connection;
    private static String dbUrl = "jdbc:mysql://localhost:8889/coolwall";

    public CoolWallBackEnd() {
        try {
            connect();
            setUpDelegates();
            setUpRoutes();
        }
        catch(SQLException e) {
            e.printStackTrace();
        }
    }

    private void setUpDelegates() {
        authHandler = new AuthenticationHandler(connection);
        userHandler = new UserHandler(connection);
        wallHandler = new WallHandler(connection);
        laneHandler = new LaneHandler(connection);
        cardHandler = new CardHandler(connection);
        notificationHandler = new NotificationHandler(connection);
    }

    private void connect() throws SQLException {
        String user = "root";
        String pass = "root";
        connection = DriverManager.getConnection(dbUrl, user, pass);
    }

    private void setUpRoutes() throws SQLException {

        externalStaticFileLocation("/Users/Stephan/Documents/4th Year Project/coolwall-spark-backend/resources");
        //staticFileLocation("/public"); 
        
        options("/*", (request,response) -> {
            response.header("Access-Control-Request-Method", "*");
            response.header("Access-Control-Allow-Headers", "Content-Type");
            response.header("Access-Control-Allow-Methods", "GET, PUT, POST, OPTIONS, DELETE");
            return "OK";
        });

        before((request, response) -> {
            /* Solve CORS problem */
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Allow-Headers", "Authorization");
            
            if(!request.requestMethod().equals("OPTIONS")) {
                
                /* This block executes before every route */
                if(!request.pathInfo().equals("/login") && 
                    !request.pathInfo().equals("/register") && !request.pathInfo().equals("/currentUser")) {
                    currentUser = userHandler.getCurrentUser(request);
                    if(currentUser == null) {
                        //halt(401);
                    }
                }
            }
        });

        get("/currentUser", (req, res) -> userHandler.getCurrentUser(req), new JsonTransformer()); // TESTED
        post("/searchUsers", (req, res) -> userHandler.searchUsers(req, res, currentUser), new JsonTransformer()); // TESTED
        
        get("/walls", (req, res) -> wallHandler.getWalls(req, res, currentUser), new JsonTransformer()); 
        get("/wall/:id", (req, res) -> wallHandler.getWall(req, res, currentUser), new JsonTransformer()); 
        post("/wall/:id/addLane", (req, res) -> wallHandler.addLane(req, res, currentUser), new JsonTransformer()); 
        post("/wall/:id/addMember", (req, res) -> wallHandler.addMember(req, res, currentUser), new JsonTransformer());
        delete("/wall/:id/member/:memberId", (req, res) -> wallHandler.deleteMember(req, res, currentUser), new JsonTransformer()); 
        get("/wall/:id/members", (req, res) -> wallHandler.getMembers(req, res, currentUser), new JsonTransformer()); 
        post("/wall/:id/searchMembers", (req, res) -> wallHandler.searchMembers(req, res, currentUser), new JsonTransformer()); 
        delete("/wall/:id", (req, res) -> wallHandler.deleteWall(req, res, currentUser), new JsonTransformer()); 
        put("/wall/:id/update", (req, res) -> wallHandler.updateWall(req, res, currentUser), new JsonTransformer());
        
        post("/lane/:id/addCard", (req, res) -> laneHandler.addCard(req, res, currentUser), new JsonTransformer());
        delete("/lane/:id", (req, res) -> laneHandler.deleteLane(req, res, currentUser), new JsonTransformer());
        put("/lane/:id/update", (req, res) -> laneHandler.updateLane(req, res, currentUser), new JsonTransformer());
        
        put("/card/:id/update", (req, res) -> cardHandler.updateCard(req, res, currentUser), new JsonTransformer());
        delete("/card/:id", (req, res) -> cardHandler.deleteCard(req, res, currentUser), new JsonTransformer());
        post("/card/:id/addComment", (req, res) -> cardHandler.addComment(req, res, currentUser), new JsonTransformer());
        post("/card/:id/addMember", (req, res) -> cardHandler.addMember(req, res, currentUser), new JsonTransformer());
        delete("/card/:id/member/:memberId", (req, res) -> cardHandler.deleteMember(req, res, currentUser), new JsonTransformer());
        post("/card/:id/addAttachment", (req, res) -> cardHandler.addAttachment(req, res, currentUser), new JsonTransformer());
        get("/card/:id/members", (req, res) -> cardHandler.getMembers(req, res, currentUser), new JsonTransformer());
        get("/card/:id/comments", (req, res) -> cardHandler.getComments(req, res, currentUser), new JsonTransformer());
        get("/card/:id/attachments", (req, res) -> cardHandler.getAttachments(req, res, currentUser), new JsonTransformer());

        /* NEED ERROR HANDLING */
        delete("/attachment/:id", (req, res) -> cardHandler.deleteAttachment(req, res, currentUser), new JsonTransformer());
        delete("/comment/:id", (req, res) -> cardHandler.deleteComment(req, res, currentUser), new JsonTransformer());

        get("/notifications", (req, res) -> notificationHandler.getNotifications(req, res, currentUser), new JsonTransformer());
        put("/notifications/markRead", (req, res) -> notificationHandler.markAllRead(req, res, currentUser), new JsonTransformer());

        
        post("/newWall", (req, res) -> wallHandler.newWall(req, res, currentUser), new JsonTransformer());
        post("/register", (req, res) -> userHandler.register(req, res), new JsonTransformer()); // TESTED
        post("/login", (req, res) -> authHandler.login(req, res), new JsonTransformer()); // TESTED
        //post("/logout", (req, res) -> authHandler.logout(req, res, currentUser), new JsonTransformer());
    }

    public Connection getConnection() {
        return this.connection;
    }

    public static void main(String [] args) {
        new CoolWallBackEnd();
    }
}
