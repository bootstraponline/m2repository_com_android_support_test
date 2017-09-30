# release notes

[Android support test release notes](https://developer.android.com/topic/libraries/testing-support-library/release-notes.html)

# local m2repository

- `~/Library/Android/sdk/extras/android/m2repository/com/android/support/test`

Note: New versions have moved to `https://maven.google.com`

# maven.google.com

```
// Add to build.gradle
maven {
  url 'https://maven.google.com'
}
```

[List of packages](https://developer.android.com/topic/libraries/testing-support-library/packages.html)

- [runner-1.0.0.pom](https://maven.google.com/com/android/support/test/runner/1.0.0/runner-1.0.0.pom)
- [rules-1.0.0.pom](https://maven.google.com/com/android/support/test/rules/1.0.0/rules-1.0.0.pom)
- [espresso-core-3.0.0](https://maven.google.com/com/android/support/test/espresso/espresso-core/3.0.0/espresso-core-3.0.0.pom)
- [espresso-contrib-3.0.0.pom](https://maven.google.com/com/android/support/test/espresso/espresso-contrib/3.0.0/espresso-contrib-3.0.0.pom)
- [espresso-intents-3.0.0.pom](https://maven.google.com/com/android/support/test/espresso/espresso-intents/3.0.0/espresso-intents-3.0.0.pom)
- [espresso-accessibility-3.0.0.pom](https://maven.google.com/com/android/support/test/espresso/espresso-accessibility/3.0.0/espresso-accessibility-3.0.0.pom)
- [espresso-web-3.0.0.pom](https://maven.google.com/com/android/support/test/espresso/espresso-web/3.0.0/espresso-web-3.0.0.pom)
  - [espresso-web-3.0.1-sources.jar](https://maven.google.com/com/android/support/test/espresso/espresso-web/3.0.1/espresso-web-3.0.1-sources.jar)
- [espresso-idling-resource-3.0.0.pom](https://maven.google.com/com/android/support/test/espresso/espresso-idling-resource/3.0.0/espresso-idling-resource-3.0.0.pom)
- [idling-net-3.0.0.pom](https://maven.google.com/com/android/support/test/espresso/idling/idling-net/3.0.0/idling-net-3.0.0.pom)
- [idling-concurrent-3.0.0.pom](https://maven.google.com/com/android/support/test/espresso/idling/idling-concurrent/3.0.0/idling-concurrent-3.0.0.pom)

# Android Test Orchestrator

[Usage notes](https://developer.android.com/training/testing/junit-runner.html#using-android-test-orchestrator) for Android Test Orchestrator.

```
adb install -r orchestrator-1.0.0.apk
adb install -r test-services-1.0.0.apk

adb shell 'CLASSPATH=$(pm path android.support.test.services) app_process / \
  android.support.test.services.shellexecutor.ShellMain am instrument -w -e \
  targetInstrumentation com.example.test/android.support.test.runner.AndroidJUnitRunner \
  android.support.test.orchestrator/.AndroidTestOrchestrator'
```


- [orchestrator-1.0.0.pom](https://maven.google.com/com/android/support/test/orchestrator/1.0.0/orchestrator-1.0.0.pom)
  - [orchestrator-1.0.0.apk](https://maven.google.com/com/android/support/test/orchestrator/1.0.0/orchestrator-1.0.0.apk)
  - [orchestrator-1.0.0-sources.jar](https://maven.google.com/com/android/support/test/orchestrator/1.0.0/orchestrator-1.0.0-sources.jar)
- [test-services-1.0.0.pom](https://maven.google.com/com/android/support/test/services/test-services/1.0.0/test-services-1.0.0.pom)
  - [test-services-1.0.0.apk](https://maven.google.com/com/android/support/test/services/test-services/1.0.0/test-services-1.0.0.apk)
  - [test-services-1.0.0-sources.jar](https://maven.google.com/com/android/support/test/services/test-services/1.0.0/test-services-1.0.0-sources.jar)

