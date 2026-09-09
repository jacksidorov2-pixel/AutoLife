# AutoLife 0.1

Experimental Android Auto-like projection client that speaks the **Baidu CarLife USB accessory protocol** to a compatible head unit.

The initial target is the Chinese Mazda KBST head unit. The car continues to open its stock CarLife mode; the Android phone supplies its own launcher, screen video, playback audio and touch handling.

## What is implemented

- Android 10+ (`minSdk 29`).
- USB Accessory filter: `manufacturer=Baidu`, `model=CarLife`, `version=1.0.0`.
- Clean-room CarLife packet framing and minimal protobuf wire codec.
- CarLife handshake for:
  - HU protocol version;
  - HU/device info;
  - video encoder configuration;
  - video start;
  - statistic/authentication response.
- H.264 screen projection with the resolution/FPS requested by the head unit.
- Full-display MediaProjection on Android 14+ so switching between apps remains visible.
- Playback audio capture: PCM 48 kHz / stereo / 16 bit sent through the CarLife media channel.
- Reverse touch control through Android Accessibility gestures.
- Center-fit/letterbox compensation when converting Mazda touch coordinates to phone coordinates.
- Launcher buttons for:
  - 2GIS: `ru.dublgis.dgismobile`
  - Yandex Music: `ru.yandex.music`

## First car test

1. Build and install the debug APK on the Android phone.
2. Open **AutoLife** once.
3. Tap **Touch-доступ** and enable the AutoLife accessibility service.
4. Connect the phone to the Mazda USB port and open the stock **Baidu CarLife** function on the head unit.
5. Android should ask which app should handle the `Baidu / CarLife / 1.0.0` USB accessory. Choose **AutoLife** and allow USB access.
6. When Mazda reaches `VIDEO_ENCODER_START`, AutoLife automatically asks for screen-capture permission.
7. Grant the screen capture. On Android 14+ AutoLife explicitly requests the whole default display, not a single application.
8. The Mazda screen should begin showing the phone projection. Test a tap in AutoLife, then open 2GIS and Yandex Music from the projected launcher.

Useful log filter:

```bash
adb logcat -s AutoLife
```

The most valuable first log sequence is:

```text
CarLife USB подключён
CarLife: protocol match OK
CarLife: HU info получено
CarLife video: <width>x<height> @ <fps> fps
CarLife запросил запуск проекции
```

If the sequence stops at one of those points, that tells us exactly which Mazda-specific protocol detail to adjust next.

## Build

Project configuration:

- JDK 17
- compileSdk 36
- targetSdk 36
- Android Gradle Plugin 8.13.2
- Gradle 8.13

### Android Studio

Open the `AutoLife` directory as an existing project. If Android Studio asks for a Gradle distribution, select/download Gradle **8.13** and use JDK **17**. Install Android SDK Platform 36 when prompted, then build `app` / `debug`.

### GitHub Actions

A workflow is included at `.github/workflows/build-apk.yml`. Pushing this project to a GitHub repository and starting the **Build AutoLife APK** workflow produces `AutoLife-debug-apk` as an Actions artifact.

## Important Android limitations

- Android requires user consent for each MediaProjection capture session on modern releases; this cannot be silently bypassed by a normal app.
- Playback audio from another app can be captured only if that app's playback-capture policy allows it. Video/touch can still work when audio capture is unavailable.
- Accessibility is used only to inject the touch gestures received from the head unit. AutoLife does **not** request window-content retrieval in its accessibility configuration.
- A normal app cannot force every third-party app into landscape orientation. For the first hardware test, keep phone auto-rotate enabled and the phone in landscape.
- This is an interoperability prototype, not a Google Android Auto implementation and not affiliated with Baidu, Mazda, 2GIS, Yandex or Google.

## Project layout

```text
app/src/main/java/com/autolife/
  MainActivity.java                   launcher + USB permission + capture consent
  AutoLifeApp.java                    process-wide CarLife transport
  AutoLifeAccessibilityService.java   HU touch -> Android gesture
  ProjectionService.java              mediaProjection foreground service

app/src/main/java/com/autolife/core/
  UsbCarLifeTransport.java             CarLife USB state machine
  CarLifeWire.java                     outer/inner packet framing
  ProtoLite.java                       minimal protobuf wire codec
  H264ScreenEncoder.java               MediaProjection -> H.264
  PlaybackAudioCapture.java            Android playback -> PCM
```

## v0.2 after the Mazda test

The first car test should tell us which of these to prioritize:

- exact Mazda hard-key mapping (commander knob / seek / back / home);
- latency and bitrate tuning for the KBST decoder;
- permanent AutoLife home/navigation bar overlay;
- media controls and current-track widget;
- navigation card/next-maneuver widget;
- better screen-off strategy;
- automatic reconnect and session recovery.
