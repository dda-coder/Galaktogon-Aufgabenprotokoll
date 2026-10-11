import test from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";

const root = dirname(fileURLToPath(import.meta.url));
const html = readFileSync(join(root, "..", "index.html"), "utf8");

test("HTML document and inline script tags are balanced", () => {
  assert.equal((html.match(/<script\b/g) || []).length, (html.match(/<\/script>/g) || []).length);
  assert.match(html, /<\/html>\s*$/);
});

test("backup controls and validation are present", () => {
  for (const id of ["exportBackupBtn", "importBackupBtn", "importBackupFile", "backupStatus"]) {
    assert.ok(html.includes('id="' + id + '"'), "Missing backup element: " + id);
  }
  assert.match(html, /file\.size>5\*1024\*1024/);
  assert.match(html, /parsed\?\.app!=="GALAKTOGON AUFGABENPROTOKOLL"/);
  assert.match(html, /Backup wirklich importieren\?/);
});

test("manual sync reports its actual result", () => {
  assert.match(html, /const ok=await saveNow\(\)/);
  assert.match(html, /Synchronisierung erfolgreich/);
  assert.match(html, /Synchronisierung fehlgeschlagen/);
  assert.doesNotMatch(html, /Synchronisierung wurde angestoßen/);
});

test("active task lists have wired search filters", () => {
  for (const id of ["taskSearch", "examSearch", "questSearch", "recurringSearch"]) {
    assert.ok(html.includes('id="' + id + '"'), "Missing search field: " + id);
    assert.ok(html.includes('el("' + id + '")?.addEventListener("input"'), "Search field is not wired: " + id);
  }
  assert.match(html, /function queryMatches\(item,query,fields\)/);
});

test("exam reminders use the configured preference and avoid daily duplicates", () => {
  assert.match(html, /async function checkExamReminders\(\)/);
  assert.match(html, /settings\.examReminder/);
  assert.match(html, /gap_exam_reminder_/);
  assert.match(html, /registration\.showNotification\(title,options\)/);
});

test("past appointments are not falsely marked completed", () => {
  assert.match(html, /status=a\.completed\?'<span class="tag green">ERLEDIGT<\/span>':past\?'<span class="tag red">VERSÄUMT<\/span>'/);
  assert.doesNotMatch(html, /done=Boolean\(a\.completed\)\|\|past/);
});

test("grade and subject statistics plus trust breakdown are present", () => {
  for (const id of ["gradeAverage", "gradeRecentAverage", "gradeTrend", "gradeHistory", "subjectXPStats", "rankHistory"]) {
    assert.ok(html.includes('id="' + id + '"'), "Missing statistics element: " + id);
  }
  assert.match(html, /xpScore:Math\.round\(xpScore\)/);
  assert.match(html, /category:"Effekt"/);
});

test("floating navigation refreshes for account roles and active groups", () => {
  assert.match(html, /function enterApp\(\)[\s\S]*?renderDock\(\);[\s\S]*?showPage\(page,false\)/);
  assert.match(html, /currentAccountType="child";currentProfile=null;activeChildId=null;activeChildUsername="";linkedChildren=\[\];\s*renderDock\(\);/);
  assert.match(html, /const groups=dockGroups\[currentAccountType==="parent"\?"parent":"child"\]/);
  assert.match(html, /group\.items\?\.some\(\(\[page\]\)=>page===p\)/);
  for (const page of ["dashboard", "tasks", "exams", "quests", "recurring", "appointments", "activities", "progress", "ranks", "leaderboard", "parents", "parentTasks", "parentExams", "parentQuests", "parentEffects", "parentLogs", "parentReview", "settings", "updates"]) {
    assert.ok(html.includes('id="' + page + '"'), "Navigation target missing: " + page);
  }
});
