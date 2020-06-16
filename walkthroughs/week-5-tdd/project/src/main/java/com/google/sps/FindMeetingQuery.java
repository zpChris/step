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

package com.google.sps;

import java.util.Collections;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;

public final class FindMeetingQuery {

  public Collection<TimeRange> query(Collection<Event> events, MeetingRequest request) {
    
    // This variable will hold all of the invalid meeting times.
    // This list is sorted by start times, and has no overlapping times.
    List<TimeRange> invalidTimeRanges = new ArrayList<>();
    
    // This is the list of the attendees which attend the meeting.
    Collection<String> attendees = request.getAttendees();

    // The duration of the meeting request.
    int meetingDuration = (int) request.getDuration();

    // Populate invalidTimeRanges variable, and sort by increasing start time.
    invalidTimeRanges = getInvalidTimeRanges(invalidTimeRanges, events, attendees);
    Collections.sort(invalidTimeRanges, new Comparator<TimeRange>() {
      public int compare(TimeRange t1, TimeRange t2) {
        if (t1.start() < t2.start()) {
          return -1;
        } else if (t1.start() == t2.start()) {
          return 0;
        } else {
          return 1;
        }
      }
    });

    // Merge conflicting TimeRanges.
    invalidTimeRanges = mergeConflictingTimeRanges(invalidTimeRanges);

    // Get the inverse / valid TimeRanges of the invalidTimeRanges.
    List<TimeRange> validTimeRanges = getValidTimeRanges(invalidTimeRanges, 
      meetingDuration);

    return validTimeRanges;

  }

	/**
   * Returns true if the duration (presumably of a TimeRange) is longer than
   * the meeting duration request.
   */
  private boolean fitsMeetingDuration(int duration, int meetingDuration) {
    return duration >= meetingDuration; 
  }

  /**
   * Get the valid (inverse) TimeRanges from invalidTimeRanges.
   * A valid TimeRange is not added if it is shorter than the meeting request 
   * length.
   * 
   * Example:
   * Meeting Request Duration: 30 minutes.
   * invalidTimeRanges: [(100, 200), (500, 1400), (1700, 1900)]
   * validTimeRanges:   [(0, 100), (200, 500), (1400, 1700), (1900, 2400)]
   */
  private List<TimeRange> getValidTimeRanges(List<TimeRange> invalidTimeRanges, 
    int meetingDuration) {
    List<TimeRange> validTimeRanges = new ArrayList<>();

    // Get the inverse TimeRanges of the invalidTimeRanges.
    int index = 0;
    int start = TimeRange.START_OF_DAY;
    int end = TimeRange.START_OF_DAY;
    while (index < invalidTimeRanges.size()) {
      end = invalidTimeRanges.get(index).start();

      // Add new valid TimeRange to list.
      if (start != end && fitsMeetingDuration(end - start, meetingDuration)) {
        validTimeRanges.add(TimeRange.fromStartEnd(start, end, false));
      }

      // Get the start of the next valid TimeRange, and increment index.
      start = invalidTimeRanges.get(index).end();
      index++;
    }

    // Get the last valid TimeRange, if applicable (inclusive on END_OF_DAY).
    if (start != TimeRange.END_OF_DAY + 1 && 
      fitsMeetingDuration(TimeRange.END_OF_DAY - start, meetingDuration)) {

      validTimeRanges.add(
        TimeRange.fromStartEnd(start, TimeRange.END_OF_DAY, true));
    }

    return validTimeRanges;
  }

  /**
   * Merge any conflicting TimeRanges in the invalidTimeRanges list.
   */
  private List<TimeRange> mergeConflictingTimeRanges(
    List<TimeRange> invalidTimeRanges) {
      
    int index = 0;
    while (index < invalidTimeRanges.size() - 1) {
      TimeRange firstTimeRange = invalidTimeRanges.get(index);
      TimeRange secondTimeRange = invalidTimeRanges.get(index + 1);

      // Check if TimeRanges conflict, and if so, merge them.
      if (firstTimeRange.overlaps(secondTimeRange)) {
        // Create merged TimeRange.
        int endMergedTime = (firstTimeRange.end() > secondTimeRange.end()) ?
          firstTimeRange.end() : secondTimeRange.end();
        TimeRange mergedTimeRange = TimeRange.fromStartEnd(firstTimeRange.start(), 
          endMergedTime, false);

        // Remove old TimeRanges, and add new merged TimeRange.
        invalidTimeRanges.remove(firstTimeRange);
        invalidTimeRanges.remove(secondTimeRange);
        invalidTimeRanges.add(index, mergedTimeRange);
      } else {
        index++;
      }
    }

    return invalidTimeRanges;

  }

  /**
   * Return all of the invalid TimeRanges (conflicting events) to a list.
   */
  private List<TimeRange> getInvalidTimeRanges(
    List<TimeRange> invalidTimeRanges, Collection<Event> events, 
    Collection<String> attendees) {

    // Add event TimeRange if event has attendees in current meeting query.
    for (Event e : events) {
      if (containsElement(attendees, e.getAttendees())) {
        invalidTimeRanges.add(e.getWhen());
      }
    }

    return invalidTimeRanges;
  }

  /**
   * Return true if l1 contains any element in l2; otherwise, false.
   */
  private boolean containsElement(Collection<String> l1, Collection<String> l2) {
    for (String ls2 : l2) {
      if (l1.contains(ls2)) {
        return true;
      }
    }
    return false;
  }
  
}
