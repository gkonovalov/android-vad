## Android Voice Activity Detection (VAD)
Android [VAD](https://en.wikipedia.org/wiki/Voice_activity_detection) library is designed to process audio in 
real-time and identify presence of human speech in audio samples that contain a mixture of speech 
and noise. The VAD functionality operates offline, performing all processing tasks directly on the mobile device.

The repository offers three distinct models for voice activity detection:

[WebRTC VAD](https://chromium.googlesource.com/external/webrtc/+/branch-heads/43/webrtc/common_audio/vad/) [[1]](#1)
is based on a Gaussian Mixture Model [(GMM)](http://en.wikipedia.org/wiki/Mixture_model#Gaussian_mixture_model)
which is known for its exceptional speed and effectiveness in distinguishing between noise and silence.
However, it may demonstrate relatively lower accuracy when it comes to differentiating speech from background noise.

[Silero VAD](https://github.com/snakers4/silero-vad) [[2]](#2) is based on a Deep Neural Networks 
[(DNN)](https://en.wikipedia.org/wiki/Deep_learning) and utilizes the 
[ONNX Runtime Mobile](https://onnxruntime.ai/docs/install/#install-on-web-and-mobile) for execution. 
It provides exceptional accuracy and achieves processing time that is very close to WebRTC VAD.

[Yamnet VAD](https://github.com/tensorflow/models/tree/master/research/audioset/yamnet) [[3]](#3) is based on a Deep Neural Networks
[(DNN)](https://en.wikipedia.org/wiki/Deep_learning) and employs the Mobilenet_v1 depthwise-separable 
convolution architecture. For execution utilizes the [Tensorflow Lite](https://www.tensorflow.org/lite/android) runtime.
Yamnet VAD can predict [521](https://github.com/tensorflow/models/blob/master/research/audioset/yamnet/yamnet_class_map.csv)
audio event classes (such as speech, music, animal sounds and etc).
It was trained on [AudioSet-YouTube](https://research.google.com/audioset/) corpus.

For higher accuracy, I recommend to use Silero VAD DNN or Yamnet VAD DNN. 
For more detailed insights and a comprehensive comparison between DNN and GMM, refer to the following comparison 
[Silero VAD vs WebRTC VAD](https://github.com/snakers4/silero-vad/wiki/Quality-Metrics#vs-other-available-solutions).

<p align="center">
  <img src="https://raw.githubusercontent.com/gkonovalov/android-vad/master/vad-comparison.png" />
</p>

## Parameters
VAD library only accepts 16-bit Mono PCM audio stream and can work with next Sample Rates, 
Frame Sizes and Classifiers.

#### WebRTC VAD
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
| LOW_BITRATE       |
| AGGRESSIVE        |
| VERY_AGGRESSIVE   |
</td>
</tr>
</table>

#### Silero VAD
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
| OFF               |
| NORMAL            |
| AGGRESSIVE        |
| VERY_AGGRESSIVE   |
</td>
</tr>
</table>

#### Yamnet VAD
<table>
<tr>
<td>

| Valid Sample Rate |  Valid Frame Size   |
|:-----------------:|:-------------------:|
|      16000Hz      | 243, 487, 731, 975  |

</td>
<td>

| Valid Classifiers |
|:------------------|
| OFF               |
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

Recommended parameters for WebRTC VAD:
* Sample Rate - **16KHz**,
* Frame Size - **512**,
* Mode - **VERY_AGGRESSIVE**,
* Silence Duration - **300ms**,
* Speech Duration - **50ms**.

## Usage
VAD supports 2 different ways of detecting speech:

1. Continuous Speech listener was designed to detect long utterances
   without returning false positive results when user makes pauses between
   sentences.

```kotlin
    val vad = Vad.builder()
        .setContext(applicationContext)
        .setSampleRate(SampleRate.SAMPLE_RATE_16K)
        .setFrameSize(FrameSize.FRAME_SIZE_512)
        .setMode(Mode.VERY_AGGRESSIVE)
        .setSilenceDurationMs(300)
        .setSpeechDurationMs(50)
        .build()

    //Silero and WebRTC continuous speech detector.
    vad.setContinuousSpeechListener(audioData, object : VadListener {
        override fun onSpeechDetected() {
            //speech detected!
        }

        override fun onNoiseDetected() {
            //noise detected!
        }
    })

    //Yamnet continuous sound classifier.
    vad.setContinuousClassifierListener("Cat", audioData, object : VadListener {
        override fun onResult(event: SoundCategory) {
            when (event.label) {
                "Cat" -> "Cat!" + event.score
                else -> "Noise!" + event.score
            }
        }
    })

    vad.close()
```

2. Speech detector was designed to detect speech/noise in short audio
   frames and return result for every frame. This method will not work for
   long utterances.

```kotlin
    val vad = Vad.builder()
        .setContext(applicationContext)
        .setSampleRate(SampleRate.SAMPLE_RATE_16K)
        .setFrameSize(FrameSize.FRAME_SIZE_512)
        .setMode(Mode.VERY_AGGRESSIVE)
        .build()

    //Silero and WebRTC speech detector.
    val isSpeech = vad.isSpeech(audioData)

    //Yamnet audio classifier.
    val soundCategory = vad.classifyAudio(audioData)

    vad.close()
```

## Requirements
Android VAD supports Android 6.0 (API level 23) and later and require JDK 8 or later.

## Dependencies
#### Silero VAD DNN
The library utilizes the ONNX runtime to run Silero VAD DNN, which requires the addition of 
necessary dependencies.

```groovy
dependencies {
   implementation 'com.microsoft.onnxruntime:onnxruntime-android:1.15.1'
}
```
#### Yamnet VAD DNN
The library utilizes the Tensorflow Lite runtime to run Yamnet VAD DNN, which requires next dependencies.

```groovy
dependencies {
   implementation 'org.tensorflow:tensorflow-lite-task-audio:0.4.0'
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

2. Add one dependency from list below:

#### WebRTC VAD
```groovy
dependencies {
    implementation 'com.github.gkonovalov.android-vad:webrtc:2.0.3'
}
```

#### Silero VAD
```groovy
dependencies {
    implementation 'com.github.gkonovalov.android-vad:silero:2.0.3'
}
```

#### Yamnet VAD
```groovy
dependencies {
    implementation 'com.github.gkonovalov.android-vad:yamnet:2.0.3'
}
```
You also can download precompiled AAR library and APK files from 
GitHub's [releases page](https://github.com/gkonovalov/android-vad/releases).

## References
<a id="1">[1]</a>
[WebRTC VAD](https://chromium.googlesource.com/external/webrtc/+/branch-heads/43/webrtc/common_audio/vad/) -
Voice Activity Detector from Google which is reportedly one of the best available: it's fast,
modern and free. This algorithm has found wide adoption and has recently become one of the
gold-standards for delay-sensitive scenarios like web-based interaction.

<a id="2">[2]</a>
[Silero VAD](https://github.com/snakers4/silero-vad) - pre-trained enterprise-grade Voice Activity Detector,
Number Detector and Language Classifier <a href="mailto:hello@silero.ai">hello@silero.ai</a>.

<a id="3">[3]</a>
[Yamnet VAD](https://github.com/tensorflow/models/tree/master/research/audioset/yamnet) -
YAMNet is a pretrained deep neural network that can predicts 521 audio event classes based on the AudioSet-YouTube 
corpus, employing the Mobilenet_v1 depthwise-separable convolution architecture.

------------
Georgiy Konovalov 2023 (c) [MIT License](https://opensource.org/licenses/MIT)