# SI-TIRTA

![SI-TIRTA feature graphic](https://raw.githubusercontent.com/khrlanamm/khrlanamm/main/assets/projects/si-tirta.webp)

Water billing and bookkeeping app for a village water utility (KPSPAM) in West Sumatra. Used for the June 2025 payments and taught to 10 utility officers.

## Features

- **Dashboard**: customers grouped by the officer in charge (penanggung jawab), with billing and payment progress for the month.
- **Penagihan (billing)**: search customers, filter by officer and status, and mark bills as billed (Sudah Ditagih) or paid (Sudah Bayar).
- **Cetak (print)**: print bills and receipts on a Bluetooth ESC/POS thermal printer.
- **Pembukuan (bookkeeping)**: debit and credit entries per month. Only treasurers listed in the `bendahara` collection can open it.
- Google sign-in with Credential Manager.

## Data and access

Data lives in Firestore: `pelanggan` (customers), `penanggung_jawab` (officers), `transaksi` (bookkeeping), and `bendahara` (treasurer emails). The security rules in [`firestore.rules`](firestore.rules) allow signed-in users only, and new sign-ups are disabled in Firebase Authentication, so only existing officer accounts can sign in. A user can read only their own `bendahara` entry.

The Firebase config (`app/google-services.json`) is committed. It is not a secret: access is controlled by the rules and the sign-up setting above.

## Stack

Kotlin, XML layouts, Navigation component, ViewModel and LiveData, Firebase Authentication and Firestore, Credential Manager, ESC/POS thermal printer library.

## Build

```sh
./gradlew :app:assembleDebug
```

Release builds are signed in Android Studio with the upload key, which is kept outside the repo.

Deploy the security rules with the Firebase CLI:

```sh
firebase deploy --only firestore:rules
```

## License

All rights reserved. The source code is public for reference only. See [LICENSE](LICENSE).
