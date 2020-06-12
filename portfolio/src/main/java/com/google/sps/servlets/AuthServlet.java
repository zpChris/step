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

    final UserService userService = UserServiceFactory.getUserService();
    final UserAuth userAuth;
    if (userService.isUserLoggedIn()) {
      final String userEmail = userService.getCurrentUser().getEmail();
      final String urlToRedirectToAfterUserLogsOut = "/";
      final String logoutUrl = userService.createLogoutURL(urlToRedirectToAfterUserLogsOut);

      // Create UserAuth object to represent logged-in user.
      userAuth = new UserAuth(logoutUrl, userEmail);
    } else {
      final String urlToRedirectToAfterUserLogsIn = "/";
      final String loginUrl = userService.createLoginURL(urlToRedirectToAfterUserLogsIn);

      // Create UserAuth object to represent logged-out user.
      userAuth = new UserAuth(loginUrl);
    }

    String json = convertToJson(userAuth);
    response.getWriter().println(json);
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

    // Constructor to create UserAuth object with no user logged in.
    // Empty strings represent no value (null is avoided).
    private UserAuth(String loginUrl) {
      this(false, loginUrl, "", "");
    }

    // Constructor to create UserAuth object with user logged in.
    private UserAuth(String logoutUrl, String email) {
      this(true, "", logoutUrl, email);
    }

    // Full constructor to assign values to all fields.
    private UserAuth(boolean loggedIn, String loginUrl, String logoutUrl, 
      String email) {
        this.loggedIn = loggedIn;
        this.loginUrl = loginUrl;
        this.logoutUrl = logoutUrl;
        this.email = email;
    }
  }
}
