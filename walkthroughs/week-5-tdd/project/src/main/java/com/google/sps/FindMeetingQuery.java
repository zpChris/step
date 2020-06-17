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

    // This variable will hold all of the invalid meeting times for optional 
    // attendees. This list is sorted by start times, and has no overlapping times.
    List<TimeRange> invalidOptionalTimeRanges = new ArrayList<>();
    
    // This is the list of the attendees which attend the meeting.
    Collection<String> attendees = request.getAttendees();

    // This is the list of optional attendees for the meeting.
    Collection<String> optionalAttendees = request.getOptionalAttendees();

    // The duration of the meeting request.
    int meetingDuration = (int) request.getDuration();

    // If no attendees (regular or optional) exist, return the whole day.
    if (attendees.isEmpty() && optionalAttendees.isEmpty()) {
      List<TimeRange> allDayTimeRangeList = new ArrayList<TimeRange>();
      allDayTimeRangeList.add(TimeRange.WHOLE_DAY);
      return allDayTimeRangeList;
    } else if (attendees.isEmpty()) {
      // If only optional attendees exist, create a schedule for them.
      invalidOptionalTimeRanges = getAndMergeInvalidTimeRanges(
        invalidOptionalTimeRanges, events, optionalAttendees);
      List<TimeRange> validOptionalTimeRanges = getValidTimeRanges(
        invalidOptionalTimeRanges, meetingDuration);
      return validOptionalTimeRanges;
    }

    // Get and merge both the TimeRanges for regular and optional attendees.
    invalidTimeRanges = getAndMergeInvalidTimeRanges(
      invalidTimeRanges, events, attendees);
    invalidOptionalTimeRanges = getAndMergeInvalidTimeRanges(
      invalidOptionalTimeRanges, events, optionalAttendees);

    // Get the inverse / valid TimeRanges of the invalidTimeRanges.
    List<TimeRange> validTimeRanges = getValidTimeRanges(invalidTimeRanges, 
      meetingDuration);

    // Restrict valid TimeRanges with optional TimeRanges.
    validTimeRanges = restrictTimeRanges(validTimeRanges, 
      invalidOptionalTimeRanges, meetingDuration);

    return validTimeRanges;

  }

  /**
   * Restrict the valid TimeRanges to allow optional attendees to attend.
   * If it is impossible to allow all optional attendees to attend, return 
   * the same valid TimeRanges.
   */
  private List<TimeRange> restrictTimeRanges(List<TimeRange> validTimeRanges, 
    List<TimeRange> invalidOptionalTimeRanges, int meetingDuration) {
    
    // Save the current valid TimeRange list, in case optional attendees cannot attend.
    List<TimeRange> originalValidTimeRanges = new ArrayList<TimeRange>(validTimeRanges);

    // Create indices for validTimeRanges (vtr) and invalidOptionalTimeRanges (iotr).
    int vtrIndex = 0;
    int iotrIndex = 0;
    while (vtrIndex < validTimeRanges.size() - 1 &&
      iotrIndex < invalidOptionalTimeRanges.size()) {

      // Get the two TimeRanges in question.
      TimeRange validTimeRange = validTimeRanges.get(vtrIndex);
      TimeRange optionalTimeRange = invalidOptionalTimeRanges.get(iotrIndex);

      // Restrict the valid TimeRange to fit optional attendees if a conflict occurs.
      if (optionalTimeRange.overlaps(validTimeRange)) {
        List<TimeRange> builtValidTimeRanges = buildValidTimeRanges(validTimeRange, 
          optionalTimeRange, meetingDuration);
        
        // Add newly-built validTimeRanges to list, and delete old valid TimeRange.
        validTimeRanges.remove(validTimeRange);
        for (TimeRange builtValidTimeRange : builtValidTimeRanges) {
          validTimeRanges.add(iotrIndex, builtValidTimeRange);
          iotrIndex++;
        }
        iotrIndex--;
      }

      // Update the vtrIndex or iotrIndex depending on sorted order.
      if (validTimeRanges.get(vtrIndex).end() < 
        invalidOptionalTimeRanges.get(iotrIndex).end()) {
        
        vtrIndex++;
      } else {
        iotrIndex++;
      }
    }

    // If no valid TimeRanges are left, return the original validTimeRanges variable.
    if (validTimeRanges.isEmpty()) {
      return originalValidTimeRanges;
    }
    
    return validTimeRanges;
  }

  /**
   * Return a built list for the new valid TimeRange(s), given that it 
   * conflicts with an optional TimeRange.
   * 
   * An empty list indicates that the valid TimeRange is completely overlapped
   * by the optional TimeRange, or that no valid TimeRange can fit the 
   * specified meeting duration.
   */
  private List<TimeRange> buildValidTimeRanges(TimeRange validTimeRange, 
    TimeRange optionalTimeRange, int meetingDuration) {

    List<TimeRange> builtValidTimeRanges = new ArrayList<>();

    // Separate into different cases, and address specifically.
    // The first 'if' statement ensures that if the optional TimeRange contains
    // the entire valid TimeRange, an empty list is returned.
    if (optionalTimeRange.contains(validTimeRange)) {
      return builtValidTimeRanges;
    } else if (validTimeRange.contains(optionalTimeRange)) {
      TimeRange fromValidStart = TimeRange.fromStartEnd(validTimeRange.start(), 
        optionalTimeRange.start(), false);
      TimeRange fromOptionalEnd = TimeRange.fromStartEnd(optionalTimeRange.end(),
        validTimeRange.end(), false);
      
      // Add the TimeRanges if they fit the meeting duration.
      if (fitsMeetingDuration(fromValidStart.duration(), meetingDuration)) {
        builtValidTimeRanges.add(fromValidStart);
      }
      if (fitsMeetingDuration(fromOptionalEnd.duration(), meetingDuration)) {
        builtValidTimeRanges.add(fromOptionalEnd);
      }
    } else if (validTimeRange.start() < optionalTimeRange.start()) {
      TimeRange fromValidStart = TimeRange.fromStartEnd(validTimeRange.start(), 
        optionalTimeRange.start(), false);
      
      // Add the TimeRange if it fits the meeting duration.
      if (fitsMeetingDuration(fromValidStart.duration(), meetingDuration)) {
        builtValidTimeRanges.add(fromValidStart);
      }
    } else if (optionalTimeRange.start() < validTimeRange.start()) {
      TimeRange fromOptionalEnd = TimeRange.fromStartEnd(optionalTimeRange.end(), 
        validTimeRange.end(), false);
      
      // Add the TimeRange if it fits the meeting duration.
      if (fitsMeetingDuration(fromOptionalEnd.duration(), meetingDuration)) {
        builtValidTimeRanges.add(fromOptionalEnd);
      }
    }

    return builtValidTimeRanges;
  }

  /**
   * Calls both the 'get' and 'merge' functions for preparing the invalid 
   * TimeRanges. This function works with both regular and optional attendees.
   */
  private List<TimeRange> getAndMergeInvalidTimeRanges(List<TimeRange> invalidTimeRanges, 
    Collection<Event> events, Collection<String> attendees) {

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
    return invalidTimeRanges;
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
