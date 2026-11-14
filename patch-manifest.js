const fs = require("fs");
const p = "android/app/src/main/AndroidManifest.xml";
let s = fs.readFileSync(p, "utf8");
const perms = [
  "android.permission.SCHEDULE_EXACT_ALARM",
  "android.permission.USE_EXACT_ALARM",
  "android.permission.POST_NOTIFICATIONS",
  "android.permission.RECEIVE_BOOT_COMPLETED",
  "android.permission.WAKE_LOCK",
  "android.permission.VIBRATE"
];
perms.forEach(function (perm) {
  if (s.indexOf(perm) === -1) {
    s = s.replace("</manifest>", '    <uses-permission android:name="' + perm + '" />\n</manifest>');
  }
});
fs.writeFileSync(p, s);
console.log("manifest patched with " + perms.length + " permissions");
