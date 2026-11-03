# PD 표시 문자열 수정

한국어·영어 표시 문자열은 `ko.json`·`en.json`에서 관리합니다. 수정 후 저장소 루트에서 다음을 실행합니다.

```sh
python3 scripts/generate_translations.py
python3 scripts/generate_translations.py --check
```

생성 대상은 `shared/src/com/veloster/i18n/UiText.java`입니다. 앱의 수동 언어 선택은 각 패키지 설정에 저장되며 시스템 언어를 바꾸지 않습니다. 텔레메트리 식별자·단위·수신 명령·설정 키를 번역용으로 변경하지 않습니다. 앱 이름 리소스는 각 앱의 `res/values`·`res/values-ko`에도 있습니다. 전체 검사는 [빌드 안내](../docs/BUILD.md)를 따릅니다.
