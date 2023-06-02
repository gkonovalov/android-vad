## Android Voice Activity Detection (VAD)
Android [VAD](https://en.wikipedia.org/wiki/Voice_activity_detection) library is designed to process audio in 
real-time and identify presence of human speech in an audio samples that contain a mixture of speech 
and noise. The VAD functionality operates offline, performing all processing tasks directly on the mobile device.

<p align="center">
<img src="https://raw.githubusercontent.com/gkonovalov/android-vad/master/demo.gif" alt="drawing" height="400"/>
</p>

The library offers two distinct models for voice activity detection:

[Silero VAD](https://github.com/snakers4/silero-vad) [[1]](#1) is based on a Deep Neural Networks 
[(DNN)](https://en.wikipedia.org/wiki/Deep_learning) and utilizes the 
[ONNX Runtime Mobile](https://onnxruntime.ai/docs/install/#install-on-web-and-mobile) for execution. 
It provides exceptional accuracy and achieves processing time that are very close to WebRTC VAD.

[WebRTC VAD](https://chromium.googlesource.com/external/webrtc/+/branch-heads/43/webrtc/common_audio/vad/) [[2]](#2)
is based on a Gaussian Mixture Model [(GMM)](http://en.wikipedia.org/wiki/Mixture_model#Gaussian_mixture_model)
which is known for its exceptional speed and effectiveness in distinguishing between noise and silence.
However, it may show relatively lower accuracy when it comes to differentiating speech from background noise.

<p align="center">
  <img src="https://raw.githubusercontent.com/gkonovalov/android-vad/master/comparison.png" />
</p>

If your priority is higher accuracy, I recommend using Silero VAD DNN. For more detailed insights 
and a comprehensive comparison between DNN and GMM, refer to the following comparison 
[Silero VAD vs WebRTC VAD](https://github.com/snakers4/silero-vad/wiki/Quality-Metrics#vs-other-available-solutions).

## Parameters
VAD library only accepts 16-bit mono PCM audio stream and can work with next Sample Rates, 
Frame Sizes and Classifiers.

### Silero VAD
<table>
<tr>
<td>

| Valid Sample Rate |      Valid Frame Size      |
|:-----------------:|:--------------------------:|
|      8000Hz       |       256, 512, 768        |
|      16000Hz      |      512, 1024, 1536       |
</td>
<td>

| Valid Classifiers |
|:------------------|
| NORMAL            |
| AGGRESSIVE        |
| VERY_AGGRESSIVE   |
</td>
</tr>
</table>

### WebRTC VAD
<table>
<tr>
<td>

| Valid Sample Rate | Valid Frame Size |
|:-----------------:|:----------------:|
|      8000Hz       |   80, 160, 240   |
|      16000Hz      |  160, 320, 480   |
|      32000Hz      |  320, 640, 960   |
|      48000Hz      |  480, 960, 1440  |

</td>
<td>

| Valid Classifiers |
|:------------------|
| NORMAL            |
| AGGRESSIVE        |
| VERY_AGGRESSIVE   |
</td>
</tr>
</table>

**Silence duration (ms)** - This parameter is utilized in the Continuous Speech detector. 
It determines the required duration of consecutive negative results to recognize it as silence.

**Speech duration (ms)** - This parameter is used in the Continuous Speech detector. 
It specifies the necessary duration of consecutive positive results to recognize it as speech.

Recommended parameters:
* Model - **SILERO_DNN**,
* Sample Rate - **16KHz**,
* Frame Size - **512**,
* Mode - **VERY_AGGRESSIVE**,
* Silence Duration - **300ms**,
* Speech Duration - **50ms**,
* Android Context - only required for Silero VAD;

## Usage
VAD supports 2 different ways of detecting speech:

1. Continuous Speech listener was designed to detect long utterances
   without returning false positive results when user makes pauses between
   sentences.

```kotlin
    val vad = Vad.builder()
        .setModel(Model.SILERO_DNN)
        .setSampleRate(SampleRate.SAMPLE_RATE_16K)
        .setFrameSize(FrameSize.FRAME_SIZE_512)
        .setMode(Mode.VERY_AGGRESSIVE)
        .setSilenceDurationMs(300)
        .setSpeechDurationMs(50)
        .setContext(applicationContext)
        .build()

    vad.setContinuousSpeechListener(audioData: ShortArray, object : VadListener {
        override fun onSpeechDetected() {
            //speech detected!
        }

        override fun onNoiseDetected() {
            //noise detected!
        }
    })

    vad.close()
```

2. Speech detector was designed to detect speech/noise in short audio
   frames and return result for every frame. This method will not work for
   long utterances.

```kotlin
    val vad = VadBuilder.newBuilder()
        .setModel(Model.WEB_RTC_GMM)
        .setSampleRate(SampleRate.SAMPLE_RATE_16K)
        .setFrameSize(FrameSize.FRAME_SIZE_160)
        .setMode(Mode.VERY_AGGRESSIVE)
        .build()

    val isSpeech = vad.isSpeech(audioData: ShortArray)

    vad.close()
```
## Requirements
Android VAD supports Android 5.0 (Lollipop) and later.

## Dependencies
The library utilizes the ONNX runtime to run Silero VAD DNN, which requires the addition of 
necessary dependencies.

```groovy
dependencies {
   implementation 'com.microsoft.onnxruntime:onnxruntime-android:1.15.0'
}
```


## Development

To open the project in Android Studio:

1. Go to *File* menu or the *Welcome Screen*
2. Click on *Open...*
3. Navigate to VAD's root directory.
4. Select `setting.gradle`

## Download
[![](https://jitpack.io/v/gkonovalov/android-vad.svg)](https://jitpack.io/#gkonovalov/android-vad)


Gradle is the only supported build configuration, so just add the dependency to your project `build.gradle` file:
1. Add it in your root build.gradle at the end of repositories:
```groovy
allprojects {
   repositories {
     maven { url 'https://jitpack.io' }
   }
}
```

2. Add the dependency
```groovy
dependencies {
    implementation 'com.github.gkonovalov:android-vad:2.0.0'
}
```
You also can download precompiled AAR library and APK files from 
GitHub's [releases page](https://github.com/gkonovalov/android-vad/releases).

## References

<a id="1">[1]</a>
[Silero VAD](https://github.com/snakers4/silero-vad) - pre-trained enterprise-grade Voice Activity Detector,
Number Detector and Language Classifier <a href="mailto:hello@silero.ai">hello@silero.ai</a>.

<a id="2">[2]</a>
[WebRTC VAD](https://chromium.googlesource.com/external/webrtc/+/branch-heads/43/webrtc/common_audio/vad/) -
Voice Activity Detector from Google which is reportedly one of the best available: it's fast, 
modern and free. This algorithm has found wide adoption and has recently become one of the 
gold-standards for delay-sensitive scenarios like web-based interaction.

------------
Georgiy Konovalov 2023 (c) [MIT License](https://opensource.org/licenses/MIT)