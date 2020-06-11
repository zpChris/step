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

@WebServlet("/auth")
public class AuthServlet extends HttpServlet {

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType("application/json");

    UserService userService = UserServiceFactory.getUserService();
    UserAuth userAuth;
    if (userService.isUserLoggedIn()) {
      String userEmail = userService.getCurrentUser().getEmail();
      String urlToRedirectToAfterUserLogsOut = "/";
      String logoutUrl = 
        userService.createLogoutURL(urlToRedirectToAfterUserLogsOut);
      String id = userService.getCurrentUser().getUserId();
      String username = getUsername(userEmail, id);

      // Create UserAuth object to represent logged-in user.
      userAuth = new UserAuth(logoutUrl, userEmail, username);
    } else {
      String urlToRedirectToAfterUserLogsIn = "/";
      String loginUrl = userService.createLoginURL(urlToRedirectToAfterUserLogsIn);

      // Create UserAuth object to represent logged-out user.
      userAuth = new UserAuth(loginUrl);
    }

    String json = convertToJson(userAuth);
    response.getWriter().println(json);
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    UserService userService = UserServiceFactory.getUserService();

    // TODO: What should I do if the user is not logged in, but still resets a username?
    if (!userService.isUserLoggedIn()) {
      response.sendRedirect("/");
      return;
    }

    // Add the username to the database
    String username = getParameter(request, "text-input", "");
    String id = userService.getCurrentUser().getUserId();
    putUsername(username, id);

    response.sendRedirect("/");
  }

  /**
   * Add the username to the database, or replace the current username.
   */
  public static void putUsername(String username, String id) {
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Entity entity = new Entity("UserInfo", id);
    entity.setProperty("id", id);
    entity.setProperty("username", username);
    // The put() function automatically inserts new data or updates existing 
    // data based on ID
    datastore.put(entity);
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
   * Returns the username of the user with id, or null if the user has not
   * set a username.
   */
  public static String getUsername(String email, String id) {
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Query query =
        new Query("UserInfo")
            .setFilter(new Query.FilterPredicate("id", Query.FilterOperator.EQUAL, id));
    PreparedQuery results = datastore.prepare(query);
    Entity entity = results.asSingleEntity();

    // If user does not yet have username, create and add the username to datastore.
    if (entity == null) {
      String username = createUsername(email);
      putUsername(username, id);
      return username;
    }
    String username = (String) entity.getProperty("username");
    return username;
  }

  /**
   * Create a username based on an email, by taking the portion before '@'.
   * If email does not contain an "@", return the email.
   */
  public static String createUsername(String email) {
    if (email.indexOf('@') == -1) {
      return email;
    }
    return email.substring(0, email.indexOf('@'));
  }

  /**
  * Converts a UserAuth object into a JSON string using the Gson library.
  */
  private String convertToJson(UserAuth userAuth) {
    Gson gson = new Gson();
    String json = gson.toJson(userAuth);
    return json;
  }

  /**
   * Inner class that holds relevant login/logout and user information.
   */
  class UserAuth {
    // Fields that hold relevant login data.
    private boolean loggedIn;
    private String loginUrl;
    private String logoutUrl;
    private String email;
    private String username;

    // Constructor to create UserAuth object with no user logged in.
    // Empty strings represent no value (null is avoided to prevent errors).
    public UserAuth(String loginUrl) {
      this(false, loginUrl, "", "", "");
    }

    // Constructor to create UserAuth object with user logged in.
    public UserAuth(String logoutUrl, String email, String username) {
      this(true, "", logoutUrl, email, username);
    }

    // Full constructor to assign values to all fields.
    public UserAuth(boolean loggedIn, String loginUrl, String logoutUrl, 
      String email, String username) {
        this.loggedIn = loggedIn;
        this.loginUrl = loginUrl;
        this.logoutUrl = logoutUrl;
        this.email = email;
        this.username = username;
    }
  }
}
