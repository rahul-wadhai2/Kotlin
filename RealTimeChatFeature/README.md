# 📱 RealTimeChat App

This is an Android application demonstrating a real-time chat interface.

## Project Overview

The RealTimeChat app allows users to send and receive messages in a chat-like interface. It is built using modern Android development practices, including Jetpack Compose for the UI and the MVVM (Model-View-ViewModel) architectural pattern.

*It simulates a basic chat experience with predefined messages.*

## Architecture: MVVM (Model-View-ViewModel)


*Folder Structure Design (MVVM)*

This structure promotes separation of concerns, testability, and maintainability.

  ```
   ├── data
   │    ├── model
   │    │   └── ChatMessage.kt
   │    └── repository
   │        └── ChatRepository.kt
   │
   ├── ui
   │   ├── chatscreen
   │   │   ├── ChatScreen.kt
   │   │   ├── ChatViewModel.kt
   │   │   ├── components
   │   │   │   ├── MessageBubble.kt
   │   │   │   └── MessageInputField.kt
   │   │   │   └── DateSeparator.kt
   │   │   │   └── CustomBubbleShape.kt
   │   │   └── ChatScreenState.kt (Sealed Class)
   │   │
   │   └── ChatActivity.kt
   │   
   └── utils
       └── DateFormatter.kt
   
```

**MVVM Design Breakdown**

**data Layer:** model/ChatMessage.kt: Defines the data structure for a single chat message.
repository/ChatRepository.kt: An interface defining how to interact with chat data (e.g., getMessages(), sendMessage()). This abstracts the data source.

**ui Layer:** chatscreen/ChatScreen.kt: The Composable function responsible for rendering the entire chat UI. It observes the ViewModel's state.
chatscreen/ChatViewModel.kt: Holds and manages the UI-related data. It exposes a StateFlow of ChatScreenState to the UI.
chatscreen/components: Smaller, reusable Composable functions that make up the ChatScreen (e.g., MessageBubble, MessageInputField).
chatscreen/ChatScreenState.kt: A sealed class representing the different states of the ChatScreen (Loading, Content, Error).

**utils Layer:** DateUtils.kt: Helper for formatting timestamps.

MainActivity.kt: The entry point of the application, responsible for setting up the Compose UI.

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
│       │   ├── clientGeneratedId: "client_id_1"
│       │   ├── senderId: "user_abc"
│       │   ├── senderName: "Alice"
│       │   ├── text: "Hello everyone!"
│       │   ├── timestamp: 1678886400000
│       │   ├── status: "SENT"
│       │   └── isSystemMessage: false
│       ├── message_id_2/
│       │   ├── id: "message_id_2"
│       │   ├── clientGeneratedId: "client_id_2"
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
    val clientGeneratedId: String = UUID.randomUUID().toString(), // Client-generated ID for retry safety (not used as Firestore ID)
    val senderId: String = "",       // ID of the user who sent the message (e.g., Firebase Auth UID)
    val senderName: String = "",     // Display name of the sender
    val text: String = "",           // The content of the message
    val timestamp: Long = System.currentTimeMillis(), // Timestamp when the message was created (milliseconds)
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


