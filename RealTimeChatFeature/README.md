# 📱 RealTimeChat App

This is an Android application demonstrating a real-time chat interface.

## Project Overview

The RealTimeChat app allows users to send and receive messages in a chat-like interface. It is built using modern Android development practices, including Jetpack Compose for the UI and the MVVM (Model-View-ViewModel) architectural pattern.

*It simulates a basic, real-time chat experience.*

## Architecture: MVVM (Model-View-ViewModel)


*Folder Structure Design (MVVM)*

This structure promotes separation of concerns, testability, and maintainability.

  ```
   ├── data
   │    ├── local
   │    │   ├── ChatDatabase.kt
   |    |   ├── ChatMessageEntity.kt
   |    |   ├── GroupMembersEntity.kt
   |    |   └── MessageDao.kt 
   │    ├──model
   │    │    └── MessageStatus.kt
   │    └── repository
   │        └── ChatRepository.kt
   |
   ├── ui
   │   ├── chatscreen
   │   │   ├── ChatScreen.kt
   │   │   ├── ChatScreenViewModel.kt
   |   |   ├── ChatViewModelFactory.kt
   |   |   |
   │   │   ├── components
   │   │   │   ├── DateSeparator.kt
   │   │   │   └── MessageBubble.kt
   │   │   │   └── MessageInputField.kt
   │   │   │   └── SystemMessage.kt
   │   │   └── ChatScreenState.kt (Sealed Class)
   │   │
   │   └── ChatActivity.kt
   │   
   └── utils
   │   ├── Constants.kt
   │   ├── DateUtils.kt
   │   ├── NetworkMonitor.kt
   │   ├── SharedPreferencesUtil.kt
   │   └── UuidGenerator.kt
   │ 
   └── ChatApplication.kt
    
```

**MVVM Design Breakdown**

**data Layer:** model/ChatMessage.kt: Defines the data structure for a single chat message.
repository/ChatRepository.kt: An interface defining how to interact with chat data (e.g., getMessages(), sendMessage()). This abstracts the data source.

**ui Layer:** ```ChatScreen.kt```: The Composable function responsible for rendering the entire chat UI. It observes the ViewModel's state.
```ChatViewModel.kt```: Holds and manages the UI-related data. It exposes a StateFlow of ChatScreenState to the UI.
```components```: Smaller, reusable Composable functions that make up the ChatScreen (e.g., MessageBubble, MessageInputField).
```ChatScreenState.kt```: A sealed class representing the different states of the ChatScreen (Loading, Content, Error).
```ChatActivity.kt```: The entry point of the application, responsible for setting up the Compose UI.

**utils Layer:** This an overview of the utility classes and constants located in the utils directory. These components are designed to be a central repository for reusable logic and application-wide configurations, helping to maintain a clean, organized, and efficient codebase.

*File Descriptions*

* ```Constants.kt```: This file contains a collection of constants used throughout the application. It defines immutable values such as 
```GENERAL_CHAT_ROOM_ID```, ```MESSAGE_CHAR_LIMIT```, and various keys for ```SharedPreferences```. This centralizes hardcoded values and makes them easy to manage.

* ```DateUtils.kt```: This utility class provides helper functions for formatting dates and times. It includes methods like ```formatTime``` for displaying a timestamp in a 12-hour format with an AM/PM marker (e.g., "hh:mm a"), and ```formatDate``` for a full date format (e.g., "dd MMMM yyyy"). It also has a function 
```isSameDay``` to check if two timestamps fall on the same day.

* ```NetworkMonitor.kt```: The ```NetworkMonitor``` object is responsible for checking the device's network connectivity. It provides an 
```isOnline()``` function to determine if there is an active internet connection (Wi-Fi, cellular, or Ethernet). This is essential for applications that need to handle network state changes and offline behavior.

* ```SharedPreferencesUtil.kt```: This class simplifies the process of interacting with ```SharedPreferences```. It offers type-safe methods to save and retrieve simple data types, such as strings, and centralizes the logic for local data persistence.

* ```UuidGenerator.kt:``` This utility provides a simple function, ```generateUniqueId()```, to create universally unique identifiers (UUIDs). This is useful for assigning unique IDs to entities within the application, such as chat messages or user sessions.

## Dummy Data Structure

The application utilizes dummy data to simulate chat messages. The primary data structure for a message might look like this
```
data class ChatMessage(
    val id: String,
    val text: String,
    val senderId: String,
    val timestamp: Long,
    val isSystemMessage: Boolean = false
)
```
This data is likely stored in a list within the ViewModel or a repository and is used to populate the chat UI.

*The ViewModel initializes a list of these `ChatMessage` objects upon creation.*

## 💡 UI Logic

The user interface is built entirely with Jetpack Compose, a modern declarative UI toolkit for Android.

*   **Main Screen:** The primary screen displays a list of chat messages and an input field for typing new messages.
*   **Message List:**
    *   A `LazyColumn` is likely used to efficiently display a potentially long list of messages.
    *   Each item in the `LazyColumn` is a Composable function that renders a single chat message, potentially styled differently based on whether the message was sent by the current user or another participant.
*   **Message Input:**
    *   A `TextField` or `OutlinedTextField` Composable allows the user to type their message.
    *   A "Send" button triggers the action of sending the message.
*   **State Management:**
    *   The ViewModel exposes the list of messages and the current text in the input field as `State` objects (e.g., using `mutableStateOf` and observed via `collectAsStateWithLifecycle`).
    *   When the user types in the input field, the `State` in the ViewModel is updated.
    *   When the "Send" button is pressed, the ViewModel processes the new message (e.g., adds it to the list of messages) and the UI automatically recomposes to reflect the change.

## How to Build and Run 🚀

1.  Clone the repository.
2.  Open the project in Android Studio.
3.  Let Gradle sync the project.
4.  Run the app on an Android emulator or a physical device.

# 🔥 Firebase Integration Guide
**Prerequisites**
* Android Studio
* Create firebase account (https://firebase.google.com/)
* Please check Google Play services on your Android device/emulator

**Step 1: Create a Firebase Project**
1. Go to Firebase Console
2. Click Add project → Enter project name → Follow setup wizard

**Step 2: Add Firebase to Your Android App**
1. In Firebase Console:
* Click Android icon
* Enter your package name (e.g.com.calluscompany.ocrapp)
* Click Register app

**Step 3: Download Config File**
1. Download google-services.json
2. Place it in your Android project app module root directory: ```/app/google-services.json```

## Firebase Database Choice: Firestore
For this chat feature, Firebase Firestore has been chosen over Firebase Realtime Database. Here's the reasoning:

**Flexible Data Model:** Firestore uses a document-oriented data model, which is highly flexible and scales well for complex, nested data structures like chat messages (which can include text, timestamps, sender info, and status).

**Powerful Querying:** Firestore offers more robust and efficient querying capabilities, including compound queries and ordering across multiple fields, crucial for fetching message history.

**Scalability:** Firestore is designed for large-scale applications and automatically handles data scaling.

**Offline Support:** Firestore's offline persistence is advanced and reliable, allowing users to interact with the app even without an internet connection.

**Readability and Maintainability:** Firestore's collection-document model often leads to more organized and readable data structures.

## Firebase Structure
The data in Firestore is organized into collections and documents. For this chat application, the primary structure is as follows:

* ``` /chatrooms/{roomId}:```
   * This is a collection named chatrooms.
   * Each document within this collection represents a specific chat room. The ```{roomId}``` is the unique identifier for that room (e.g., general_chat_room for a public chat).
   * Each chat room document can contain metadata about the room (though not explicitly used in the provided code, it's a common pattern).

* ```/chatrooms/{roomId}/messages/{messageId}:```
  * This is a subcollection named messages nested within each chat room document.
  * Each document within the messages subcollection represents a single chat message.
  * The ```{messageId}``` is the unique identifier for that message, typically a UUID generated on the client side (message.id).

Example Structure:
```
chatrooms/
├── general_chat_room/
│   └── messages/
│       ├── message_id_1/
│       │   ├── id: "message_id_1"
│       │   ├── senderId: "user_abc"
│       │   ├── senderName: "Alice"
│       │   ├── text: "Hello everyone!"
│       │   ├── timestamp: 1678886400000
│       │   ├── status: "SENT"
│       │   └── isSystemMessage: false
│       ├── message_id_2/
│       │   ├── id: "message_id_2"
│       │   ├── senderId: "user_xyz"
│       │   ├── senderName: "Bob"
│       │   ├── text: "Hi Alice!"
│       │   ├── timestamp: 1678886460000
│       │   ├── status: "SENT"
│       │   └── isSystemMessage: false
│       └── ...
└── private_chat_room_userA_userB/
    └── messages/
        └── ...
```

## Message JSON Format (ChatMessage Data Class)
The ChatMessage Kotlin data class directly maps to the JSON structure of documents stored in the /messages subcollection.
```
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(), // Unique ID for the message (Firestore Document ID)
    val senderId: String = "",       // ID of the user who sent the message (e.g., Firebase Auth UID)
    val senderName: String = "",     // Display name of the sender
    val text: String = "",           // The content of the message
    val timestamp: Long = DateUtils.getTimestamp(), // Timestamp when the message was created (milliseconds)
    val status: MessageStatus = MessageStatus.SENDING, // Current status: SENDING, SENT, FAILED
    val isSystemMessage: Boolean = false // True for system-generated messages, false for user messages
)

enum class MessageStatus {
    SENDING, // Message is currently being sent/queued locally
    SENT,    // Message has been successfully sent and confirmed by Firestore server
    FAILED   // Message sending failed (e.g., no internet, local write error)
}
```
## Error Handling Logic
The application employs a robust error handling strategy to provide clear feedback to the user regarding message sending status and general UI states.

**1. Optimistic UI Updates**
* When a user sends a message, it is immediately added to the local _messages list in ChatScreenViewModel with a status of MessageStatus.SENDING. This provides instant visual feedback to the user.

**2. Network Connectivity Check**
* Before attempting to send or retry a message, ChatScreenViewModel uses NetworkMonitor.isOnline() to check for an active internet connection.

* If no internet is detected:
   * The message's status is immediately updated to MessageStatus.FAILED in the local _messages list.
   * A ChatScreenState.Error is emitted to the _uiState, which can be displayed as a retry icon.

**3. Firestore Write Result Handling**
* The ChatRepository.sendMessage() method attempts to write the message to Firestore. Due to Firestore's offline persistence, this operation often succeeds locally even without internet (queuing the message for later sync).
* The Result<Unit> returned by sendMessage() primarily indicates if the local write to Firestore's cache was successful.
* If result.isFailure is true (e.g., due to invalid data, client-side permission issues, or if offline persistence was disabled and no connection was available at the moment of the call):
  * The message's status is immediately updated to MessageStatus.FAILED in the local _messages list.
  * A ChatScreenState.Error is emitted to _uiState, which can be displayed as a retry icon.

**4. Real-time Status Confirmation (The Source of Truth)**
* The ChatScreenViewModel continuously observes the Firestore messages collection via chatRepository.getMessages().collect().
* This real-time listener is the ultimate source of truth for a message's SENT status. When a message successfully syncs to the Firestore server, it will be re-emitted by this listener.
* When the collect block receives the server-confirmed message, it updates the _messages StateFlow. The MessageBubble Composable, observing message.status, will then automatically transition the message's icon from SENDING to SENT (double tick).

**5. UI State Management (ChatScreenState)**
* The _uiState StateFlow in ChatScreenViewModel is used to communicate overall screen status and one-time events to the UI:
 * ChatScreenState.Loading: Initial state while messages are being fetched.
 * ChatScreenState.Success: Messages have been loaded, and the screen is ready.
 * ChatScreenState.Error(messageResId, args): Used for all error notifications (input validation, network issues, send/retry failures, load failures). The UI resolves the messageResId and args to display a user-friendly message (e.g., a Retry Icon).

## Listener Lifecycle
The listener is managed by the ```ChatScreenViewModel``` and ```ChatRepository```.
* **Initialization:** When the ```ChatScreenViewModel``` is created, it calls ```startFirestoreMessageListener``` from the ```ChatRepository```.
* **Real-time Updates:** This listener, a coroutine launched in the applicationScope, continuously listens for real-time updates from a specific Firestore chat room.
* **Data Flow:** Any new or updated messages from Firestore are collected as a snapshot and then inserted or updated into the local Room database.
* **UI Observation:** The UI observes a Flow from the local Room database, which serves as the single source of truth. This ensures that the UI always displays the most up-to-date messages, whether from the local cache or a recent Firestore update.

## Deduplication
Deduplication is handled implicitly through the use of a local database and Firestore's real-time updates.
* **Firebase Listener:** The ```startFirestoreMessageListener``` function in the ```ChatRepository``` retrieves all messages from Firestore and passes them to the ```messageDao```.
* ```OnConflictStrategy.REPLACE:``` The insertMessages method in the ```MessageDao``` uses ```OnConflictStrategy.REPLACE```. This means that if a message with the same primary key (id) already exists in the Room database, the new version from Firestore will replace it, effectively handling updates and preventing duplicate messages from being stored.

## Scroll Logic
The scroll behavior is implemented in the ```ChatScreen.kt``` file using LazyColumn and LaunchedEffect.
* ```LazyListState:``` ```LazyListState``` is created with ```rememberLazyListState()``` to manage the scroll position of the ```LazyColumn```.
* **Auto-scrolling to the bottom:** ```LaunchedEffect``` observes changes to the size of the ```messages``` list. Whenever a new message is added (causing the list size to change), ```animateScrollToItem``` is called to automatically scroll the list to the last item, ensuring the user is always at the bottom of the conversation.

## Firebase Listener
The Firebase listener is a core component of this application's real-time functionality.
* **Implementation:** The ```startFirestoreMessageListener``` function in ```ChatRepository.kt``` sets up the listener on a Firestore collection.
* **Purpose:** The listener's purpose is to synchronize the Firestore data with the local Room database. It queries the ```messages``` subcollection within a specific ```chatroom document```, ordered by the ```timestamp```.
* **Data Handling:** The listener's snapshots are collected as a ```Flow```, and the remote messages are then mapped to ```ChatMessageEntity``` objects before being inserted into the local database. This ensures the local data is always a reflection of the remote data.

## Unread messages tracking logic and schema

This document outlines the logic and schema used to track and display unread message counts for chat rooms within the application.

#### 1. Data Schema

The unread message count and other chat room data are managed using a local Room database, which is synchronized with Firestore.

**`ChatRoomEntity.kt`**
This is the primary data class representing a chat room in the local database. It contains all the necessary information for a chat room item in the list screen, including the unread message count.

* **`roomId`**: A unique ID for the chat room.
* **`groupName`**: The name of the chat room.
* **`lastMessage`**: The text of the last message sent in the room. This field is updated in the `ChatRoomsViewModel` after fetching the chat room list.
* **`lastTimestamp`**: The timestamp of the last message.
* **`unreadCount`**: An integer representing the number of unread messages for the current user.
* **`lastReadTimestamp`**: The timestamp of the last message the user read in this room. This is a critical field for calculating `unreadCount`.

#### 2. Unread Message Count Logic

The logic for tracking and displaying unread messages is handled by a combination of a WorkManager, the `ChatRoomsRepository`, a Data Access Object (DAO), and the `ChatRoomsViewModel`.

**Data Flow**
1.  **Data Source**: The application uses a `Room` database to store `ChatRoomEntity` and `ChatMessageEntity` objects locally.
2.  **`TimestampInitializationWorker`**: This worker is responsible for initializing the `lastReadTimestamp` for a given chat room. This is critical for ensuring that when a user joins a new room, they don't see all previous messages as "unread." It sets the `lastReadTimestamp` to the current time, effectively resetting the unread count to zero for that user and room.
3.  **Repository Layer**: The `ChatRoomsRepository` is responsible for providing a flow of chat rooms that includes the unread count for each room.
4.  **ViewModel Layer**: The `ChatRoomsViewModel` observes this flow from the repository. When a new list of chat rooms is emitted, it iterates through each `ChatRoomEntity`.
5.  **Last Message Fetching**: For each chat room, the `ChatRoomsViewModel` calls `chatRoomsRepository.getLastMessageForRoom(chatRoom.roomId.toString())` to fetch the last message text, and then uses `chatRoom.copy(lastMessage = lastMessage)` to create a new `ChatRoomEntity` with the updated last message information.
6.  **UI Update**: The updated list of `ChatRoomEntity` objects is then passed to the UI, where the `ChatRoomItem` composable displays the `unreadCount` and `lastMessage` fields.

**Key Implementation Details**

* **Firebase Listener**: A Firestore listener is used to keep the local `Room` database up-to-date with remote changes. When new messages arrive, they are inserted into the local database, which triggers the flow to update.
* **`MessageDao`**: The `MessageDao` contains a query to calculate the number of unread messages for a given room and user. This is typically done by counting all messages that have a timestamp greater than the user's `lastReadTimestamp` for that room.
* **`SharedPreferences`**: The current user's ID is retrieved from `SharedPreferences` to ensure the unread count is specific to their account.

This Android application implements a real-time chat feature using **Firebase Cloud Firestore** for backend data storage and **Firebase Cloud Messaging (FCM)** for notifications. It also utilizes **Room Persistence Library** for local data caching and **WorkManager** for background synchronization tasks.


## FCM Structure and Integration

The application integrates FCM to deliver real-time notifications for new chat messages.

### AndroidManifest.xml Configuration

The `AndroidManifest.xml` declares the `RealtimeChatMessagingService`, a custom FirebaseMessagingService, to handle incoming FCM messages:

```xml
<service
    android:name=".workers.RealtimeChatMessagingService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>
```

This declaration ensures that the `RealtimeChatMessagingService` is triggered whenever a new FCM message is received by the device. The `android:exported="false"` attribute ensures that this service is not accessible by other applications.

### RealtimeChatMessagingService (`workers/RealtimeChatMessagingService.kt`)

This service is the core component for handling FCM messages:

  * **`onMessageReceived(remoteMessage: RemoteMessage)`**:
      * This method is invoked when an FCM message is received.
      * It first calls `NotificationHelper.createNotificationChannel()` to ensure the notification channel exists (important for Android 8.0+ notifications).
      * It checks if the `remoteMessage` contains data (payload).
      * If data is present and the `type` is "chat\_message", it extracts `roomId`, `messagePreview`, and `senderId`.
      * Finally, it calls `NotificationHelper.showChatNotification()` to display a system notification to the user.
  * **`onNewToken(token: String)`**:
      * This method is called when a new FCM registration token is generated (e.g., on initial app install, app data clear, or token refresh).
      * While the current implementation simply calls `super.onNewToken(token)`, in a production application, this token would typically be sent to your application server. The server uses these tokens to send targeted notifications to specific devices.

### NotificationHelper (`utils/NotificationHelper.kt`)

This utility object is responsible for creating and displaying notifications:

  * **`createNotificationChannel(context: Context)`**:
      * The channel is named "Chat Notifications" with a high importance.
  * **`showChatNotification(context: Context, roomId: String, message: String, senderId: String?)`**:
      * Builds and displays a notification for a new chat message.
      * It creates an `Intent` to launch `ChatActivity` when the notification is tapped.
      *  ⚠️(Issue) Crucially, it includes `EXTRA_ROOM_ID` in the intent, allowing `ChatActivity` to deep-link directly to the relevant chat room.
      * A `PendingIntent` is created to wrap this `Intent`, making it available to the system.
      * The `NotificationCompat.Builder` is used to construct the notification with a small icon, title, content text, and priority.
      * `setAutoCancel(true)` ensures the notification is dismissed when the user taps it.
      * `NotificationManagerCompat.from(context).notify()` displays the notification.

-----

## Read Tracking Logic Analysis

The application implements a robust read tracking mechanism to manage unread message counts in chat rooms, leveraging both **Firestore** and **Room Database**.

### Data Models (`data/local/ChatMessageEntity.kt`, `data/local/ChatRoomEntity.kt`)

  * **`ChatMessageEntity`**: Represents an individual chat message. Key fields for read tracking include:
      * `timestamp`: The time the message was sent.
      * `senderId`: The ID of the user who sent the message.
      * `status`: (Though primarily for send status, it indirectly relates to message delivery/read state in a broader sense).
  * **`ChatRoomEntity`**: Represents a chat room. Key fields for read tracking include:
      * `lastTimestamp`: Timestamp of the latest message in the room.
      * `lastReadTimestamp`: **The most critical field for read tracking.** This stores the timestamp of the *last message the current user has read in that specific chat room*.
      * `unreadCount`: This field is **calculated dynamically** in the DAO query rather than being stored directly in the `ChatRoomEntity` itself, ensuring accuracy.

### MessageDao (`data/local/MessageDao.kt`)

The `MessageDao` provides the core database operations for read tracking:

  * **`getAllChatRoomsWithUnreadCount(currentUserId: String): Flow<List<ChatRoomEntity>>`**:
      * This is the primary query for displaying chat rooms with their unread counts on the main screen.
      * It performs a `LEFT JOIN` between `CHAT_ROOM` and `MESSAGES` tables.
      * The `SUM(CASE WHEN T2.senderId != :currentUserId AND T2.timestamp > T1.lastReadTimestamp THEN 1 ELSE 0 END)` clause is the **heart of the unread message calculation**.
          * It counts messages where the `senderId` is *not* the `currentUserId` (i.e., messages sent by others).
          * AND the message `timestamp` is *greater than* the `lastReadTimestamp` of the chat room.
          * This effectively counts all messages from other users that arrived *after* the user last read the room.
      * The results are grouped by `roomId` and ordered by `lastTimestamp` (most recent activity first).
  * **`updateLastReadTimestamp(roomId: String, timestamp: Long)`**:
      * This crucial method updates the `lastReadTimestamp` for a specific `roomId` to the given `timestamp`.
      * This function is called when a user enters a chat room, effectively marking all messages up to that `timestamp` as "read" for that user in that room.
  * **`updateAllLastReadTimestamps(timestamp: Long)`**:
      * This method is used by `TimestampInitializationWorker` to set the `lastReadTimestamp` for all chat rooms to the current time if they are initially `0L` or `null`. This ensures that on the very first launch, all existing messages are considered "read" to avoid a large, inaccurate unread count.

### Repositories (`data/repository/ChatRoomRepository.kt`, `data/repository/ChatRoomsRepository.kt`)

  * **`ChatRoomRepository`**:
      * **`updateLastReadTimestamp(roomId: String, timestamp: Long)`**: Exposes the DAO's update method to the ViewModel.
  * **`ChatRoomsRepository`**:
      * **`insertRooms(rooms: List<ChatRoomEntity>)`**: When new chat rooms are fetched from Firestore, this method ensures that the `lastReadTimestamp` of existing local rooms is preserved. This is vital because the Firestore `ChatRoomEntity` might not contain the `lastReadTimestamp` specific to the *current user*, which is a local-only tracking mechanism. It copies the existing local `lastReadTimestamp` to the updated room object before inserting.
      * **`getAllChatRoomsWithUnreadCount(currentUserId: String): Flow<List<ChatRoomEntity>>`**: Directly exposes the DAO's flow to the `ChatRoomsViewModel`.
      * **`getLastMessageForRoom(roomId: String): String?`**: Fetches the last message text from the local database for a given room. This is used to display a preview of the last message in the chat room list.

### ViewModels (`ui/chatroomscreen/ChatRoomViewModel.kt`, `ui/chatroomsscreen/ChatRoomsViewModel.kt`)

  * **`ChatRoomViewModel`**:
      * **`updateLastReadTimestamp()`**: This method is called in the `init` block and `LaunchedEffect` of `ChatRoomScreen` , ensuring that whenever a user enters a chat room, the `lastReadTimestamp` for that room is updated to the current time. This marks all messages currently visible as read.
  * **`ChatRoomsViewModel`**:
      * Collects the `getAllChatRoomsWithUnreadCount` flow from `ChatRoomsRepository` to provide the `chatRooms` `StateFlow` to the UI. This `StateFlow` directly contains the calculated unread counts for each room, which are then displayed in `ChatRoomsScreen`.

### Workers (`workers/TimestampInitializationWorker.kt`)

  * **`TimestampInitializationWorker`**:
      * This `CoroutineWorker` is enqueued on application launch (in `ChatApplication.onCreate()`).
      * Its `doWork()` method calls `chatRoomsRepository.ensureAllTimestampsInitialized()`.
      * This worker's purpose is to set the `lastReadTimestamp` for all chat rooms to the current time if they are `0L` (indicating they haven't been read before). This prevents a flood of "unread" messages when a user first installs or opens the app, as all historical messages would otherwise be counted as unread.

-----

## Overall Data Flow and Read Tracking Summary

1.  **Initial Setup**: On app launch, `TimestampInitializationWorker` ensures all existing chat rooms have their `lastReadTimestamp` initialized to the current time if not already set.
2.  **Receiving Messages (FCM)**:
      * When a new message arrives via FCM, `RealtimeChatMessagingService` receives it.
      * It extracts relevant data and uses `NotificationHelper` to display a notification.
      * The notification includes a deep link to the specific `roomId`.
3.  **Real-time Data Sync (Firestore to Room)**:
      * `ChatRoomRepository.startFirestoreMessageListener()` continuously listens for new messages in Firestore for a given room.
      * When new messages arrive, they are inserted into the local `ChatMessageEntity` table via `messageDao.insertMessages()`.
      * Similarly, `ChatRoomsRepository.startFirestoreChatRoomsListener()` syncs `ChatRoomEntity` data from Firestore to the local Room database, ensuring `lastReadTimestamp` is preserved during updates.
4.  **Displaying Messages and Unread Counts (UI)**:
      * `ChatRoomViewModel` observes `chatRoomRepository.getLocalMessages()` and `chatRoomRepository.getGroupMembers()` to combine and display messages in the `ChatRoomScreen`.
      * `ChatRoomsViewModel` observes `chatRoomsRepository.getAllChatRoomsWithUnreadCount()`. This query dynamically calculates the `unreadCount` for each room by comparing message timestamps with the `lastReadTimestamp` stored locally.
5.  **Marking as Read**:
      * When a user enters a `ChatRoomScreen`, `ChatRoomViewModel.updateLastReadTimestamp()` is immediately called.
      * This updates the `lastReadTimestamp` for that specific `roomId` in the local Room database to the current time.
      * This update automatically causes the `getAllChatRoomsWithUnreadCount` flow to re-emit, effectively reducing the unread count for that room to zero (or the number of messages received *after* the user entered the room if the timestamp is slightly delayed).
6.  **Offline Handling**:
      * Messages sent while offline are initially marked as `SENDING` in the local Room DB.
      * When connectivity is restored, `ChatRoomRepository.sendAndUpadteMessage()` attempts to send these failed/pending messages to Firestore.
      * If successful, the local status is updated to `SENT`. If it fails again, it's marked `FAILED`.
      * This ensures a responsive UI even without immediate network access and provides retry mechanisms.
7.  **Deletion Sync**:
      * When a chat room is deleted, `ChatRoomsRepository.deleteChatRoom()` performs a soft delete locally and enqueues `DeletionSyncWorker`.
      * This worker, when online, attempts to delete the room from Firestore and then permanently from the local database, ensuring consistency.
---

## ❌ Known Issues

-   **Firestore Rules:** For development purposes, Firestore rules are currently open (`allow read, write: if true;`). These will be secured in upcoming phases to ensure data integrity and user privacy.

-   **Server Integration:** Notifications can currently only be triggered through the Firebase Console for testing. Full server-side integration, which involves storing FCM tokens and dynamically sending notifications, is a key focus for Phase.

-   **Deep Linking:** Tapping a notification does not yet navigate the user to the relevant chatroom. This functionality will be addressed with server-triggered data payloads, ensuring a seamless user experience.

-   **Read/Unread Count:** The logic for updating the `lastReadTimestamp` and counting messages, after entering a chatroom and returning to the room list, sometimes shows the same count.

-   **Image Upload:** Image upload is showing failed because Firebase storage is a paid service, so it cannot be tested, but the code is implemented.

-   **Stretch Features:** Presence tracking and typing indicators are planned for future releases.

---

# What was done

 1. Resend Failed Messages & Delivery Receipts

 2. Offline Support & Local Cache

 3. Message Attachments (PDF/Image/Audio)

 4. Chat Export & Deletion (Personal Data Control)
