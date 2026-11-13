# 🎮 PS5 PKG Merger

A modern, cross-platform GUI tool built with **Kotlin** and **Jetpack Compose** to seamlessly join split PlayStation PKG files (**PS3, PS4, PS5**) with built-in **SHA-256** integrity verification.

---

## ✨ Features

* **Cross-Platform Support:** Available for both **Android** and **Windows Desktop**.
* **Fast & Reliable Merging:** Recombines multi-part `.pkg` archives (`_0.pkg`, `_1.pkg`, etc.) into a single complete package.
* **SHA-256 Verification:** Automatically verifies file integrity before and after merging to prevent corrupted installations.
* **Clean Material UI:** Intuitive and lightweight graphical interface.

---

## 📥 Installation

1. Go to the [**Releases**](https://github.com/Ridagames/PS-PKG-MERGER/releases) section on the right side of this page.
2. Download the latest version for your platform:
   * **Android:** Download the `.apk` file and install it on your device.
   * **Windows (PC):** Download the Windows release package.

---

## 🛠️ How to Use

1. Launch **PS5 PKG Merger**.
2. Select the folder or the first part (`_0.pkg`) of your split PKG files.
3. Choose your desired output destination.
4. Click **Merge** and wait for the process and SHA-256 hash check to complete.

---

## 💻 Tech Stack

* **Language:** 100% Kotlin
* **UI Framework:** Jetpack Compose / Compose Multiplatform
* **Build System:** Gradle (Kotlin DSL) & GitHub Actions CI/CD
