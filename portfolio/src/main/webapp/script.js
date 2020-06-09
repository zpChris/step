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
  disablePostComment();

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
 * Adds comments to the page.
 */
function addComments() {
  fetch('/data?limit=2').then(response => response.json()).then((comments) => {
    const commentContainer = document.getElementById('comment-container');
    comments.forEach((comment) => {
      commentContainer.append(createListElement(comment));
    });
  });
}

/** 
 * Creates an <li> element containing text. 
 */
function createListElement(comment) {
  const liElement = document.createElement('li');
  liElement.innerText = "Text: " + comment.text;
  liElement.innerText += "\nTime: " + comment.date + " UTC";
  return liElement;
}

/**
 * Disables the "submit" comment button by default.
 */
function disablePostComment() {
  document.getElementById('comment-submit').disabled = true;
}

/**
 * Check if the comment box has text; if so, enable "submit" button.
 */
function checkPostComment() {
  let commentText = document.getElementById('comment-box').value;
  if (commentText === '') {
    document.getElementById('comment-submit').disabled = true;
  } else {
    document.getElementById('comment-submit').disabled = false;
  }
}

/**
 * Add the user login/logout status to page, along with email (if applicable).
 * Provides options to login and logout.
 */
function addAuth() {
  fetch('/auth').then(response => response.text()).then((authText) => {
    // Parse text to HTML.
    let parser = new DOMParser();
    let authHtml = parser.parseFromString(authText, 'text/html');
    
    // Add authHtml to the auth-container div.
    let authContainerDiv = document.getElementById('auth-container');
    authContainerDiv.append(authHtml.documentElement);
  });
}