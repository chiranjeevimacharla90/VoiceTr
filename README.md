# VoiceTr

A browser-based voice translator for **English, Hindi, and Tamil**.

## Features

- 🎙 Voice input using the browser Speech Recognition API
- 🌐 Translation between English, Hindi, and Tamil
- 🔊 Text-to-speech playback in the selected target language
- 🔄 One-click language swap
- 📋 Copy translation to clipboard
- 📱 Responsive interface for desktop and mobile
- No application server required

## Run locally

Open `index.html` in a modern browser, or serve the folder with a simple static server:

```bash
python -m http.server 8000
```

Then open `http://localhost:8000`.

## Browser notes

Voice input availability depends on browser/device support for `SpeechRecognition` / `webkitSpeechRecognition`. HTTPS or localhost may be required for microphone permissions.

Translation currently uses the public MyMemory translation endpoint, so an internet connection is required for translation.

## GitHub Pages

This project is a single static HTML application and can be published directly with GitHub Pages from the repository's `main` branch.
