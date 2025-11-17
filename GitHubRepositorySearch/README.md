# GitHub Repository Search  
A modern Android application built using **MVVM** + **Clean Architecture** + **Jetpack Compose** + **Hilt** + **Retrofit**.  
This project demonstrates scalable architecture, separation of concerns, offline support (local favorites), DI, and reactive UI design.

---

## 🚀 Features  
- Search GitHub repositories  
- View repository details  
- Add/remove favorites (local persistence)  
- Clean Architecture with domain/data/presentation layers  
- Jetpack Compose UI  
- API calls with Retrofit  
- Dependency Injection with Hilt  
- Network Monitoring  
- ViewModel with StateFlow  

---

## 📂 Project Architecture (MVVM + Clean Architecture)

```
com.example.githubrepository
│
├── data                    # Framework & implementation layer
│   ├── local               # Local data sources
│   ├── remote              # Retrofit API and DTO models
│   ├── repository          # Repository implementation
│
├── domain                  # Business logic
│   ├── model               # Clean domain models
│   ├── repository          # Repository interface (abstraction)
│
├── presentation            # UI + ViewModels
│   ├── ui                  # Screens (Compose)
│   ├── viewmodel           # ViewModels for screens
│
├── di                      # Hilt modules
└── MainActivity            # Hosts navigation & root UI
```

---

# 🧠 Clean Architecture Explanation

### **1️⃣ Presentation Layer (Jetpack Compose + ViewModels)**  
- Contains UI screens and ViewModels.  
- Uses `StateFlow` to expose UI state.  
- Does *not* know anything about Retrofit or data sources.  
- Interacts **only with the domain layer** (`GitHubRepository` interface).

Example: `SearchViewModel`  
```kotlin
class SearchViewModel @HiltViewModel constructor(
    private val repository: GitHubRepository
) : ViewModel() {

    private val _searchResults = MutableStateFlow<List<Repository>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    fun searchRepo(query: String) {
        viewModelScope.launch {
            _searchResults.value = repository.searchRepositories(query)
        }
    }
}
```

---

### **2️⃣ Domain Layer (Business Logic)**  
Contains:  
✔ Models  
✔ Repository interfaces  
✔ Business rules  

Example: `Repository.kt`  
```kotlin
data class Repository(
    val id: Int,
    val name: String,
    val description: String?,
    val owner: String,
    val stars: Int
)
```

---

### **3️⃣ Data Layer (API + Local + Repository Implementation)**  
Implements the domain repository interface.

#### **Remote (Retrofit API)**
```kotlin
interface GitHubApi {
    @GET("search/repositories")
    suspend fun searchRepositories(@Query("q") query: String): SearchResponse
}
```

#### **Local (FavoritesLocalDataSource)**
Stores favorites in memory (can be upgraded to Room easily).

#### **Repository Implementation**
```kotlin
class GitHubRepositoryImpl @Inject constructor(
    private val api: GitHubApi,
    private val localDataSource: FavoritesLocalDataSource
): GitHubRepository {

    override suspend fun searchRepositories(query: String): List<Repository> {
        return api.searchRepositories(query).items.map { it.toDomain() }
    }

    override fun addToFavorites(repo: Repository) = localDataSource.add(repo)
    override fun getFavorites() = localDataSource.getFavorites()
}
```

---

## 🔧 Dependency Injection (Hilt)

### `NetworkModule.kt`
Provides Retrofit, OkHttp, and API instance.

### `AppModule.kt`
Provides repository + local data source.

---

## 🎨 UI (Jetpack Compose)

Screens:  
- `SearchScreen.kt`  
- `FavoritesScreen.kt`  
- `DetailScreen.kt`  

Example UI snippet:
```kotlin
LazyColumn {
    items(searchResults) { repo ->
        RepoItem(
            repo = repo,
            onClick = { onRepoClick(repo) }
        )
    }
}
```

---

## 🌐 Network Monitoring  
`NetworkMonitor` emits real-time connectivity updates.

---

## 📦 How Data Flows (Clean Architecture)

```
UI → ViewModel → Repository Interface → Repository Impl → API/Local
UI ← StateFlow ← ViewModel
```

Completely decoupled and testable.

---

## 🛠 Tech Stack

| Component | Library |
|----------|---------|
| UI | Jetpack Compose |
| DI | Hilt |
| Networking | Retrofit + OkHttp |
| Async | Coroutines + Flow |
| Architecture | MVVM + Clean Architecture |
| Networking Monitor | ConnectivityManager |

---

## 📘 Setup Instructions

### Prerequisites

  * **Android Studio Dolphin (2021.3.1)** or later.
  * **Kotlin 1.7.0** or later.
  * Minimum SDK: 24, Target SDK: 35.

### How to Run the App

1.  **Clone the Repository:**
    ```bash
    git clone [repo-link]
    ```
2.  **Open in Android Studio:** Open the cloned directory as an existing Android Studio project.
3.  **Build & Run:** Select a device/emulator and click the 'Run' button (the green triangle) in the toolbar.

### Testing Environment

The application was primarily tested on the following setup:

  * **Device/Emulator:** Pixel 5 (API 33)
  * **OS Version:** Android 13.0

-----

## ⚠️ Known Issues and Limitations

  * **Local Storage:** The local favorites feature currently uses a simple mechanism (in-memory or SharedPreferences for simplicity). For a production app, this should be upgraded to **Room Persistence Library** for robust database management.
  * **Pagination:** The current search implementation fetches the first page of results. It does not yet support loading subsequent pages (pagination) for large search queries.
