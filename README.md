# Calendest

Calendest is a Kotlin Android calendar application built with Jetpack Compose and the Google Calendar API. The app allows users to authenticate with Google, manage calendar events, configure recurring schedules and notifications, and locally store application data using Room.

## Features

- Google Calendar OAuth authentication
- View Google Calendar events
- Create, edit, and delete events
- Support for recurring events
- Custom recurrence builder
- All-day and timed events
- Event notifications and reminders
- Push notification support with Firebase Cloud Messaging
- Local Room database caching
- Pull-to-refresh event synchronization
- Event search functionality
- Multi-account switching support
- Snag reporting and local issue tracking
- Offline persistence of calendar data
- Modern Jetpack Compose UI

## Tech Stack

### Android
- Kotlin
- Jetpack Compose
- Android ViewModel
- StateFlow
- Material 3

### Networking
- Retrofit
- Gson

### Authentication
- Google Identity Services
- OAuth 2.0
- Credential Manager

### Local Storage
- Room Database

### Notifications
- Firebase Cloud Messaging
- AlarmManager
- Notification Channels

## Architecture

Calendest follows a layered architecture with:
- UI Layer
- ViewModel Layer
- Repository Layer
- Network Layer
- Local Database Layer

The app also incorporates an MVI-inspired state management approach using:
- `EventsState`
- `EventsAction`
- `EventsReducer`
- `UiState`

## Project Structure

```text
app/
├── auth/
├── data/
│   ├── local/
│   ├── model/
│   ├── mvi/
│   ├── network/
│   └── repository/
├── navigation/
├── notifications/
├── ui/
│   └── screens/
└── MainActivity.kt
```

## Setup Instructions

### 1. Clone the Repository

```bash
git clone https://github.com/danielsinensky/Calendest.git
cd Calendest
```

### 2. Create `secrets.properties`

Create a file named:

```text
secrets.properties
```

in the project root.

Add:

```properties
GOOGLE_WEB_CLIENT_ID=your_google_web_client_id
SUPPORT_EMAIL=your_email@example.com
```

This file is ignored by Git and should never be committed.

### 3. Firebase Configuration

Add your Firebase configuration file:

```text
app/google-services.json
```

This file is ignored by Git and should not be committed publicly.

### 4. Configure Google Cloud OAuth

Create:
- Android OAuth Client
- Web OAuth Client

Enable:
- Google Calendar API

Add the Android package name and SHA-1 fingerprint in Google Cloud Console.

### 5. Build the App

Open the project in Android Studio and run:

```bash
./gradlew build
```

or use the Android Studio Run button.

## Permissions Used

Calendest uses the following Android permissions:

- Internet access
- Notifications
- Exact alarms
- Google account access

## Notifications

Calendest supports:
- Local scheduled event reminders
- Push notifications via Firebase Cloud Messaging
- Custom reminder timing
- Email and popup reminder configurations

## Security

Sensitive configuration values are excluded from source control using:
- `.gitignore`
- `secrets.properties`

The repository does not include:
- OAuth secrets
- Firebase service credentials
- Signing keys
- Access tokens

## Future Improvements

- Calendar widget support
- Wear OS integration
- Shared calendars
- Event attachments
- Cloud backup for snag reports
- Improved offline synchronization
- Material You dynamic theming
- Calendar import/export

## License

This project is provided for educational and portfolio purposes.

## Author

Daniel Sinensky
