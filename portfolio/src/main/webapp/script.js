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