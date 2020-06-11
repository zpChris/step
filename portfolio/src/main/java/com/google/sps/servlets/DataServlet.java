// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.sps.servlets;

import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.SortDirection;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;

/** Servlet that handles comment data. */
@WebServlet("/data")
public class DataServlet extends HttpServlet {

  // String identifiers for comment attributes.
  final String COMMENT_TEXT = "text";
  final String COMMENT_DATE = "date";
  public final String COMMENT_EMAIL = "userEmail";
  public final String COMMENT_ID = "id";
  public final String COMMENT_USERNAME = "username";
  final String COMMENT_NAME = "Comment";

  // Default for max number of comments to show.
  final int COMMENT_MAX_DEFAULT = 5;

  // Maximum number of comments that are shown in UI.
  private int commentMax;

  // UserService object to identify user attributes
  private UserService userService;

  @Override
  public void init() {
    this.commentMax = COMMENT_MAX_DEFAULT;
    this.userService = UserServiceFactory.getUserService();
  }

  /**
  * Converts a List of Comments into a JSON string using the Gson library.
  */
  private String convertToJson(List<Comment> messages) {
    Gson gson = new Gson();
    String json = gson.toJson(messages);
    return json;
  }

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // Get comments in datastore, by most recent order at the top.
    Query query = new Query(COMMENT_NAME)
      .addSort(COMMENT_DATE, SortDirection.DESCENDING);
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    PreparedQuery results = datastore.prepare(query);

    // Extract limit on number of comments from query string (default is 5).
    int commentMax = 
      Integer.parseInt(getParameter(request, "comment-max", "" + this.commentMax));

    // Iterate over all entities, get comment.
    List<Comment> comments = getComments(results, commentMax);

    // Return comments in JSON format.
    response.setContentType("application/json;");
    String json = convertToJson(comments);
    response.getWriter().println(json);
  }

  /**
   * Get all Comment entities from the provided PreparedQuery. 
   * No more comments than the provided limit allows will be returned.
   */
  public List<Comment> getComments(PreparedQuery results, int commentMax) {
    List<Comment> comments = new ArrayList<Comment>();

    // Populate comment list until limit is reached or no comments remain.
    int count = 0;
    for (Entity entity : results.asIterable()) {
      // Build the comment.
      String text = (String) entity.getProperty(COMMENT_TEXT);
      Date date = (Date) entity.getProperty(COMMENT_DATE);
      String email = (String) entity.getProperty(COMMENT_EMAIL);
      String userId = (String) entity.getProperty(COMMENT_ID);

      // The username is not fetched from the entity as it could be updated.
      String username = AuthServlet.getUsername(email, userId);
      User user = new User(email, userId, username);
      long id = entity.getKey().getId();
      Comment comment = new Comment(id, text, date, user);
      comments.add(comment);

      // Update count, and stop adding comments if comment max limit is reached.
      count++;
      if (commentMax <= count) {
        return comments;
      }
    }

    return comments;
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType("text/html;");
    // Separate POST logic for comments and max comments shown.
    if (request.getParameter("text-input") != null) {
      postComment(request, response);
    } else if (request.getParameter("comment-max") != null) {
      setCommentMax(request, response);
    }
  }

  /**
   * Returns the email address of the user, if the user is logged in.
   * If no user is logged in, return an empty string (however, a user 
   * only has post access when logged in).
   * 
   * TODO: A user must be signed in to post a comment. However, what if there 
   * is a bug? How should this be handled?
   */
  public String getUserEmail() {
    if (userService.isUserLoggedIn()) {
      return userService.getCurrentUser().getEmail();
    }
    return "zpchris@wharton.upenn.edu";
  }

    /**
   * Returns the id of the user, if the user is logged in.
   * If no user is logged in, return an empty string (however, a user 
   * only has post access when logged in).
   * 
   * TODO: A user must be signed in to post a comment. However, what if there 
   * is a bug? How should this be handled? (Duplicate from getUserEmail().)
   */
  public String getUserId() {
    if (userService.isUserLoggedIn()) {
      return userService.getCurrentUser().getUserId();
    }
    return "";
  }

  /**
   * Handle logic of posting comment and redirecting user to original page.
   */
  public void postComment(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String text = getParameter(request, "text-input", "");
    Date date = new Date();
    String userEmail = getUserEmail();
    String userId = getUserId();
    String username = AuthServlet.getUsername(userEmail, userId);
    User user = new User(userEmail, userId, username);

    // Create a comment entity from the Comment object.
    Comment commentObject = new Comment(text, date, user);
    Entity commentEntity = commentObject.createCommentEntity();

    // Add the comment entity to the DatastoreService.
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    datastore.put(commentEntity);

    // Redirect back to the HTML page.
    response.sendRedirect("/");
  }

  /**
   * Set the max number of comments that can be shown (value between 1 and 50).
   */
  public void setCommentMax(HttpServletRequest request, HttpServletResponse response) throws IOException {
    this.commentMax = 
      Integer.parseInt(getParameter(request, "comment-max", "" + this.commentMax));

    // Redirect back to the HTML page.
    response.sendRedirect("/");
  }

  /**
   * @return the request parameter, or the default value if the parameter
   *         was not specified by the client
   */
  private String getParameter(HttpServletRequest request, String name, String defaultValue) {
    String value = request.getParameter(name);
    if (value == null) {
      return defaultValue;
    }
    return value;
  }

  /**
   * Inner class for the Comments posted by users on the portfolio site.
   */
  class Comment {
    // The ID is not required for creating a Comment Entity.
    // The default ID of "0" means the Comment object is used to make an Entity.
    // Otherwise, the ID is an Entity property necessary to delete the Comment. 
    private long id;

    // The fields that hold the relevant comment data.
    private String text;
    private Date date;
    private User user;

    // This constructor can create a Comment Entity (no ID required).
    public Comment(String text, Date date, User user) {
      this(0, text, date, user);
    }

    // This constructor can accept a Comment Entity object (includes ID).
    public Comment(long id, String text, Date date, User user) {
      this.id = id;
      this.text = text;
      this.date = date;
      this.user = user;
    }

    public Entity createCommentEntity() {
      // Create a comment entity.
      Entity commentEntity = new Entity(COMMENT_NAME);
      commentEntity.setProperty(COMMENT_TEXT, this.text);
      commentEntity.setProperty(COMMENT_DATE, this.date);
      commentEntity.setProperty(COMMENT_EMAIL, this.user.emailAddress);
      commentEntity.setProperty(COMMENT_ID, this.user.id);
      commentEntity.setProperty(COMMENT_USERNAME, this.user.username);
      return commentEntity;
    }
  }

  /**
   * Inner class for the User that is currently logged in.
   */
  class User {
    // The fields that hold relevant user data.
    // The id is used to keep the username up-to-date.
    private String emailAddress;
    private String id;
    private String username;

    public User(String emailAddress, String id, String username) {
      this.emailAddress = emailAddress;
      this.id = id;
      this.username = username;
    }
  }

}
