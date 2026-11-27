import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const rootDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const targets = [
    path.join(rootDir, 'build/tasks/_webApp_buildWasmJsAppWasmJsRelease/skiko.mjs'),
    path.join(rootDir, 'build/tasks/_webApp_buildWasmJsAppWasmJsDebug/skiko.mjs'),
];

const pattern = /^export let ([A-Za-z0-9_$]+) = \(\.\.\.a\) => \(\1 = (loadedWasm\._\["\1"\])\)\(\.\.\.a\)$/gm;

let patchedFiles = 0;

for (const filePath of targets) {
    if (!fs.existsSync(filePath)) continue;
    const original = fs.readFileSync(filePath, 'utf8');
    let count = 0;
    const updated = original.replace(pattern, (_, name, target) => {
        count++;
        return `export let ${name} = (...a) => { try { return (${name} = ${target})(...a); } catch (e) { throw e; } }`;
    });
    if (count > 0) {
        fs.writeFileSync(filePath, updated, 'utf8');
        patchedFiles++;
        console.log(`Patched ${count} exports in ${path.relative(rootDir, filePath)}`);
    } else {
        console.log(`No unpatched tail-call exports in ${path.relative(rootDir, filePath)}`);
    }
}

if (patchedFiles === 0) {
    console.log('Done (nothing needed patching).');
}
