# Moonlight Android 打包大包指南

## 目标

- 在 `macOS` 上打出一个可安装的 `release APK`
- 适用于当前仓库：`/Users/reynold/Desktop/Git/moonlight-android`
- 最终产物：`/Users/reynold/Desktop/moonlight-axi-nonRoot-release.apk`

## 1. 安装环境

- 安装 `JDK 17`

```bash
brew install openjdk@17
```

- 如果还没装 Android SDK 命令行工具，先打开一次 `Android Studio`，让它初始化 SDK 目录
- 设置环境变量

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export ANDROID_SDK_ROOT="$HOME/Library/Android/sdk"
export PATH="$JAVA_HOME/bin:$ANDROID_SDK_ROOT/platform-tools:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$PATH"
```

## 2. 拉取子模块

- 这个项目依赖子模块，不拉取会编译失败

```bash
cd /Users/reynold/Desktop/Git/moonlight-android
git submodule update --init --recursive
```

## 3. 安装 SDK / NDK

- 当前项目要求 `NDK 27.0.12077973`

```bash
yes | sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0" "ndk;27.0.12077973"
```

## 4. 生成 `local.properties`

- 在项目根目录创建 `local.properties`

```bash
cd /Users/reynold/Desktop/Git/moonlight-android

cat > local.properties <<EOF
sdk.dir=$HOME/Library/Android/sdk
ndk.dir=$HOME/Library/Android/sdk/ndk/27.0.12077973
EOF
```

## 5. 编译 `release APK`

- 普通设备使用 `nonRootRelease`

```bash
cd /Users/reynold/Desktop/Git/moonlight-android
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
./gradlew clean assembleNonRootRelease
```

- 编译输出的未签名 APK 一般在：

```text
app/build/outputs/apk/nonRoot/release/app-nonRoot-release-unsigned.apk
```

## 6. 生成签名证书

- 第一次打包时生成一次即可

```bash
keytool -genkeypair \
  -v \
  -keystore "$HOME/moonlight-android.keystore" \
  -alias moonlightaxi \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

## 7. 对 APK 进行签名

- 先执行 `zipalign`，再执行 `apksigner`

```bash
cd /Users/reynold/Desktop/Git/moonlight-android
export ANDROID_SDK_ROOT="$HOME/Library/Android/sdk"

"$ANDROID_SDK_ROOT/build-tools/34.0.0/zipalign" -f 4 \
  app/build/outputs/apk/nonRoot/release/app-nonRoot-release-unsigned.apk \
  "$HOME/Desktop/moonlight-axi-aligned.apk"

"$ANDROID_SDK_ROOT/build-tools/34.0.0/apksigner" sign \
  --ks "$HOME/moonlight-android.keystore" \
  --ks-key-alias moonlightaxi \
  --out "$HOME/Desktop/moonlight-axi-nonRoot-release.apk" \
  "$HOME/Desktop/moonlight-axi-aligned.apk"

"$ANDROID_SDK_ROOT/build-tools/34.0.0/apksigner" verify --verbose \
  "$HOME/Desktop/moonlight-axi-nonRoot-release.apk"
```

## 8. 最终产物

- 最终安装包路径：

```text
/Users/reynold/Desktop/moonlight-axi-nonRoot-release.apk
```

## 9. 一条龙完整命令

- 下面命令会自动完成环境准备、子模块拉取、SDK/NDK 安装、编译、签名
- 如果本地还没有 `keystore`，中间会提示你输入密码并创建

```bash
cd /Users/reynold/Desktop/Git/moonlight-android && \
brew install openjdk@17 && \
export JAVA_HOME=$(/usr/libexec/java_home -v 17) && \
export ANDROID_SDK_ROOT="$HOME/Library/Android/sdk" && \
export PATH="$JAVA_HOME/bin:$ANDROID_SDK_ROOT/platform-tools:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$PATH" && \
git submodule update --init --recursive && \
yes | sdkmanager --licenses && \
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0" "ndk;27.0.12077973" && \
cat > local.properties <<EOF
sdk.dir=$HOME/Library/Android/sdk
ndk.dir=$HOME/Library/Android/sdk/ndk/27.0.12077973
EOF
./gradlew clean assembleNonRootRelease && \
if [ ! -f "$HOME/moonlight-android.keystore" ]; then
  keytool -genkeypair -v -keystore "$HOME/moonlight-android.keystore" -alias moonlightaxi -keyalg RSA -keysize 2048 -validity 10000;
fi && \
"$ANDROID_SDK_ROOT/build-tools/34.0.0/zipalign" -f 4 app/build/outputs/apk/nonRoot/release/app-nonRoot-release-unsigned.apk "$HOME/Desktop/moonlight-axi-aligned.apk" && \
"$ANDROID_SDK_ROOT/build-tools/34.0.0/apksigner" sign --ks "$HOME/moonlight-android.keystore" --ks-key-alias moonlightaxi --out "$HOME/Desktop/moonlight-axi-nonRoot-release.apk" "$HOME/Desktop/moonlight-axi-aligned.apk" && \
"$ANDROID_SDK_ROOT/build-tools/34.0.0/apksigner" verify --verbose "$HOME/Desktop/moonlight-axi-nonRoot-release.apk"
```

## 10. 如果只想快速测试

- 不签名正式包，直接打调试包

```bash
cd /Users/reynold/Desktop/Git/moonlight-android
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
./gradlew assembleNonRootDebug
```

- 输出目录一般为：

```text
app/build/outputs/apk/nonRoot/debug/
```
