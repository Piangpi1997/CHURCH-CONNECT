# Localization Guide

The app has app-owned English and Myanmar (`my`) Android string catalogs, and a persisted language selector is available before sign-in and in More. The selector supports **System default**, English, Myanmar, and **Tedim (English until reviewed)**. Android 13+ per-app language settings list the same language tags. System default follows the device/app locale when no explicit language is selected; selection is stored locally and applied before Activity resources load.

Compose-owned labels, workflow statuses, common validation and success messages, date formatting, notification fallbacks, and accessibility text are resource-backed. The app title remains **CMF CHURCH APP**. Member-entered names, church-published event/announcement content, QR verification messages from the server, and unrecognized provider/backend errors are not automatically translated. English is the fallback for any missing locale key.

## Myanmar review

The Myanmar catalog has source translations for the app-owned UI. Before a church rollout, a native Myanmar reader should review terminology, tone, numerals and fees, small-screen wrapping, TalkBack wording, and Unicode rendering on the actual supported Android devices. This local source-only task did not include a device-based language review.

## Tedim review

Tedim (`ctd`) has not been translated. Its resource catalog contains explicit English fallback copies; the file comment and language selector state that they are English, not Tedim. This keeps runtime behavior and resource coverage deterministic without representing English as a translation. Do not add guessed Tedim copy: a trusted church-language reviewer must supply and approve translations, including membership, finance, privacy, status, and safety-critical language, before replacing the fallback or claiming Tedim support.

## Adding or reviewing strings

1. Add the English key to `app/src/main/res/values/strings.xml` first.
2. Add an approved Myanmar translation to `values-my/strings.xml` and confirm key parity.
3. Keep the documented Tedim fallback catalog aligned with the English keys and placeholders. Replace its text only from a church-reviewed source in `values-b+ctd/strings.xml`; until then its values must remain identical English fallback copy.
4. Keep formatting placeholders (`%1$s`, `%2$d`, etc.) identical across locales, and test plural counts.
5. Verify the language chooser, persistence across process restart, system-default behavior, and fallback on an Android device. Review long labels, Burmese Unicode shaping, RTL-independent layout, screen readers, and date/fee displays.

Suggested parity check:

```bash
python3 - <<'PY'
import xml.etree.ElementTree as ET
from pathlib import Path
r = Path('app/src/main/res')
def keys(path):
    return {e.attrib['name'] for e in ET.parse(r / path).getroot() if e.attrib.get('name')}
assert keys('values/strings.xml') == keys('values-my/strings.xml')
print('English/Myanmar resource keys match')
PY
```
