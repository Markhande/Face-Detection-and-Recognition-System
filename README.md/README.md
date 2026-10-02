# Face Detection and Recognition System

Android app for **live face detection** and **on-device face recognition**. Built in Kotlin with CameraX, MediaPipe / ML Kit, TensorFlow Lite (FaceNet), and Room.

> Previously published under the working name **Rampage**.

## What it does

The app finds faces in the camera feed, builds a compact face embedding for each face, and matches that embedding against people already saved on the device. Matches are stored with name, timestamp, and an optional face image path so you can review who was recognized and when.

Typical flow:

1. **Splash** — app launch and setup.
2. **Dashboard** — entry point into detection / recognition workflows.
3. **Main / Check Face** — live camera capture, detect faces, compare embeddings, and show recognition results.

## Features

- Real-time face detection from the device camera
- Face recognition with FaceNet embeddings and cosine similarity
- Local enrollment of known faces (name + embedding + image path)
- Persistent history via Room (student / person records with date and time)
- Fully on-device processing — no cloud face API required for the core path

## Tech stack

| Area | Libraries |
| --- | --- |
| Language | Kotlin |
| UI / camera | Android + CameraX |
| Detection | MediaPipe / ML Kit Face Detection |
| Recognition | TensorFlow Lite FaceNet embeddings + cosine similarity |
| Storage | Room (names, timestamps, embeddings, image paths) |
| Build | Gradle (Kotlin DSL) |

## Project structure

```
app/                 # Android application module
gradle/              # Gradle wrapper and version catalog
build.gradle.kts     # Root build config
settings.gradle.kts  # Project settings
```

Key screens under `app/` include Splash, Dashboard, MainActivity, and CheckFace.

## How recognition works

1. CameraX delivers frames from the live preview.
2. The face detector locates face bounding boxes in each frame.
3. Face crops are passed through a FaceNet TensorFlow Lite model to produce an embedding vector.
4. That vector is compared to stored embeddings with **cosine similarity**.
5. If similarity is above the decision threshold, the person is treated as recognized and a Room record is written (name, date/time, embedding, image path).

## Getting started

### Requirements

- Android Studio (recent stable)
- Android SDK with a device or emulator that has a camera
- JDK compatible with the project's Gradle toolchain

### Run

```bash
./gradlew :app:assembleDebug
```

Or open the project in Android Studio and run the `app` configuration on a connected device / emulator.

Grant camera (and any storage) permissions when prompted so live detection can start.

## Notes

- Face embeddings and recognition history stay on the device in Room.
- Tune the cosine-similarity threshold carefully: too low increases false matches; too high misses true matches.
- This README describes the current architecture as implemented in the repo (Kotlin Android app with MediaPipe/ML Kit, FaceNet TFLite, CameraX, and Room).

## Author

[Markhande](https://github.com/Markhande)
