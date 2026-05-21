# Personal Expense Tracker

A privacy-focused Android app that automatically captures bank transactions from SMS, categorizes them intelligently, and backs them up to Google Drive — all stored locally on your device.

## Features

### Automatic Transaction Capture
- Listens for incoming bank SMS and parses transaction details
- Detects amounts, payment method (UPI, card, etc.), and direction (debit/credit)
- Smart filtering keeps out OTPs and non-transaction messages

### Intelligent Categorization
- Auto-categorizes transactions based on merchant and contextual hints
- Default categories: Groceries, Food, Transport, Utilities, Entertainment, Healthcare, Transfers, Income, Uncategorized
- Fully customizable categories, merchants, and payment channels

### Account Management
- Track multiple bank accounts and cards
- Auto-links transactions to the correct account via card suffixes

### Charts & Summary
- Visual spending breakdown with pie charts
- Filter by account or view all transactions at once

### Backup & Restore
- Local backup saved to device storage
- Export/Import via JSON files
- **Google Drive sync** — sign in with Google to backup and restore across devices

## Screenshots

| Transactions | Summary | Accounts |
|-------------|---------|----------|
| ![Transactions](docs/transactions.png) | ![Summary](docs/summary.png) | ![Accounts](docs/accounts.png) |

## Tech Stack

| | |
|---|---|
| **Language** | Kotlin |
| **UI** | Jetpack Compose + Material 3 |
| **Architecture** | MVVM with ViewModel |
| **Database** | Room (SQLite) |
| **Navigation** | Jetpack Navigation Compose |
| **Charts** | MPAndroidChart |
| **Backup** | Google Drive API |
| **Min SDK** | 24 (Android 7.0) |
| **Target SDK** | 35 |

## Project Structure

```
app/src/main/java/com/hktech/personalexpensetracker/
├── MainActivity.kt              # Entry point, permissions gate
├── data/                        # Room database, entities, DAOs
├── ingest/                      # SMS receiver and transaction parser
├── backup/                      # Local and Google Drive backup
└── ui/                          # Compose screens, ViewModel, theming
```

## Building

```bash
./gradlew assembleDebug        # Debug APK
./gradlew assembleRelease     # Release APK (requires signing config)
```

## Permissions

| Permission | Purpose |
|-----------|---------|
| `RECEIVE_SMS` | Catch incoming SMS broadcasts |
| `READ_SMS` | Read SMS content for transaction parsing |
| `INTERNET` | Google Drive backup |
| `ACCESS_NETWORK_STATE` | Connectivity checks |

## License

MIT License