#!/usr/bin/env node
/**
 * format.js — Prettier Java runner for the IntelliJ plugin.
 *
 * Matches the behavior of RudraPatel's prettier-plugin-java-vscode:
 *   - Calls prettier.resolveConfig(filePath) to pick up .prettierrc from the project
 *   - IntelliJ user overrides (from Settings) take priority over .prettierrc values
 *
 * Protocol (avoids Windows CLI quoting issues):
 *   stdin line 1: JSON { filePath?, printWidth?, tabWidth?, ... }
 *   stdin rest:   Java source code to format
 *
 * Output: formatted Java to stdout | error to stderr (exit 1)
 */

'use strict';

const path   = require('path');
const Module = require('module');

// Require scoped to our extracted node_modules
const nodeRequire = Module.createRequire(
    path.join(__dirname, 'node_modules', 'prettier', 'package.json')
);

function readStdin() {
    return new Promise((resolve, reject) => {
        let data = '';
        process.stdin.setEncoding('utf8');
        process.stdin.on('data', chunk => { data += chunk; });
        process.stdin.on('end', () => resolve(data));
        process.stdin.on('error', reject);
    });
}

async function main() {
    const input = await readStdin();

    const newlineIdx = input.indexOf('\n');
    if (newlineIdx === -1) {
        process.stderr.write('Invalid input: missing options line\n');
        process.exit(1);
    }

    const optionsLine = input.substring(0, newlineIdx);
    const code        = input.substring(newlineIdx + 1);

    // Parse options sent from IntelliJ
    let intellijOptions = {};
    try {
        intellijOptions = JSON.parse(optionsLine);
    } catch (e) {
        process.stderr.write('Invalid options JSON: ' + e.message + '\n');
        process.exit(1);
    }

    // Extract filePath from options (used for resolveConfig)
    const { filePath, ...userOverrides } = intellijOptions;

    let prettier, javaPlugin;
    try {
        prettier = nodeRequire('prettier');
        const _pluginModule = nodeRequire('prettier-plugin-java');
        javaPlugin = _pluginModule.default ?? _pluginModule;
    } catch (e) {
        process.stderr.write('Failed to load prettier modules: ' + e.message + '\n');
        process.exit(1);
    }

    try {
        // Resolve .prettierrc from the project — same as VS Code plugin does
        // Falls back to null (Prettier defaults) if no config found
        const resolvedConfig = filePath
            ? (await prettier.resolveConfig(filePath, { editorconfig: true }) ?? {})
            : {};

        // Merge: Prettier defaults ← .prettierrc ← IntelliJ Settings overrides
        const formatted = await prettier.format(code, {
            ...resolvedConfig,
            ...userOverrides,
            parser:  'java',
            plugins: [javaPlugin],
        });

        process.stdout.write(formatted);
    } catch (e) {
        process.stderr.write(e?.message ?? String(e));
        process.stderr.write('\n');
        process.exit(1);
    }
}

main().catch(e => {
    process.stderr.write(e?.message ?? String(e));
    process.stderr.write('\n');
    process.exit(1);
});
