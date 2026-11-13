# PD 데이터 수집과 매핑

[English](PD_DATA_MAPPING.md) | **한국어** · [README](../README_ko.md)

i30 PD용 수신 매핑을 어디서 확인했고, 어떤 경로로 수집·해석했는지 정리한 문서입니다. **해당 PD 차량의 순정 AVN 파서가 주근거**이며, 원본 앱의 수신 구조·공개 CAN 정의·실차 대조를 함께 사용했습니다. 인터넷 매핑표 하나를 그대로 적용한 것은 아닙니다.

정리 기준은 2026-11-13, 구현 확인 기준은 [커밋 `7a6f92b`](https://github.com/Linuchan5468/i30-pd-dashboard/commit/7a6f92b9122f36879734663bdf00130d79927c3b)입니다. 이 문서 작성 과정에서 새 차량 연결이나 수집 시험은 하지 않았습니다. 배포 APK와 현재 소스의 차이는 아래 [계산값](#계산값과-화면-표시)을 참고하세요.

## 확인된 환경

한국 사양 **i30 PD N Line, 7단 DCT 차량 1대**의 순정 Gen5 AVN에서 확인했습니다.

| 항목 | 확인된 값 |
| --- | --- |
| 제품 | `pd_kr` |
| Android | 4.2.2 / API 17 |
| 빌드 | `eng.vmadmin.20260114.084130` |
| 펌웨어 | `PD.KOR.SOP.V141.260114.STD_H` |

다른 차종·연식·지역·엔진·AVN 빌드에는 별도 확인이 필요합니다. 앱의 수신 연결도 위 제품·빌드로 제한합니다. [검증 범위](PD_VALIDATION.md).

## 어디서 확인했는가

| 자료 | 활용한 부분 |
| --- | --- |
| [reikop/veloster-n-dashboard](https://github.com/reikop/veloster-n-dashboard) | 독립 Android 앱 구조와 HSCAN 소켓 수신 방식. 원본 JSN용 배치를 그대로 적용하면 PD의 RPM·속도 등이 표시되지 않아 PD 분기를 추가했습니다. |
| 해당 차량에서 확보한 순정 `com.infobank.odex` | PD 메시지 테이블과 파서. 내부 function 번호·실제 CAN ID·길이·바이트/비트 위치·환산식을 추적한 주자료입니다. OEM 바이너리와 디컴파일 코드는 배포하지 않습니다. |
| [commaai/opendbc Hyundai CCAN](https://github.com/commaai/opendbc/blob/master/opendbc/dbc/hyundai_2015_ccan.dbc) | 대응하는 메시지 ID를 확인한 뒤 TPS·흡기온도·ESP12 가속도/압력·토크 입력 등의 비트와 단위를 대조했습니다. 공개 DBC 전체가 AVN에 전달된다고 가정하지 않았습니다. |
| [OPKR TPMS 해석](https://github.com/openpilotkr/openpilot/blob/3ff6dc6081c2a986c75292eefa0952641ab66b6f/selfdrive/car/hyundai/carstate.py) | TPMS 단위 해석을 대조했습니다. 앱은 표시값을 psi로 통일합니다. |
| 실차 캡처와 사용자 확인 | 시동·브레이크·조향·기어·TCS/ESC 상태와 계기판 공기압을 수신값에 대조했습니다. 반응 확인과 계측 정확도 교정은 구분합니다. |

순정 코드는 다음 순서로 확인했습니다.

1. `Can2BPDMessage`: 메시지 이름, 실제 CAN ID, MICOM function 번호, 길이의 대응표.
2. `CanMessageManager`: 수신 function을 대응표에서 찾는 과정.
3. `CanCsm.receivedCanDataFor_ALL_C`: 레코드 분리와 파서 입력 배열 구성.
4. `Can2BCsm`: `parseEMS11`·`parseCLU11`·`parseEMS12`·`parseSAS11`·`parseESP12` 등의 위치·부호·배율.
5. `ConvertType`: 부호 없는 바이트 변환. 순정 서비스 getter는 결과 대조에 활용했습니다.

opendbc의 `master` 링크는 변경될 수 있습니다. 위 링크를 이 프로젝트가 검증한 고정 커밋으로 보지는 않습니다. [NOTICE](../NOTICE.md)와 [검증 문서](PD_VALIDATION.md)에도 외부 참조와 범위를 남겼습니다.

모든 비트와 단위를 순정 파서에서 직접 확인한 것은 아닙니다. ESP12 가속도·상태 비트와 일부 물리 단위는 대응하는 공개 정의로 보완했으며, 무효값 배제에는 앱의 보수적 정책도 포함합니다.

CN7 N 소프트웨어는 후속 G-Force 그래프·가속 측정·서킷 선택 UI를 참고하는 데 사용했습니다. **PD 센서 바이트 매핑의 주출처는 CN7 N 앱이 아닙니다.**

## 어떻게 수집하는가

```text
차량 신호 → AVN 순정 수신부 → hscand
        → UNIX abstract socket: hscan_mux
        → HscanDataClient → HscanFrameDecoder
        → Telemetry / PdGearState / PdPowertrain → 화면·이력
```

순정 초기화 설정에서 `hscand`가 `/dev/tcc-tsif1`을 사용하는 것을 확인했습니다. 앱이 실제로 읽는 지점은 순정 AVN이 내보내는 `hscan_mux` 로컬 스트림입니다. MICOM 내부 경로나 차량의 모든 원시 CAN 프레임을 재구현한 것은 아닙니다.

- 앱은 로컬 소켓에 수신자로 연결합니다. 자체 OBD PID·UDS 진단 요청, CAN 제어 명령이나 수신 시작 handshake를 추가하지 않습니다.
- ADB는 설치·분석·자료 회수에 사용한 PC 연결입니다. 앱의 정상 수신 데이터가 PC ADB를 거치는 구조는 아닙니다.
- IGN 상태와 앱 수명주기 조건을 확인합니다. 수신 클라이언트 시작·IGN 변경 시 텔레메트리 모델을 초기화합니다. 소켓만 끊어진 경우 부분 프레임과 G-Force/가속 측정의 연속성을 끊으며, 일반 값 전체를 즉시 삭제하지는 않습니다. 캐시 getter를 반복 조회한 값을 새 수신처럼 사용하지 않습니다.
- 수신 시각은 `LocalSocket.read()` 완료 시점의 `SystemClock.elapsedRealtime()`입니다. ECU/CAN 하드웨어 타임스탬프가 아니며 같은 값도 새로 수신되면 새 표본입니다.

구현: [HscanDataClient](../apps/dashboard/src/com/veloster/dashboard/HscanDataClient.java), [VehicleDataClient](../apps/dashboard/src/com/veloster/dashboard/VehicleDataClient.java).

## 스트림과 인덱스 해석

264바이트 묶음 안에 여러 레코드가 들어 있습니다. 소켓의 한 번 읽기와 한 묶음의 경계는 같지 않습니다.

| 위치 | 현재 디코더가 확인하는 내용 |
| --- | --- |
| 0~7 | `FF 0B 8E 01 8B C7 01 00` 헤더 |
| 8 | 묶음 타입 `0xC7` |
| 9~10 | little-endian 레코드 수, 최대 84 |
| 11부터 | `[function 1바이트][길이 1바이트][payload 1~8바이트]` 반복 |
| 레코드 끝~262 | 0 padding |
| 전체 264바이트 | XOR 결과 0 |

전체 묶음의 구조·XOR·padding을 검사한 다음 PD용 function과 정확한 길이를 확인합니다. 분할 수신·연속 묶음·헤더 재탐색을 처리하고, 재연결·수신큐 단절 시 부분 프레임을 폐기합니다. API17의 일시적인 `IOException: Try again`은 읽기 대기로 다룹니다. 이 XOR는 **로컬 스트림 포장 검사**이며 차량 CAN CRC 검사를 대체한다는 뜻은 아닙니다. [HscanFrameDecoder](../apps/dashboard/src/com/veloster/dashboard/HscanFrameDecoder.java).

**내부 function 번호와 실제 CAN ID는 다릅니다.** 예를 들어 function `0x08`은 순정 테이블의 EMS11/CAN `0x316`, `0x02`는 CLU11/CAN `0x4F1`에 대응합니다. 앱은 function으로 분기하고 공개 DBC는 대응하는 CAN 메시지와 비교했습니다.

순정 파서 배열은 `[function, payload...]`이고 앱은 function을 분리합니다. 따라서 순정 배열 index 3/4는 앱 payload의 `b[2]`/`b[3]`입니다. 초기 RPM 후보도 이 한 바이트 차이를 추적해 수정했습니다.

## 적용한 매핑

아래 `b[n]`은 **function을 제외한 payload의 0부터 시작하는 unsigned byte**입니다. `u16LE`는 little-endian unsigned 16비트, `s16LE`는 signed 16비트입니다. `raw` 계산에서 `+`로 합친 비트 필드는 서로 겹치지 않습니다. 실제 유효성 조건까지 확인하려면 [Telemetry](../shared/src/com/veloster/vehicle/Telemetry.java)를 함께 읽으세요.

| function / 길이 | 항목 | 해석 |
| --- | --- | --- |
| `0x08` / 8 | RPM | `u16LE(b[2],b[3]) × 0.25 rpm`. `65535` 무효, 수신 RPM 0은 유효. |
| `0x02` / 4 | 속도 | `raw=b[1]+((b[2]&1)<<8)`, `raw×0.5`. `b[2]` bit1이 1이면 mph이므로 `×1.609344`로 km/h 변환. `511` 무효. |
| `0x09` / 8 | 냉각수 | `b[1]×0.75−48°C`, `255` 무효. |
| `0x09` / 8 | 가속페달 | `b[6]×0.3906%`, `255` 무효. 스로틀과 별도 신호. |
| `0x09` / 8 | TPS | `b[5]×0.4694836−15.0234742%`. raw 32~245만 사용하고 0~100%로 제한. |
| `0x0B` / 8 | 흡기온도 | `b[2]×0.75−48°C`, `255` 무효. |
| `0x14` / 8 | 외기온도 | `b[3]×0.5−40°C`, `255` 무효. |
| `0x19` / 5 | 조향각·속도 | `s16LE(b[0],b[1])×0.1°`, `b[2]×4°/s`. 관측 상태 `b[3]==7`, 각도 raw 절댓값 ≤10800, 속도 raw <255 사용. |
| `0x13` / 8 | 브레이크 압력 | `((b[3]>>2)+((b[4]&63)<<6))×0.1 bar`. `(b[4]&0xC0)==0` 요구. |
| `0x13` / 8 | 횡·종가속 | 아래 ESP12 식 사용. |
| `0x1C` / 4 | TCS·ESC·ABS | 아래 상태 비트 사용. |
| `0x20` / 6 | TPMS | 아래 바퀴 순서·상태·단위 사용. |
| `0x1D` / 8 | 선택 위치 | `b[1]&15`: 0=P, 7=R, 6=N, 5=D. 4/8/12는 수동 분기로 허용하지만 각 값의 실차 의미를 모두 규명한 것은 아님. |
| `0x1E` / 8 | 현재 단수 | `b[1]&15`: 1~7. 새 D/M 위치와 단수를 1초 이내로 조합. |

합성 예: RPM `b[2]=0xE0`, `b[3]=0x2E`이면 raw 12000, 표시 3000 rpm입니다. 속도 raw 100·단위 0이면 50 km/h입니다. 반면 signed 조향 `FFFF`는 −0.1°이므로 모든 필드에 동일한 FF 무효 규칙을 적용하면 안 됩니다.

### G-Force

```text
횡 raw = b[0] + ((b[1] & 7) << 8)
종 raw = (b[1] >> 5) + (b[2] << 3)
가속도 m/s² = raw × 0.01 − 10.23
G = 가속도 / 9.80665
```

횡은 `(b[1]&0x18)==0`, 종은 `(b[3]&3)==0`, 각각 raw <2047을 요구합니다. **ESP12의 차량 가속도 신호**이며 Android `SensorManager`나 PC 센서를 읽는 기능이 아닙니다.

### TCS·ESC·ABS

- TCS 해제 단계: `(b[0]>>1)&3`, 0=ON, 1=OFF. 그 외 값은 미확인.
- ESC 해제 단계: `b[1]&3`, 0·1=ON, 2=OFF. TCS만 해제한 단계에서 ESC까지 OFF로 표시하지 않습니다.
- ABS: `b[0]&1`은 **경고등**입니다. ABS 제어 개입 여부가 아닙니다.
- 전체 FF 레코드는 무효로 처리합니다.

### TPMS와 기어

TPMS `b[2]`~`b[5]`는 앞왼쪽·앞오른쪽·뒤왼쪽·뒤오른쪽입니다. 상태 `(b[1]&7)==1`, 단위 `(b[1]>>3)&3`을 확인합니다.

| 단위 코드 | raw 단위 | 앱의 psi 환산 |
| --- | --- | --- |
| 0 | psi | raw |
| 1 | 5 kPa | raw × 0.7251887 |
| 2 | 0.1 bar | raw × 1.4503774 |

각 바퀴의 raw 255는 해당 바퀴만 미확인으로 두고, 변환 후 100 psi 초과도 제외합니다. 실차 대조는 단위 0의 32/32/32/31 psi이며 단위 1·2는 호스트 변환 검사만 했습니다.

기어는 선택 위치 변경 전에 받은 단수를 폐기합니다. 신선한 단수가 없으면 D/M만 표시하며, RPM·속도 비율로 단수를 추측하지 않습니다. [PdGearState](../shared/src/com/veloster/vehicle/PdGearState.java).

## 계산값과 화면 표시

토크·출력·부스트는 단일 필드의 계측값이 아니라 수신 필드들을 조합한 **연구 계산값**입니다. [PdPowertrain](../shared/src/com/veloster/vehicle/PdPowertrain.java).

```text
기준토크 = (EMS12 b[0] & 63) × 10 N·m
  단, b[0] >> 6 == 3인 mux3에서만 갱신; raw 0/63 제외

순토크 N·m = (EMS11 b[1] − EMS11 b[5]) × 0.390625 × 기준토크 / 100
출력 PS = 순토크 × RPM × 2π / 60 / 735.49875

대기압 raw = (function 0x0A b[0] >> 3) & 31
대기압 mmHg = raw × 10.731613 + 458.98
절대압 raw = (function 0x0D b[2] >> 4) + (b[3] << 4)
절대압 hPa = raw × 1.322
게이지 부스트 bar = (절대압 hPa − 대기압 mmHg × 1.33322387415) / 1000
```

기준토크를 350 N·m 상수로 넣지 않고 mux3에서 받습니다. 기준토크는 3초, 압력 쌍은 1초 이내여야 합니다. 다른 mux 수신으로 기준토크의 시각을 갱신하지 않으며 결과에는 입력 중 오래된 시각을 사용합니다. EMS11 상태 마스크 `0xB2`, RPM raw 65535, 비율 255, 대기압 raw 31, 절대압 raw 4095 등의 무효 조건도 적용합니다. 음수 토크·부스트는 유효한 계산이면 유지합니다. 토크 단위 변환은 `1 kgf·m = 9.80665 N·m`이며 저장하는 토크와 출력 계산에 넣는 토크는 N·m로 유지합니다. 계산 출력의 단위는 PS입니다.

2026-11-12 main 패치는 새 RPM 0 수신 시 계산 토크·출력을 미확인으로 바꿉니다. RPM 0 자체의 표시는 유지하고 양수 공회전 RPM에서는 계산합니다. **이 수정은 정리 기준 시점의 소스에만 반영됐으며 공개 v1.2.0/code21 APK에는 포함되지 않았습니다.** [CHANGELOG](../CHANGELOG.md).

G-Force의 전후 점 위치 반전과 조향 표시 반전은 UI 정책입니다. 수신 숫자의 원래 부호와 분리합니다. 가속 측정은 CLU11의 AVN 수신 시각을 쓰므로 ECU 하드웨어 시각 기반 계측이 아닙니다. GPS 랩 타이머는 별도 Android 위치 제공자 경로입니다.

## 기록과 한계

당시 주행 수집은 목표 900초 전에 중단됐습니다. 보존된 구간은 약 808.547초, 5,236,176바이트, 완전 프레임 19,834개, 레코드 426,431개입니다. 보존 구간의 reject/discard/trailing partial은 0이지만 수집 작업 전체의 최종 상태는 실패이므로 900초 성공으로 기록하지 않습니다. 재생은 RPM·속도 각각 9,917회, RPM 621~4,845, 속도 0~74.5 km/h, 유온 유효 수신 0회를 보고했습니다. 원시 캡처와 기기 식별자는 공개하지 않습니다. [집계 검증 자료](../VALIDATION.json).

캡처는 소켓 바이트와 단조 증가 수신 시각을 보존했고, 재생은 실제 Java 디코더·모델에 입력했습니다. 수신 시각 없는 과거 바이너리의 가정 간격과 실제 기록 시각은 구분했습니다. 이 사설 연구용 수집·재생 도구는 공개 저장소에 포함하지 않았습니다. 공개 [모델·디코더 검사](../tests/)의 합성 입력도 실차 검증과 구분합니다.

| 항목 | 확인 범위 |
| --- | --- |
| RPM·속도·온도·페달·브레이크·조향 | 수신과 갱신 관측. 브레이크·조향은 정차 중 조작 반응도 확인. 센서 절대 정확도 교정은 아님. |
| 기어 | 사용자가 실차 P/R/N과 D1~D7 전체 표시 확인. |
| TCS·ESC | 사용자가 개별 ON/OFF 동작 확인. 실제 제어 개입 측정은 아님. |
| 유온 | 후보 `0x0D b[4]×0.75−40°C`가 시동 중에도 raw 0으로 고정돼 사용 가능한 값을 확보하지 못함. raw 0/255를 제외. 별도 OBD 진단기의 약 92°C 보고를 앱 값으로 대신 쓰지 않음. ECU 자체에 유온 정보가 없다는 뜻은 아님. |
| 배터리 | 후보 환산은 코드에 있지만 유효한 실차 SOC·전압은 미확립. |
| 휠 속도 | 순정 PD 테이블의 WHL_SPD11 function이 −1이고 파서가 비어 있어 해당 AVN 경로의 수신 근거 없음. |
| e-LSD | 확인 차량에 장착되지 않아 사용 불가. |

미수신·무효 값은 `-`로 둡니다. 일반 표시의 60초 stale 기준, 기어 1초·기준토크 3초·압력 1초 짝맞춤, 일부 상태·범위 배제는 **앱의 보수적 정책**입니다. 제조사 timeout이나 완전한 OEM 상태 명세로 보지 않습니다. 무효 새 입력은 해당 이전 값을 무효화합니다. 수신 클라이언트 시작·IGN 변경 시 모델 전체를 초기화하지만, 소켓 단절만으로 모든 값을 즉시 지우지는 않습니다. 남은 일반 값은 기존 시각과 stale·짝맞춤 기준을 따릅니다.

다른 차량에 적용할 때는 CAN ID만 복사하지 말고 해당 AVN의 function·길이·배열 구성·파서를 다시 확인해야 합니다. [검증 범위](PD_VALIDATION.md), [NOTICE](../NOTICE.md).

---

[English](PD_DATA_MAPPING.md) | **한국어** · [README](../README_ko.md)
