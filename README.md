# [AllTalk](https://github.com/erew123/alltalk_tts) Android Client

Simple Android client for TTS audio generation using a local [AllTalk](https://github.com/erew123/alltalk_tts) server.
Connects to a self-hosted AllTalk server to generate AI voices without relying on cloud APIs.
Ability to search and import novels from online sources.
Download individual chapters or batch download for offline reading.

## Tech Stack

Kotlin, Jetpack Compose, MVVM, Retrofit, OkHttp, Jsoup, Coil.

## Configuration

Enter your server IP in the settings menu.

This project uses a Factory Pattern to decouple the app from specific web scraping logic. This allows the core app to be open-sourced while keeping specific site scrapers private.
The actual scraper is excluded from this repository.



