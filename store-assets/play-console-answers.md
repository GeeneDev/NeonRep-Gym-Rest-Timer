# GymReps: Play Console answers (draft)

## Privacy policy

URL to paste in Play Console: https://humorous-rabbit.staticdomains.app/ (hosted on static.app; source is docs/index.html in this repo).

Original draft text (the live page supersedes it):

**Gym Reps Privacy Policy** (last updated 14 November 2026)

Gym Reps counts your sets and times your rests. Everything you enter (exercise names, notes, history, groups and exercise photos) is stored only on your device. Gym Reps has no account, no servers, no analytics, no ads and no tracking. We do not collect, share or sell any personal data.

- **Photos:** if you add a photo to an exercise, it is saved in the app's private storage on your phone and is never uploaded.
- **Purchases:** the optional one-time "full version" unlock is handled by Google Play Billing. Google processes the payment under its own privacy policy; Gym Reps only receives a confirmation that the unlock was purchased.
- **Notifications and alarms:** used only to show the running timer and alert you when a rest ends.
- **Backup:** Android's own device backup may include the app's data if you have it turned on. This is controlled by your Google account settings.
- **Deleting data:** clear the app's data or uninstall the app to remove everything.
- **Children:** the app is not directed at children under 13.

Contact: (your support email)

## Data safety form

- Does your app collect or share any of the required user data types? **No**
- Is all user data encrypted in transit? Not applicable (no data leaves the device)
- Account creation: **No accounts**
- Note: purchase history is handled by Google Play, not by the app, so you do not declare it.

## Foreground service declaration (specialUse)

- Type: **Special use** (`FOREGROUND_SERVICE_SPECIAL_USE`)
- Description: "Workout set and rest timer. While a workout session runs, the service shows an ongoing notification with the live timer and alerts the user the moment a rest period ends, even with the screen off. It starts only when the user taps START and stops when they finish or reset the session."
- User impact if deferred or interrupted: the rest alert would arrive late or not at all, and the user would miss the start of their next set.
- Video: record a 30 s screen capture: tap START, tap to rest, lock the screen, show the notification counting and the "Rest over" alert.

## Exact alarm (SCHEDULE_EXACT_ALARM)

- Used as a backup so the "Rest over" alert fires on time in Doze. The user is asked once and can decline; the app falls back to an inexact alarm.

## Content rating questionnaire (IARC)

- Category: **Utility, productivity, communication or other**
- Violence, sexuality, language, controlled substances, gambling: **No** to all
- User-generated content shared with others / user interaction: **No**
- Shares location: **No**
- Digital purchases: **Yes** (one-time in-app unlock)
- Expected result: PEGI 3 / Everyone

## Other setup

- Target audience: 18+ (or 13+), not designed for children
- Ads: **No ads**
- Upload: `release/GymReps-1.0-vc1.aab`, enrolled in Play App Signing (the keystore is your upload key)
