# 증강 픽던

**증강 픽던**은 [Shattered Pixel Dungeon](https://shatteredpixel.com/shatteredpd/) 4.0.2를 바탕으로 만든 **비공식 팬 모드**입니다.
레벨업마다 증강 카드를 골라 강해지는 "증강" 시련과, 던전의 분위기를 살리는 시각 효과(조명·그림자·물 반사·안개·흔들리는 풀 등)가 들어 있습니다.

- 모드 제작: **하남자 매직심포니**
- 기반 게임: Shattered Pixel Dungeon 4.0.2 — 개발 Evan Debenham (https://shatteredpixel.com)
- Shattered Pixel Dungeon은 Watabou의 Pixel Dungeon 오픈소스를 바탕으로 합니다.

> 이 모드는 원작 개발자(Evan Debenham, Shattered Pixel)와 **아무 관계가 없으며**, 원작 개발자가 만들거나 확인한 것이 아닙니다.
> 모드에서 생긴 문제나 문의는 원작 쪽이 아니라 **모드 제작자에게** 해 주세요.
> 원작이 마음에 드신다면 공식 판(구글 플레이, 앱스토어, 스팀 등)을 구매하거나 원작 개발자의 Patreon을 후원해 주세요.

## 내려받기

[Releases](../../releases) 페이지에서 최신 버전의 APK를 받을 수 있습니다.
이 저장소가 그 APK를 만든 전체 소스 코드입니다. 버전별 변경 내역은 [CHANGELOG.md](CHANGELOG.md)에 있습니다.

## 설치

### 안드로이드

1. Releases에서 받은 APK를 휴대폰에서 실행합니다. (안드로이드 5.0 이상)
2. "출처를 알 수 없는 앱" 설치 허용을 묻는다면 허용합니다.
3. 공식 Shattered Pixel Dungeon과는 **별개의 앱**으로 설치되며, 저장 데이터도 따로 관리됩니다. 공식 판을 지울 필요가 없습니다.
4. 이전에 받은 "SPD 분위기판" 빌드가 있다면, 그 위에 그대로 덮어 설치되고 저장 데이터도 유지됩니다.

### Windows

1. Releases에서 `AugmentPD-버전-windows.zip`을 받아 압축을 풉니다. (Windows 10 이상, 64비트)
2. 풀린 `AugmentPD` 폴더 안의 `AugmentPD.exe`를 실행합니다. Java를 따로 설치할 필요는 없습니다.
3. "Windows의 PC 보호" 창이 뜨면 *추가 정보* → *실행*을 누릅니다. 코드 서명을 하지 않은 프로그램이라 처음 실행할 때 나오는 안내입니다.
4. 공식 Shattered Pixel Dungeon과 저장 데이터가 따로 관리됩니다. 휴대폰과 PC의 저장 데이터는 서로 이어지지 않습니다.

## 즐기는 법

- **증강 시련**: 영웅 선택 화면의 *도전* 에서 "증강"을 켜고 시작합니다. 레벨업할 때마다 증강 카드 셋 중 하나를 고릅니다. 자세한 규칙은 도전 목록에서 "증강"을 눌러 볼 수 있습니다.
- **증강 II 시련 (개발 중)**: "증강" 바로 아래에 있는 두 번째 증강 시련입니다. 정해진 레벨마다 직접 쓰는 스킬을 골라 화면의 스킬 버튼으로 사용합니다. 아직 기본 스킬만 들어 있는 미완성 상태입니다. "증강"과 함께 켤 수 없습니다.
- **분위기 효과**: *설정 → 화면* 에서 분위기 효과를 켜고 끄거나, 화면 흔들림·피해 숫자·증강 발동 알림 등을 조절할 수 있습니다. 성능이 낮은 기기에서는 분위기 효과를 끄면 가벼워집니다.
- **편의 기능**: *설정 → 화면* 에서 애니메이션 속도를 바꾸거나, 이슬과 씨앗을 밟을 때 자동으로 줍게(1턴 소모) 할 수 있습니다.
- **시드 파인더**: 영웅 선택 화면의 옵션에 있는 "시드 파인더"로, 원하는 아이템이 나오는 던전 시드를 찾을 수 있습니다.

## 소스에서 직접 빌드하기

원작 Shattered Pixel Dungeon과 같은 방식으로 빌드합니다. 원작 안내서는 `docs/` 에 그대로 들어 있습니다.

### 준비물
- JDK 17 이상 (21 권장)
- Android SDK (API 36). Android Studio를 설치하면 함께 설치됩니다.
- 인터넷 연결 (처음 빌드할 때 Gradle과 라이브러리를 내려받습니다)

`local.properties` 파일을 만들고 SDK 위치를 적어 주세요. 예:
```
sdk.dir=C\:\\Users\\이름\\AppData\\Local\\Android\\Sdk
```

### 안드로이드 APK
```
./gradlew :android:assembleRelease        (Windows: gradlew.bat :android:assembleRelease)
```
결과물: `android/build/outputs/apk/release/`

- 서명 키는 소스에 들어 있지 않습니다. 환경 변수 `ATMO_KEYSTORE`(키 파일 경로), `ATMO_KS_PASS`(비밀번호), `ATMO_KEY_ALIAS`(별칭)를 지정하면 그 키로 서명합니다.
- 지정하지 않으면 서명되지 않은 APK가 나오므로, 직접 만든 키로 서명하거나 디버그 빌드(`:android:assembleDebug`)를 쓰세요.
- 직접 만든 키로 서명한 APK는 배포된 APK와 서명이 달라서, 이미 설치된 앱 위에 덮어 설치할 수 없습니다.

### PC(데스크톱)에서 실행
```
./gradlew :desktop:debug
```

### Windows용 exe 폴더
Windows PC에서 아래 명령을 실행하면 `desktop/build/jpackage/AugmentPD/` 에 Java 없이 실행되는 폴더가 만들어집니다.
```
gradlew.bat :desktop:jpackageImage
```

### 이 모드를 고쳐서 다시 배포하려면
원작 개발자의 안내(`docs/recommended-changes.md`)를 따라 앱 이름·패키지 이름·아이콘을 자신의 것으로 바꾸고,
GPLv3에 따라 소스를 함께 공개해 주세요.

## 라이선스

Shattered Pixel Dungeon은 **GNU General Public License v3.0(GPLv3)** 으로 공개되어 있으며, 이 모드도 같은 라이선스를 따릅니다.
누구나 이 모드를 받고, 고치고, 다시 배포할 수 있습니다. 단, 다시 배포할 때도 소스 코드를 함께 공개하고 같은 라이선스를 지켜야 합니다.
자세한 내용은 `LICENSE.txt`를 보세요.

원작 및 원작이 사용한 리소스의 저작자 표기는 게임 안 *정보(About)* 화면에 그대로 남아 있습니다.
