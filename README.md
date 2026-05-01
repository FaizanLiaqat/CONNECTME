# ConnectMe — Social Networking Android App

ConnectMe is a full-stack social networking Android application where users can share moments and connect with others.

---

## Features

- 📸 Post and share images
- 🔍 Search, follow, and unfollow users
- ⏱️ Instagram-style image stories with a time limit
- ❤️ Like and comment on posts
- 💬 Real-time chat with other users

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Mobile | Android (Kotlin, Jetpack Compose) |
| Architecture | MVVM + Repository Pattern |
| Backend | PHP REST API |
| Database | MySQL |
| Auth | JWT Authentication |
| Local DB | Room Database |

---

## Screenshots

> Coming soon

---

## Getting Started

### Prerequisites
- Android Studio
- XAMPP (for local backend)
- Android device or emulator (API 24+)

### Setup

1. Clone the repository
   ```bash
   git clone https://github.com/FaizanLiaqat/ConnectMe.git
   ```

2. Set up the backend
   - Import the provided SQL file into your MySQL database via phpMyAdmin
   - Place the PHP files in your XAMPP `htdocs` folder
   - Start Apache and MySQL from XAMPP

3. Configure the base URL
   - Open the project in Android Studio
   - Update the base URL in the network config file to point to your local server

4. Run the app on your device or emulator

---

## Architecture

ConnectMe follows the MVVM (Model-View-ViewModel) architecture pattern with a Repository layer, ensuring a clean separation of concerns and a maintainable codebase.

---

## Author

**Faizan Liaqat**
- GitHub: [@FaizanLiaqat](https://github.com/FaizanLiaqat)
- LinkedIn: [faizan-liaqat](https://www.linkedin.com/in/faizan-liaqat-b9881628a/)
