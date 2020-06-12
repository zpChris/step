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

import com.google.appengine.api.blobstore.BlobInfo;
import com.google.appengine.api.blobstore.BlobInfoFactory;
import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.images.ImagesService;
import com.google.appengine.api.images.ImagesServiceFactory;
import com.google.appengine.api.images.ServingUrlOptions;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
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
  public static final String COMMENT_TEXT = "text";
  public static final String COMMENT_DATE = "date";
  public static final String COMMENT_NAME = "Comment";
  public static final String COMMENT_MAX = "comment-max";
  public static final String COMMENT_IMAGE = "imageUrl";

  // String identifier to indicate that a non-image was uploaded.
  public static final String NOT_AN_IMAGE_EXCEPTION = "invalid_image_input";

  // Default for max number of comments to show.
  final int COMMENT_MAX_DEFAULT = 5;

  // Maximum number of comments that are shown in UI.
  private int commentMax;

  @Override
  public void init() {
    this.commentMax = COMMENT_MAX_DEFAULT;
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
    List<Comment> comments = new ArrayList<>();

    // Populate comment list until limit is reached or no comments remain.
    int count = 0;
    for (Entity entity : results.asIterable()) {
      // Build the comment.
      String text = (String) entity.getProperty(COMMENT_TEXT);
      Date date = (Date) entity.getProperty(COMMENT_DATE);
      String imageUrl = (String) entity.getProperty(COMMENT_IMAGE);
      long id = entity.getKey().getId();
      Comment comment = new Comment(id, text, date, imageUrl);
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
    } else if (request.getParameter(COMMENT_MAX) != null) {
      setCommentMax(request, response);
    }
  }

  /**
   * Handle logic of posting comment and redirecting user to original page.
   */
  public void postComment(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String text = getParameter(request, "text-input", "");
    Date date = new Date();

    // Get the URL of the image that the user uploaded to Blobstore.
    // If no image was uploaded, this will simply be undefined and will be 
    // handled on the frontend. If a non-image was uploaded, redirect 
    // back to HTML page.
    String imageUrl = getUploadedFileUrl(request, "image");
    if (imageUrl.equals(NOT_AN_IMAGE_EXCEPTION)) {
      response.sendRedirect("/");
      return;
    }

    // Create a comment entity from the Comment object.
    Comment commentObject = new Comment(text, date, imageUrl);
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
      Integer.parseInt(getParameter(request, COMMENT_MAX, "" + this.commentMax));

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
   * Returns a URL that points to the uploaded file, or null if the user 
   * didn't upload a file.
   */
  private String getUploadedFileUrl(HttpServletRequest request, String formInputElementName) {
    BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
    Map<String, List<BlobKey>> blobs = blobstoreService.getUploads(request);
    List<BlobKey> blobKeys = blobs.get("image");

    // User submitted form without selecting a file, so we can't get a URL. (dev server)
    if (blobKeys == null || blobKeys.isEmpty()) {
      return null;
    }

    // Our form only contains a single file input, so get the first index.
    BlobKey blobKey = blobKeys.get(0);

    // User submitted form without selecting a file, so we can't get a URL. (live server)
    BlobInfo blobInfo = new BlobInfoFactory().loadBlobInfo(blobKey);
    if (blobInfo.getSize() == 0) {
      blobstoreService.delete(blobKey);
      return null;
    }

    // We could check the validity of the file here, e.g. to make sure it's an image file
    // https://stackoverflow.com/q/10779564/873165

    // Use ImagesService to get a URL that points to the uploaded file.
    ImagesService imagesService = ImagesServiceFactory.getImagesService();
    ServingUrlOptions options = ServingUrlOptions.Builder.withBlobKey(blobKey);

    // To support running in Google Cloud Shell with AppEngine's devserver, we must use the relative
    // path to the image, rather than the path returned by imagesService which contains a host.
    try {
      URL url = new URL(imagesService.getServingUrl(options));
      return url.getPath();
    } catch (MalformedURLException e) {
      return imagesService.getServingUrl(options);
    } catch (IllegalArgumentException e) {
      // This occurs when " If options does not contain a valid blobKey or 
      // googleStorageFileName"; one example is when a non-image is uploaded.
      return NOT_AN_IMAGE_EXCEPTION;
    }
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
    private String imageUrl;

    // This constructor can create a Comment Entity (no ID required).
    public Comment(String text, Date date, String imageUrl) {
      this(0, text, date, imageUrl);
    }

    // This constructor can accept a Comment Entity object (includes ID).
    public Comment(long id, String text, Date date, String imageUrl) {
      this.id = id;
      this.text = text;
      this.date = date;
      this.imageUrl = imageUrl;
    }

    public Entity createCommentEntity() {
      // Create a comment entity.
      Entity commentEntity = new Entity(COMMENT_NAME);
      commentEntity.setProperty(COMMENT_TEXT, this.text);
      commentEntity.setProperty(COMMENT_DATE, this.date);
      commentEntity.setProperty(COMMENT_IMAGE, this.imageUrl);
      return commentEntity;
    }
  }

}
