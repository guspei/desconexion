#!/bin/sh
# Builds, installs and grants everything over adb. Needs the phone with USB debugging on.
set -e
cd "$(dirname "$0")"
PKG=ai.askpancho.desconexion

./apk.sh
adb install -r desconexion.apk

# Airplane mode, grayscale and keeping the watcher on
adb shell pm grant $PKG android.permission.WRITE_SECURE_SETTINGS
adb shell appops set $PKG GET_USAGE_STATS allow
adb shell pm grant $PKG android.permission.POST_NOTIFICATIONS 2>/dev/null || true
adb shell cmd notification allow_listener $PKG/$PKG.Silence

cur=$(adb shell settings get secure enabled_accessibility_services | tr -d '\r')
case "$cur" in
  *"$PKG"*) ;;
  ""|null) adb shell settings put secure enabled_accessibility_services "$PKG/$PKG.Watcher" ;;
  *) adb shell settings put secure enabled_accessibility_services "$cur:$PKG/$PKG.Watcher" ;;
esac
adb shell settings put secure accessibility_enabled 1

adb shell cmd role add-role-holder android.app.role.HOME $PKG 2>/dev/null ||
  adb shell cmd package set-home-activity $PKG/.HomeActivity

# Keeps the phone's power manager from freezing the app (it does no background work of its own)
adb shell cmd appops set $PKG RUN_ANY_IN_BACKGROUND allow
adb shell dumpsys deviceidle whitelist +$PKG >/dev/null

adb shell am start -n $PKG/.HomeActivity >/dev/null
echo listo
