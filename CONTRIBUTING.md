
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

This repository has automated tests that run on CI for every branch, but it's highly recommended to run them locally before submitting a pull request to ensure your changes don't break existing functionality.

For **detailed instructions** covering unit tests, instrumented tests, coverage reports, and troubleshooting, please refer to:

👉 **[docs/TESTING.md](docs/TESTING.md)** 👈


## 🌍 4. Translations

OSMTracker is translated using **[Transifex](https://www.transifex.com/)**, a localization platform that makes it easy for contributors to translate the app into their native language.

**How to help translate**

1. Go to the **[OSMTracker for Android™ project on Transifex](https://explore.transifex.com/labexp/osmtracker-android/)**
2. Create a free Transifex account (if you don't have one)
3. Select your language from the list
4. Start translating strings

Once translations are complete on Transifex, they will be automatically synced to the repository via an automated Transifex Pull Request. This integration is configured using the [transifex.yml](transifex.yml) file in the repository root. For technical details about how the GitHub integration works, see the [Transifex GitHub Integration Guide](https://help.transifex.com/en/articles/6265125-github-installation-and-configuration).


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