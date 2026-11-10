# PD 소스 빌드와 테스트

이 문서는 i30 PD Fork의 v1.0.0 / 1.0.0-pd와 선택형 My Apps v10 소스에 적용합니다. 설치·실행은 [PD 설치 안내](INSTALL_PD.md), 대상과 실차 확인 범위는 [PD 검증 기록](PD_VALIDATION.md)을 참고하세요.

## 준비물

- Python 3.9 이상, JDK 17 또는21의 `java`·`javac`.
- 외부 Android SDK의 API16 또는17 `android.jar`.
- AAPT2 실행 파일과 D8가 포함된 R8 JAR.

외부 도구·SDK·개인키는 저장소에 포함하지 않습니다. `compile-stubs/`의 최소 OEM 선언은 컴파일 classpath에만 사용하고 앱 DEX에 패키징하지 않습니다. 실행에는 대상 AVN에 설치된 `automotive` 프레임워크가 필요합니다. Java8 입력을 D8 `--min-api17`로 변환하며 DEX035를 사용합니다.

## 호스트 검사

JDK의 bin을 PATH에 추가해 Python의 Java 서비스 검사에서도 사용할 수 있게 합니다.

```sh
python3 scripts/test.py --java /path/to/jdk/bin/java
python3 -m unittest discover -s tests -p 'test_*.py'
python3 scripts/generate_translations.py --check
python3 scripts/verify_release.py
```

이 검사는 합성 입력과 작은 Android 스텁을 사용하며 차량·ADB·원시 캡처를 요구하지 않습니다. 실차 센서 정확도나 외부 하드웨어 동작을 대신 검증하지 않습니다. 공개 준비 당시 Java 프로그램15개, Python 검사44개가 통과했습니다.

## 서명 전 APK 생성

아래 도구 경로를 실제 경로로 바꾸고 새 출력 폴더를 지정합니다.

```sh
python3 scripts/build.py dashboard \
  --java /path/to/jdk/bin/java \
  --android-jar /path/to/android.jar \
  --aapt2 /path/to/aapt2 \
  --r8-jar /path/to/r8.jar \
  --out build/pd-dashboard
```

My Apps는 `dashboard` 대신 `hub`, 별도 `--out` 경로를 사용합니다. 결과는 `dashboard-UNSIGNED.apk` 또는 `hub-UNSIGNED.apk`이며 그대로 설치할 수 없습니다. API17 호환 V1/JAR 서명과 ZIP 정렬은 별도로 수행해야 합니다. 기존 앱 업데이트에는 서명 일치가 필요합니다. 공개 APK의 AOSP 테스트 인증서와 외부 도구의 권리·한계는 [NOTICE](../NOTICE.md)를 확인하세요.

과거 v11은 실제 설치·재읽기 해시가 일치한 APK입니다. 공개 소스를 JDK21/Java8→D8로 다시 컴파일한 DEX가 이 APK의 DEX와 바이트 일치했습니다. 당시 공개 v6의 프로젝트 리소스를 유지한 매니페스트·리소스 페이로드도 일치했습니다. 당시 기록은 [v11 빌드 출처](PD_BUILD_PROVENANCE.json)에 보존합니다.

v1.0.0은 동일 소스를 다시 컴파일하고 버전 속성만 변경한 재빌드입니다. Android `versionName`은 `1.0.0-pd`, 내부 `versionCode`는 기존11보다 큰12입니다. DEX·컴파일된 리소스는 v11과 바이트 일치하며, 매니페스트 차이는 버전 속성 두 개뿐입니다. 공개 v6의 프로젝트 리소스를 재사용했고 일반 AAPT2 리소스 재생성 경로는 이번에도 실행하지 않았습니다. APK 서명·정렬·패키지·권한을 오프라인에서 확인했습니다. [v1.0.0 빌드 출처](PD_1.0.0_BUILD_PROVENANCE.json)에 현재 매니페스트를 포함한 소스 해시를 기록했습니다. 전체 ZIP·서명 메타데이터의 동일 재현이나 새 APK의 실차 설치·설정 유지 검증을 의미하지 않습니다.

## 배포 파일과 문서 갱신

현재 PD 설치 파일은 `apks/i30-PD-Dashboard-v1.0.0-API17.apk`입니다. PD 공개 버전은 1.0.0부터 독립 관리하고, Android 내부 `versionCode`는 기존 설치본보다 증가시킵니다. 과거 PD v11 및 이전 JSN APK는 이력·출처 확인용으로 남깁니다. 기존 JSN 설치·메뉴 스크립트의 프로필 제한은 유지합니다. 수정하지 않은 My Apps의 버전은 변경하지 않습니다.

새 파일을 배포하면 `release.json`, `SHA256SUMS`, `VALIDATION.json`, `SOURCE_SHA256SUMS`와 확인 범위를 함께 갱신합니다. 개인키·OEM 구현·펌웨어·원시 차량 캡처·식별자·개인 설정 및 백업은 추가하지 않습니다. 라이선스·저작권·외부 의존성 고지는 유지합니다.
