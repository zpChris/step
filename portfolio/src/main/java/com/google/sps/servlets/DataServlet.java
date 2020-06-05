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
    Query query = new Query("Comment").addSort(COMMENT_DATE, SortDirection.DESCENDING);
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    PreparedQuery results = datastore.prepare(query);

    // Extract limit on number of comments from query string (default is 5).
    int limit = (request.getParameter("limit") == null) ?
      5 : Integer.parseInt(request.getParameter("limit"));

    // Iterate over all entities, get comment.
    List<Comment> comments = getComments(results, limit);

    // Return comments in JSON format.
    response.setContentType("application/json;");
    String json = convertToJson(comments);
    response.getWriter().println(json);
  }

  /**
   * Get the comments from the datastore based on query in GET method.
   */
  public List<Comment> getComments(PreparedQuery results, int limit) {
    List<Comment> comments = new ArrayList<Comment>();

    // Populate comment list until limit is reached or no comments remain.
    int count = 0;
    for (Entity entity : results.asIterable()) {
      // Build the comment.
      String text = (String) entity.getProperty(COMMENT_TEXT);
      Date date = (Date) entity.getProperty(COMMENT_DATE);
      Comment comment = new Comment(text, date);
      comments.add(comment);

      // Update count, and stop adding comments if limit is reached.
      count++;
      if (limit <= count) {
        break;
      }
    }

    return comments;
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType("text/html;");
    String text = getParameter(request, "text-input", "");
    Date date = new Date();

    // Create a comment entity from the Comment object.
    Comment commentObject = new Comment(text, date);
    Entity commentEntity = commentObject.createCommentEntity();

    // Add the comment entity to the DatastoreService.
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    datastore.put(commentEntity);

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
    // The fields that hold the relevant comment data.
    private String text;
    private Date date;

    public Comment(String text, Date date) {
      this.text = text;
      this.date = date;
    }

    public Entity createCommentEntity() {
      // Create a comment entity.
      Entity commentEntity = new Entity("Comment");
      commentEntity.setProperty(COMMENT_TEXT, this.text);
      commentEntity.setProperty(COMMENT_DATE, this.date);
      return commentEntity;
    }
  }

}
