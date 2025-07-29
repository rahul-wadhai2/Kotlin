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
   │
   ├── utils
   │   └── DateFormatter.kt
   │
   └── MainActivity.kt
```

**MVVM Design Breakdown**

**data Layer:** model/ChatMessage.kt: Defines the data structure for a single chat message.
repository/ChatRepository.kt: An interface defining how to interact with chat data (e.g., getMessages(), sendMessage()). This abstracts the data source.

**ui Layer:** chatscreen/ChatScreen.kt: The Composable function responsible for rendering the entire chat UI. It observes the ViewModel's state.
chatscreen/ChatViewModel.kt: Holds and manages the UI-related data. It exposes a StateFlow of ChatScreenState to the UI.
chatscreen/components: Smaller, reusable Composable functions that make up the ChatScreen (e.g., MessageBubble, MessageInputField).
chatscreen/ChatScreenState.kt: A sealed class representing the different states of the ChatScreen (Loading, Content, Error).

**util Layer:** DateUtils.kt: Helper for formatting timestamps.

MainActivity.kt: The entry point of the application, responsible for setting up the Compose UI.

## Dummy Data Structure

The application utilizes dummy data to simulate chat messages. The primary data structure for a message might look like this (example in Kotlin):
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

## Future Enhancements
*Implement actual real-time communication using Firebase Realtime Database*
