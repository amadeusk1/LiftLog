# LiftLog

LiftLog is a simple, offline, personal record (PR) and bodyweight tracking application for Android.  
It is built using Kotlin and Jetpack Compose and focuses on clarity, speed, and minimal user interaction.

---

## Overview

LiftLog allows users to log strength training data and visualize progress through clean, lightweight graphs.  
All data is stored locally on the device using internal storage, ensuring full privacy and offline reliability.

The application includes two primary tracking features:

- Personal Records (PRs) for any exercise  
- Daily bodyweight entries  

Users can add, edit, delete, and filter their entries directly from the interface.

---

## Features

### PR Tracking
- Record exercise name, weight, reps, and date  
- Edit and delete existing entries  
- Filter PRs by:
  - This month  
  - This year  
  - All time  
- View progress on an auto-generated line graph for the selected exercise

### Bodyweight Tracking
- Log daily bodyweight with date  
- Edit and delete entries  
- Filter by:
  - This month  
  - This year  
  - All time  
- Graph automatically displays when enough data points exist

### User Interface
- Jetpack Compose UI with Material 3 components  
- Clean, minimal layout focused on ease of use  
- Dialog-based forms for adding and editing data  
- Floating action button for quickly creating new entries

### Data Storage
- Uses internal storage text files for persistence  
- Separate files for PRs, bodyweight entries, and unit preferences  
- All records stored locally with no network dependency

---

## Screenshots

(Place images in `/screenshots` and update the paths if needed.)

- PRs screen  
  `screenshots/liftlog_prs.png`

- Add PR dialog  
  `screenshots/liftlog_add_pr.png`

- Bodyweight screen  
  `screenshots/liftlog_bodyweight.png`

---

## Technology Stack

- Kotlin  
- Jetpack Compose  
- Material 3  
- File-based persistence using Android internal storage  
- Basic date operations via `java.time.LocalDate`  
- Custom graph rendering using Compose `Canvas`

---


