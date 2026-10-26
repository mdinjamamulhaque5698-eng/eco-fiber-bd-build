ECO FIBER BD — Android Studio project
=====================================

Included features:
- Offline SQLite storage on one Android phone
- Seeded accounts: Sonali, Pubali, City Bank, Agrani, Hand Cash, Vault, Master Wallet
- Add additional accounts
- Deposit/add and withdrawal entries with notes
- Transfers between accounts
- Transaction history
- Torn/damaged note counts by denomination
- Bengali-first interface

Important limitations:
- This ZIP is the Android Studio source project, not a compiled APK.
- Online/cloud backup, PIN lock, bilingual language switching, and PDF/Excel export are not implemented in this first starter version.
- No bank credentials or actual bank connections are used.
- Check all balances and transactions carefully before relying on the app for business records.

Build APK:
1. Install Android Studio on a computer.
2. Open this project folder in Android Studio.
3. Let Gradle sync and install the required Android SDK (compile SDK 35).
4. Choose Build > Build Bundle(s) / APK(s) > Build APK(s).
5. Android Studio will show the APK location, typically app/build/outputs/apk/debug/app-debug.apk.

For a release APK, configure signing in Android Studio before distribution.
