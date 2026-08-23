# NagMe Bugs

## Open

### TTS startup delay on some devices

**Status:** Open  
**Observed:** Internal testing, non-AI nag on another Android device  
**Impact:** The Nag notification can appear several seconds before the spoken reminder begins.

#### Symptom

On at least one test device, a non-AI Nag notification appears immediately, but the voice starts roughly 5 seconds later.

#### Current finding

`SpeechManager` creates a new `TextToSpeech` instance when a Nag execution needs speech. The TTS engine is asynchronous to initialise, and `NagExecutionService` later calls `SpeechManager.shutdown()`, which stops and shuts down TTS and sets it back to `null`.

This means a later Nag can cold-start the TTS engine again. On slower devices or TTS engines, that initialisation may account for the visible notification-to-speech delay.

#### Relevant code

- `app/src/main/java/com/example/nagme/speech/SpeechManager.kt`
- `app/src/main/java/com/example/nagme/execution/NagExecutionService.kt`

#### Next tests

- Verify whether every Nag on the affected device has the same delay or only the first one.
- Test AI OFF first to isolate TTS from network/LLM latency.
- Compare Google TTS versus the device default/fallback engine.
- Measure TTS `onInit` timing before changing architecture.

#### Possible future fix

Consider keeping the TTS engine warm for longer rather than creating and shutting it down for each execution, but only after measuring the behaviour and reviewing lifecycle/battery implications.
