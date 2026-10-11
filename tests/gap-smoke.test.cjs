const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const vm = require("node:vm");

const html = fs.readFileSync("index.html", "utf8");
const scripts = [...html.matchAll(/<script\b[^>]*>([\s\S]*?)<\/script>/gi)]
  .map(match => match[1])
  .filter(source => source.trim());

test("alle Inline-JavaScript-Blöcke lassen sich parsen", () => {
  assert.ok(scripts.length >= 2, "Hauptskripte wurden nicht gefunden");
  for (const source of scripts) assert.doesNotThrow(() => new Function(source));
});

test("Notendurchschnitt filtert auf benotete große Sektorprüfungen", () => {
  assert.match(html, /x\?\.type==="Sektorprüfung"&&x\?\.restoreData\?\.size==="large"/);
  assert.match(html, /id="gradeAverage"/);
  assert.match(html, /id="gradeCount"/);
});

test("Backup-Import zeigt eine Vorschau vor dem Ersetzen", () => {
  assert.match(html, /Backup-Vorschau/);
  assert.match(html, /downloadBackup\(\);db=normalize\(parsed\.data\)/);
  assert.match(html, /id="personalStreak"/);
});

test("persönliche Statistiken werden gerendert und besitzen alle Ausgabefelder", () => {
  assert.match(html, /function renderPersonalStats\(\)/);
  assert.match(html, /renderPersonalStats\(\);document\.getElementById\("progressRank"\)/);
  for (const id of ["personalStreak", "personalActiveDays", "personalCompletedWeek", "personalBestDay", "personalBestDayDate"]) {
    assert.match(html, new RegExp('id="' + id + '"'));
  }
});

test("Statistikberechnung erkennt Aktivitätsserie, Wochenabschlüsse und XP-Tagesrekord", () => {
  const start = html.indexOf("function renderPersonalStats(){");
  const end = html.indexOf("function renderProgress(){", start);
  assert.ok(start >= 0 && end > start, "Statistikfunktion konnte nicht isoliert werden");
  const fn = html.slice(start, end);
  const now = new Date();
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  const isoDay = offset => new Date(today.getTime() + offset * 86400000).toISOString();
  const values = {};
  const context = {
    db: {
      activityLog: [{date: isoDay(-2)}, {date: isoDay(-1)}, {date: isoDay(0)}],
      completionLog: [{type: "Sektoraufgabe", date: isoDay(0)}, {type: "Tutorial", date: isoDay(0)}],
      xpLog: [{date: isoDay(-1), amount: 20}, {date: isoDay(0), amount: 35}]
    },
    el: id => ({set textContent(value) { values[id] = value; }})
  };
  vm.runInNewContext(fn + "\nrenderPersonalStats();", context);
  assert.equal(values.personalStreak, "3 Tage");
  assert.equal(values.personalActiveDays, "3");
  assert.equal(values.personalCompletedWeek, "1");
  assert.equal(values.personalBestDay, "35");
});
