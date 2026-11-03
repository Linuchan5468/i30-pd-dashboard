# i30 PD Dashboard

[reikop/veloster-n-dashboard](https://github.com/reikop/veloster-n-dashboard)를 기반으로 i30 PD에 맞춰 수정한 비공식 대시보드입니다. [English](README.md)

원본 커밋 이력과 [MIT LICENSE](LICENSE)를 유지하며 [NOTICE](NOTICE.md)에 출처·외부 의존성을 기재했습니다. 원본 설명과 당시 검증 기록은 [docs/upstream](docs/upstream/README_ko.md)에 보존했습니다. 원본의 Veloster N 검증 결과와 이 Fork의 PD 결과를 구분합니다.

## 확인된 차량

**한국 사양 i30 PD N Line, 7단 DCT 차량 1대**의 순정 Gen5 Android 4.2.2/API17 AVN에서 작업했습니다. 제품명 `pd_kr`, 빌드 `eng.vmadmin.20260114.084130`, 펌웨어 `PD.KOR.SOP.V141.260114.STD_H`입니다. 다른 연식·엔진·트림·지역·AVN 빌드나 일반 Android 기기 호환성은 확인하지 않았습니다. 수신 연결의 정확한 제품/빌드 제한을 유지했습니다. 기존 JSN 분기는 남아 있으나 PD v11 APK의 Veloster N 재검증은 하지 않았습니다.

## 설치 파일

[N-Dashboard-v11-PD-API17.apk](apks/N-Dashboard-v11-PD-API17.apk): **v11 / 2.0-pdkr**, 2026-10-29 실차 설치본을 다시 읽어 해시 일치까지 확인한 파일입니다. 최초 최종 UI 검사는 시동 종료 후 AVN 전원을 유지한 상태에서 수행했습니다. 이후 사용자가 콜드 부팅 후 앱 실행과 저장된 설정값 유지를 실차에서 확인했습니다.

```sh
python3 scripts/verify_release.py
```

[PD 설치·실행 안내](docs/INSTALL_PD.md)를 따르세요. 기존 `scripts/install.py`와 OEM 메뉴 도구는 **JSN 전용 프로필**을 유지하며 PD 자동 설치 도구가 아닙니다. 변경된 OEM 메뉴 APK는 배포하지 않습니다. My Apps v10은 수정 없이 남긴 원본의 선택형 보조 앱이며, 원본 기록의 미검증 항목도 그대로 적용됩니다.

APK는 호환성을 위해 공개 AOSP platform 테스트 인증서로 서명돼 있습니다. 배포자의 비밀 신원 인증서가 아니며 개인키는 포함하지 않습니다. [배포 목록·해시](release.json)와 [NOTICE](NOTICE.md)를 함께 확인하세요.

## 실제 수정 사항

- PD 전용 RPM·속도 수신, 스로틀/가속 페달 분리, 냉각수·흡기·외기 온도, 브레이크 압력, 조향각·조향 속도, 횡·종가속 처리.
- P/R/N 및 D1~D7 표시. 변속 위치 변경 전 단수를 폐기하고 신선한 위치·단수 수신을 조합합니다. 실차에서 P/R/N 및 전진 D1~D7 전체 표시를 확인했습니다.
- 네 바퀴 공기압. 정차 후 계기판의 32/32/32/31 psi와 수신값을 대조했습니다. 다른 단위는 호스트 변환 검사만 했습니다.
- TCS·ESC **사용 상태** ON/OFF와 **ABS 경고등** 표시. 실제 제어 개입 여부를 표시하는 기능은 아닙니다. 사용자가 실차에서 TCS·ESC의 개별 ON/OFF 동작을 확인했으며, TCS만 해제할 때 ESC가 ON으로 유지되는 것도 확인했습니다.
- 수신 비율·기준토크·RPM으로 계산한 추정 순토크·출력, 절대압과 대기압으로 계산한 게이지 부스트. 실측 엔진/휠 출력이나 교정된 진단값으로 주장하지 않습니다.
- F1의 `PD`·`i30 N Line` 문구와 차명 크기 확대, PD 전체 조망 배치, 목표 RPM **3000~6700 / 100 단위**. 기본값·기존 저장값 6000은 유지합니다.

오일 온도는 AVN 수신 경로에서 사용할 수 있는 값을 확인하지 못해 해당 차량용으로 구현하지 못했습니다. 이 차량에는 e-LSD가 장착되지 않아 해당 기능을 사용할 수 없습니다. 유효한 배터리 SOC·전압, 휠 속도와 ABS 실제 개입은 확인된 PD 수신 경로에서 검증되지 않았습니다. 미수신·무효 값은 `-`로 유지합니다. 일부 수치 정의·무효값 정책은 연구 해석이며, 모든 OEM 상태 정의를 규명한 것은 아닙니다. [검증 범위와 한계](docs/PD_VALIDATION.md), [변경 기록](CHANGELOG.md)을 참고하세요.

## 소스·공개 범위

[빌드 안내](docs/BUILD.md)의 외부 Android 도구로 서명 전 APK를 만들 수 있습니다. 합성 입력 호스트 검사는 다음과 같습니다.

```sh
python3 scripts/test.py --java /path/to/jdk/bin/java
python3 -m unittest discover -s tests -p 'test_*.py'
python3 scripts/generate_translations.py --check
```

PD 공개 변경에는 순정 펌웨어·OEM APK/ODEX 구현·원시 차량 캡처·기기 식별자·개인 설정/백업·SDK 바이너리·서명 개인키를 포함하지 않았습니다. 컴파일용 최소 OEM 선언은 DEX에 넣지 않습니다. 대시보드 수신은 ECU 진단·제어 명령을 추가 송신하지 않습니다. My Apps의 명시적 설정/미디어 조작은 별도 동작입니다.
