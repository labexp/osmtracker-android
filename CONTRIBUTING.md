
# 🤝 Contributing to OSMTracker

Thank you for your interest in contributing! 🎉  
Whether you're fixing a bug, adding a new feature, or improving documentation — all contributions are welcome.

---
## 📚 Table of Contents
1. 🔰 Before You Start
2. 🛠️ How to Run the App (Setup Guide)
3. 🧪 Running Tests
4. 🌍 Translations
5. 🧑‍💻 Git Workflow (GitFlow)
6. 👶 New Contributor Quick Guide (For Beginners)
7. 📜 Code Style & Commit Guidelines
8. 🚀 How to Submit a Pull Request
9. 💬 Community & Support

---

## 🔰 1. Before You Start
- Make sure you have **Git** and **Android Studio** installed.
- Familiarity with **GitFlow** is recommended (see below).
- If you're new to open/free source, check out our **beginner section** ↓

---

## 🛠️ 2. How to Run the App Locally

```bash
# Clone the repository
git clone https://github.com/labexp/osmtracker-android/
cd osmtracker-android
```

1. Install Android Studio. - [Here](https://developer.android.com/studio/install) is the official guide on how to install it on different operating systems
2. Open the IDE and click `open an existing Android Studio project`, then look for the folder where you cloned the repository  
3. Build the project (`Ctrl + F9` or the 🛠️ hammer button)



## 🧪 3. Running Tests

This repository has an automated way to run the tests on branches but if you already have the project installed on you computer then you can also run them from a terminal.
It's recommended to run the tests locally before making a new pull request to make sure the changes doesn't break any previous functionality. You can run the tests locally as follows:
 - Make sure you are at the *root directory of the project*
	 - `$ cd YOUR_PATH/osmtracker-android`

### 📱 Instrumentation Tests (Require Emulator or Device)
 - For running **instrumentation** tests it's needed to previously start up an emulator (or real device),  you can do it from Android Studio but also without it using the command line. For that,  you need to move to the Android SDK installation directory and look for a folder called `emulator` once there, start any already created emulator by typing:
	-	`$ ./emulator -avd NAME` to start the emulator called *NAME* (run `$ ./emulator -list-avds` for a valid list of AVD names)
	- When  it's up, go back to the root project folder and run the instrumentation tests with
	- `$ ./gradlew connectedAndroidTest`

### ✅ Unit Tests
 - For running the **unit tests** no emulator or device is needed, just run 
	 - `$ ./gradlew test`
 - Now just wait for gradle to run the tests for you, it'll show the results of which tests passed or failed when it's finished


## 🌍 4. Translations
OSMTracker is translated using Transifex (see the [wiki](https://github.com/labexp/osmtracker-android/wiki/Translating)).
Once translations are complete, they will be updated via automated Transifex PR.


## 🧑‍💻 5. Git Workflow (GitFlow)

We use **GitFlow** branching model:

| Branch | Purpose |
|--------|----------|
| `master` | Stable production releases |
| `develop` | Main development branch |
| `feature/*` | New features |
| `hotfix/*` | Quick fixes for production |

- If you want more information, take a look [here](https://www.atlassian.com/git/tutorials/comparing-workflows/gitflow-workflow)
---

## 👶 6. New Contributor Quick Guide (Beginner-Friendly)

> ✨ If this is your first open-source contribution, start here!

1. ⭐ **Fork the repository**

   After forking, make sure your fork includes **all the required branches** (especially `develop`, not only `master`).

   You can verify this by checking the branches tab in your fork on GitHub.

2. 📥 **Clone your fork**
   ```bash
   git clone https://github.com/YOUR_USERNAME/osmtracker-android.git
   ```
3. 🔄 (Optional) Add upstream to stay updated  
   ```bash
   git remote add upstream https://github.com/labexp/osmtracker-android.git
   ```
4. 🌱 Create a branch  
   ```bash
   git checkout develop
   git checkout -b feature/your-feature-name
   ```
5. ✍️ Make changes + commit (atomic commits)
6. 🚀 Push your branch
7. 📝 Open a Pull Request (PR)

---

## 📜 7. Commit Message Convention
Use **clear and descriptive** commit messages.

✅ Good:
```
feat: add option to export GPX file
fix: resolve crash when no GPS signal
docs: update contributing guide
```
❌ Bad:
```
update stuff
fix bug
```

---
## 🚀 8. How to Submit a Pull Request
1. Push your changes
2. Go to GitHub → Open PR
3. Fill the PR template fully
4. Wait for review ✅

⚠️ PRs without a complete template may be rejected.

---

## 💬 9. Community & Support
- Have questions? Open a **Discussion** or an **Issue**
- (Optional): Join the contributors chat/[Telegram](https://t.me/OSMTracker).

Let's build something awesome together! 🚀