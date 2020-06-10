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

// The "fact number" of the previous fact given. "-1" is special value.
let prevFactNum = -1;

/**
 * Adds a random fun fact about myself to the page.
 */
function addFunFact() {
  const facts =
    ['I have two older brothers!',
      'I\'ve played tennis for fourteen years!', 
      'My favorite book is Enlightenment Now by Steven Pinker.', 
      'My favorite TV show is It\'s Always Sunny in Philadelphia.'];

  // Pick a random number corresponding to a fun fact.
  let factNum = Math.floor(Math.random() * facts.length);

  // If the same fact number is chosen twice consecutively, change the fact number.
  if (factNum === prevFactNum) {
    factNum++;
    if (factNum == 4) {
      factNum = 0;
    }
  }

  // Update the previous fact number.
  prevFactNum = factNum;

  // Pick the corresponding fun fact.
  const fact = facts[factNum];

  // Add it to the page.
  const factContainer = document.getElementById('fact-container');
  factContainer.innerText = fact;

    // Add 'Hide fun fact' button to the page.
  const hideFactButton = document.getElementById('fun-fact-hide');
  hideFactButton.style.display = 'inline-block';
}

/**
 * Hides the fun fact from user.
 */
function hideFunFact() {
  // Remove it from the page. 
  const factContainer = document.getElementById('fact-container');
  factContainer.innerText = '';

  // Remove 'Hide fun fact' button from the page.
  const hideFactButton = document.getElementById('fun-fact-hide');
  hideFactButton.style.display = 'none';
}

/**
 * Fades in / out div section once the window reaches the top-quarter of the div.
 * Works with individual or multiple divs.
 */
function fadeDiv(classOrIdName) {
  var window_bottom = $(window).scrollTop() + $(window).height();

  // Conditionally fade each div of specified class or id name.
  $(classOrIdName).each(function() {
    var object_top_quarter = $(this).offset().top + $(this).outerHeight() / 4;

    // Fade div if window reached top-quarter marker.
    if (window_bottom > object_top_quarter) {
      $(this).addClass('fade-in');
    } else {
      $(this).removeClass('fade-in');
    }
  });
}

// Triggered upon DOM load.
$(document).ready(() => {
  // Add servlet information to frontend.
  addComments();
  addAuth();

  // Add frontend styling.
  fadeDiv('.project');

  // Disable post comment and max comments shown submit buttons, by default.
  disableSubmit('comment-submit');
  disableSubmit('comment-max-submit');

  // Event every time user scrolls.
  $(window).scroll(() => {
    fadeDiv('.project');
  });

  // Event every time the window resizes.
  $(window).resize(() => {
    fadeDiv('.project');
  });

  // Event every time the screen orientation changes.
  $(window).on('orientationchange', () => {
    fadeDiv('.project');
  });
});

/*
 * Fetches comment data from server (limiting comments to the 'comment max'),
 * builds, styles, and displays comments on the frontend.
 */
function addComments() {
  fetch('/data').then(response => response.json()).then((comments) => {
    const commentContainer = document.getElementById('comment-container');
    comments.forEach((comment) => {
      commentContainer.append(createComment(comment));
    });
  });
}

/** 
 * Creates an <li> element containing text, date posted, and a delete button.
 */
function createComment(comment) {
  const commentElement = document.createElement('li');
  commentElement.className = 'comment-element';

  // Create span element holding text.
  const textElement = document.createElement('span');
  textElement.innerText = comment.text;
  textElement.className = 'span-comment';

  // Create span element holding the Date.
  const dateElement = document.createElement('span');
  dateElement.innerText = comment.date + " UTC";
  dateElement.className = 'span-comment span-comment-date';

  // Create a delete button that triggers another function.
  const deleteButtonElement = document.createElement('button');
  deleteButtonElement.innerText = 'Delete';
  deleteButtonElement.addEventListener('click', () => {
    deleteComment(comment);

    // Remove the task from the DOM.
    commentElement.remove();
  });
  deleteButtonElement.className = 'span-comment';

  // Add the text, date, and delete button to the comment.
  commentElement.appendChild(textElement);
  commentElement.appendChild(dateElement);
  commentElement.appendChild(deleteButtonElement);
  return commentElement;
}

/**
 * Delete a comment, remove it from datastore.
 */
function deleteComment(comment) {
  const params = new URLSearchParams();
  params.append('id', comment.id);
  fetch('/delete-data', {method: 'POST', body: params});
}

/**
 * Disables the "submit" button of a particular element by default.
 */
function disableSubmit(elementId) {
  document.getElementById(elementId).disabled = true;
}

/**
 * Check if the correlated input element box has text; if so, enable the 
 * "submit" button corresponding to that input element.
 */
function checkSubmit(inputElementId, submitElementId) {
  let inputElement = document.getElementById(inputElementId).value;
  if (inputElement === '') {
    document.getElementById(submitElementId).disabled = true;
  } else {
    document.getElementById(submitElementId).disabled = false;
  }
}

/**
 * Add the user login/logout status to page, along with email (if applicable).
 * Provides options to login and logout.
 */
function addAuth() {
  fetch('/auth').then(response => response.json()).then((authObj) => {
    let authContainerDiv = document.getElementById('auth-container');

    // Dynamically construct auth information based on user login status.
    if (authObj.loggedIn) {
      // Create paragraph element holding the email.
      const displayEmail = document.createElement('p');
      displayEmail.innerText = 'Email: ' + authObj.email;

      // Create link element that allows users to log out.
      const logoutLink = document.createElement('a');
      logoutLink.innerText = 'Logout';
      logoutLink.href = authObj.logoutUrl;

      // Add the components to the auth-container div.
      authContainerDiv.append(displayEmail);
      authContainerDiv.append(logoutLink);
    } else {
      // Create link element that allows users to log in.
      const loginLink = document.createElement('a');
      loginLink.innerText = 'Login';
      loginLink.href = authObj.loginUrl;
      
      // Add the login link to the auth-container div.
      authContainerDiv.append(loginLink);
    }

  });
}

/*
 * Deletes all comments presently in datastore.
 */
function deleteAllComments() {
  // Hide all comments from the frontend.
  const commentElements = document.getElementsByClassName("comment-element");
  Array.prototype.forEach.call(commentElements, (commentElement) => {
    commentElement.style.display = 'none';
  });
  // Delete all the comments in datastore.
  fetch('/delete-data', {method: 'POST'});
}
