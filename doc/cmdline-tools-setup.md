# How to setup cmdline-tools to build and test this project in Ubuntu

Download https://developer.android.com/studio/#command-tools

Run following commands:

```
## to get rid of openjdk11 and replace it with 8
## run following commented lines as root
# apt-get autoremove openjdk*
# apt-get install openjdk-8-jdk
cd $HOME
mkdir -p android_sdk/cmdline-tools
cd android_sdk/cmdline-tools
unzip ~/commandlinetools-linux-*_latest.zip
export ANDROID_SDK_ROOT=$HOME/android_sdk ###
cmdline-tools/bin/sdkmanager --install "cmdline-tools;latest"
cmdline-tools/bin/sdkmanager --install "build-tools;29.0.3"
cmdline-tools/bin/sdkmanager --install "platforms;android-28"
rm -rf cmdline-tools
export PATH=$HOME/android_sdk/cmdline-tools/latest/bin:$PATH ###
yes | sdkmanager --licenses
cd ~/src/lamp
./gradlew tasks
./gradlew build
# connect your phone/emulator (check: adb devices, adb shell)
./gradlew installDebug
```

Consider adding lines marked with `###` to your `~/.profile` or equivalent.

See https://stackoverflow.com/questions/60440509/android-command-line-tools-sdkmanager-always-shows-warning-could-not-create-se
