<h1 align="center">SmartConnect</h1>

<p align="center">
  <strong>A Smart School Management App — Connecting Students, Teachers, Parents & Administration</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-brightgreen.svg" alt="Platform: Android">
  <img src="https://img.shields.io/badge/Language-Java-orange.svg" alt="Language: Java">
  <img src="https://img.shields.io/badge/Backend-Firebase-yellow.svg" alt="Backend: Firebase">
  <img src="https://img.shields.io/badge/License-MIT-blue.svg" alt="License: MIT">
</p>

---

## 📑 Table of Contents
*   [About the App](#-about-the-app)
*   [Features by Role](#-features-by-role)
    *   [Admin](#-admin)
    *   [Teacher](#-teacher)
    *   [Student](#-student)
    *   [Parent](#-parent)
    *   [Principal](#-principal)
*   [Security](#-security)
*   [Animations Used](#-animations-used)
*   [Tech Stack](#-tech-stack)
*   [Project Structure](#-project-structure)
*   [Getting Started](#-getting-started)
*   [Screenshots](#-screenshots)
*   [Contributing](#-contributing)
*   [License](#-license)

---

## 🏫 About the App
**SmartConnect** is a comprehensive, multi-role school management ecosystem built for the modern educational landscape. It serves as a centralized bridge between administration, faculty, students, and parents, streamlining everything from daily attendance to complex financial management. Designed with a focus on high-performance synchronization and security, SmartConnect brings the entire school community onto a single, intuitive platform.

---

## 🚀 Features by Role

### 👨‍💼 Admin
*   📊 **Dashboard:** Real-time school-wide statistics and overview.
*   👥 **User Management:** Full control to add or remove students, teachers, parents, and principals.
*   🏫 **Classroom Orchestration:** Create and manage classes, sections, and subjects.
*   💰 **Fee Management:** Assign, track, and generate detailed fee records and installments.
*   📢 **Notices:** Upload and broadcast important notices to all user roles.
*   📅 **Exam Schedules:** Centrally view and manage exam dates and details.
*   ⏰ **Timetable Control:** Manage subject-wise timetables for all classes.
*   💬 **Chat Access:** Full access to administrative chat channels.
*   🛒 **E-commerce:** Manage the school shop, including products and order fulfillment.
*   🔔 **Broadcasting:** Push notification broadcasting to all users.

### 👩‍🏫 Teacher
*   📚 **Assigned Classes:** Instant view of assigned classes and subjects.
*   📝 **Attendance:** Mark and manage student attendance with ease.
*   📈 **Results:** Upload student marks and academic results.
*   📅 **Class Timetable:** View and manage specific class schedules.
*   📢 **Class Notices:** Post targeted notices directly to students and parents.
*   💬 **Communication:** Direct chat access with students and parents.
*   📝 **Exam Portal:** View upcoming exam schedules.
*   🔔 **Notifications:** Receive real-time push notifications from the administration.

### 👨‍🎓 Student
*   📅 **Attendance Tracking:** View personal daily attendance records.
*   📊 **Results:** Check exam marks and academic performance.
*   ⏰ **Schedules:** Access class timetables and exam schedules instantly.
*   📢 **Bulletin Board:** Read the latest notices from admin and teachers.
*   � **Teacher Chat:** Direct messaging support with teachers.
*   👤 **Profile:** View and update personal profile details.
*   🛍️ **School Shop:** Browse and purchase school supplies via e-commerce.
*   🔔 **Alerts:** Stay updated with real-time push notifications.

### 👪 Parent
*   👶 **Child Monitoring:** View child's real-time attendance and behavior.
*   📉 **Progress Reports:** Check child's exam results and academic standing.
*   📅 **Stay Informed:** View school timetables and exam schedules.
*   📢 **Notices:** Receive and read important school announcements.
*   💬 **Staff Contact:** Direct chat channel with their child's teachers.
*   👤 **Child's Profile:** View and verify their child's school profile.
*   🔔 **Activity Alerts:** Receive notifications about their child's school activities.

### � Principal
*   📊 **Reports:** Access comprehensive school-wide reports and deep statistics.
*   📋 **Records:** View all teacher, student, and parent directories.
*   🔍 **Monitoring:** Oversee attendance and results across all classes and streams.
*   📢 **Leadership:** View, manage, and post important institutional notices.
*   📅 **Global Schedule:** Oversight of all exam and timetable schedules.
*   🔔 **Notifications:** Receive all push notifications sent across the platform.
*   💬 **Direct Chat:** Full chat access for institutional coordination.

---

## 🔒 Security
SmartConnect employs industry-standard security protocols to ensure data integrity and user privacy.

*   **Firebase Authentication:** Secure email/password login with a secondary layer of protection.
*   **2-Step Verification (OTP):** Custom SMTP-based OTP generation for high-security login verification.
*   **Role-Based Access Control (RBAC):** Strict programmatic enforcement ensures users only access data relevant to their specific role.
*   **Firestore Security Rules:** Server-side rules enforce granular read/write permissions at the database level.
*   **Secure Storage:** Firebase Storage rules restrict file access (like profile photos) to authorized users only.
*   **Data Privacy:** Sensitive user data is never stored locally in plain text; all sessions are managed via secure Firebase Auth tokens.
*   **FCM Management:** Push notification tokens are managed server-side to prevent unauthorized broadcasting.
*   **Clean Source Code:** Zero hardcoded credentials; all configurations are managed via `google-services.json`.

---

## ✨ Animations Used
The app features a professional, fluid UI powered by native Android animation frameworks.

*   **Activity Transitions:** Smooth slide-in/out effects using `overridePendingTransition` for seamless navigation.
*   **Fade Animations:** Elegant fade-in/out transitions using XML resources (`res/anim/`).
*   **RecyclerView Dynamics:** Native `ItemAnimator` for smooth list scrolling and item updates.
*   **ObjectAnimator:** Precise button press effects, view state transitions, and pulsing logo effects.
*   **Bouncy Scale:** Overshoot interpolators for dashboard cards and interactive profile elements.
*   **Staggered Entry:** Sequential loading of dashboard items to provide a "premium" feel.
*   **Progressive Loading:** Custom `ProgressBar` states and looping alpha animations for background tasks (like OTP sending).

---

## 🛠️ Tech Stack

| Technology | Purpose |
| :--- | :--- |
| **Java** | Primary programming language for native Android development. |
| **XML** | Layout design and animation resource definitions. |
| **Firebase Auth** | Secure user authentication and session management. |
| **Realtime DB / Firestore** | Sub-second data synchronization and role-based storage. |
| **Firebase Storage** | Cloud storage for profile images and school documents. |
| **FCM** | Push notifications for real-time alerts and broadcasts. |
| **Android Studio** | Official IDE for development and debugging. |
| **Gradle** | Dependency management and automated build system. |

---

## � Project Structure
```text
SmartConnect/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/schoolmanagement/
│   │   │   │   ├── activities/    # Dashboard and Feature screens
│   │   │   │   ├── fragments/     # Tabbed interfaces and reusable UI
│   │   │   │   ├── adapters/      # RecyclerView and Spinner adapters
│   │   │   │   ├── models/        # Data classes (Student, Teacher, etc.)
│   │   │   │   └── utils/         # UIAnimator and GmailSender helpers
│   │   │   └── res/
│   │   │       ├── anim/          # Slide, Fade, and Pulse XMLs
│   │   │       ├── drawable/      # Icons, Gradients, and Logos
│   │   │       ├── layout/        # Activity and Item XML layouts
│   │   │       └── values/        # Colors, Styles, and Strings
└── build.gradle                   # Project-level configuration
```

---

## 🏁 Getting Started

### Prerequisites
*   **Android Studio:** Ladybug (2024.2.1) or higher.
*   **Java:** JDK 17 or higher.
*   **Firebase Account:** To set up the backend services.

### Installation
1.  **Clone the Repo:**
    ```bash
    git clone https://github.com/yourusername/smartconnect.git
    ```
2.  **Add Configuration:**
    *   Download your `google-services.json` from the Firebase Console.
    *   Place it in the `app/` directory.
3.  **Setup Gmail SMTP:**
    *   Configure your Gmail App Password in `GmailSender.java` for OTP services.
4.  **Build & Run:**
    *   Sync Gradle and run the application on your physical device or emulator.

---

## 📸 Screenshots
_(Coming soon — add screenshots here)_

---

## 🤝 Contributing
We welcome contributions to make SmartConnect even better!
1.  Fork the project.
2.  Create your Feature Branch (`git checkout -b feature/AmazingFeature`).
3.  Commit your changes (`git commit -m 'Add some AmazingFeature'`).
4.  Push to the Branch (`git push origin feature/AmazingFeature`).
5.  Open a Pull Request.

---

## 📜 License
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

Distributed under the MIT License. See `LICENSE` for more information.

---
*Developed with ❤️ as a Final Year Project.*
