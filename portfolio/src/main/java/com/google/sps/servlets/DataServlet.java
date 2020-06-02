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
    Query query = new Query("Comment").addSort("date", SortDirection.DESCENDING);
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    PreparedQuery results = datastore.prepare(query);

    // Extract limit on number of comments from query string.
    Integer limit = Integer.parseInt(request.getParameter("limit"));

    // Iterate over all entities, get comment.
    List<Comment> comments = new ArrayList<>();
    Integer count = 0;
    for (Entity entity : results.asIterable()) {
      // Build the comment.
      String text = (String) entity.getProperty("text");
      Date date = (Date) entity.getProperty("date");
      Comment comment = new Comment(text, date);
      comments.add(comment);

      // Update count, and stop adding comments if limit is reached.
      count++;
      if (limit == count) {
        break;
      }
    }

    // Return comments in JSON format.
    response.setContentType("application/json;");
    String json = convertToJson(comments);
    response.getWriter().println(json);
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType("text/html;");
    String comment = getParameter(request, "text-input", "");
    Date date = new Date();

    // Create a comment entity.
    Entity commentEntity = new Entity("Comment");
    commentEntity.setProperty("text", comment);
    commentEntity.setProperty("date", date);

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
   * Inner class for Comments.
   */
  class Comment {
    private String text;
    private Date date;

    public Comment(String text, Date date) {
      this.text = text;
      this.date = date;
    }
  }

}
