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

/**
 * Adds a random fun fact about myself to the page.
 */
function addRandomFact() {
  const facts =
      ['I have two older brothers!',
       'I\'ve played tennis for fourteen years!', 
       'My favorite book is Enlightenment Now by Steven Pinker.', 
       'My favorite TV show is It\'s Always Sunny in Philadelphia.'];

  // Pick a random fact.
  const fact = facts[Math.floor(Math.random() * facts.length)];

  // Add it to the page.
  const factContainer = document.getElementById('fact-container');
  factContainer.innerText = fact;
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
    fadeDiv('.project');
    // Event every time user scrolls.
    $(window).scroll(() => {
        fadeDiv('.project');
    });
});