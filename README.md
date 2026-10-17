# Personal Tasks

Native Android / Jetpack Compose task manager using a warm pink/brown Material 3 dark
palette, standard Material typography, generously padded rounded cards, and accessible icon controls.

## Features

- Built-in My Tasks and independent custom lists.
- Create, rename, and delete custom lists.
- Create, edit, move, delete, complete, and restore tasks.
- Three priority levels: **None**, **★ Important**, and **★★ Super important**.
  Tap the star control to choose a level explicitly. The Priority tab includes
  important tasks across lists, with super-important tasks first.
- Collapsible completed section and sorting by due date, creation, or title.
- Monday-first local calendar with month navigation, optional time, and clearing.
- Realtime Firebase listeners, disk caching, queued offline writes, visible errors,
  and save forms that wait for the server to confirm success.

## Firebase

The configured database is
`https://assignment2-be688-default-rtdb.firebaseio.com`.

```
devices/{deviceId}/lists/{listId}
devices/{deviceId}/tasks/{taskId}
```

The app generates a random UUID once per installation. Its preference file is
excluded from cloud backup and device transfer. Clearing app data or reinstalling creates a different ID; it does
not delete the previous installation's remote records.

There is no Firebase Authentication. A device ID separates datasets; it does not
provide authenticated ownership enforcement. Existing database rules must allow
the no-auth reads and writes used by this application.

The old shared `/tasks` collection and old local task preferences are not deleted
or automatically imported into a device's private namespace.

Dates are Unix milliseconds; timezone and optional-time flags are stored with the
task. Unset due dates and completion timestamps are absent in RTDB, which treats
null values as deleted children. Creation/update/completion times use server timestamps.

## Following the Firebase code

1. `TaskStore.start()` attaches `ValueEventListener`s to this device's lists and tasks.
2. Each `onDataChange` converts Firebase records into Kotlin data classes and updates
   Compose state. That is what makes the screen update in realtime.
3. `saveTask()` uses `push()` for a new ID and reuses that ID for edits or moves.
4. `write()` calls `updateChildren()` to patch fields and reports success/failure.
5. Checkbox and priority actions patch just the fields that changed. Firebase
   sends their updated values back through the same listeners.
6. `stop()` detaches listeners when the UI is disposed.

Priority is stored as the number `0`, `1`, or `2`. The reader also accepts the old
Boolean format: `true` becomes Important and `false` becomes None. Existing data
is not bulk rewritten. Old repeat/order/archive metadata is ignored; previously
archived lists are visible again. Completing any task now only toggles completion;
it never creates another task. Tasks already created by the old recurrence feature remain.

Scheduling is entirely local UI plus Firebase data. No Google Calendar APIs or
calendar permissions are used. Local notification reminders are not implemented.

## Build

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest
```
