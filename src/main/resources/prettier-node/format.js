'use strict';

const path = require('path');
const fs = require('node:fs');

let prettierCache = null;
let javaPluginCache = null;

function loadPrettierModules() {
    if (prettierCache && javaPluginCache) return { prettier: prettierCache, javaPlugin: javaPluginCache };

    try {
        const prettierPath = path.join(__dirname, 'node_modules', 'prettier', 'index.js');
        // Plugin 1.x entry point is in dist/index.js
        const pluginPath = path.join(__dirname, 'node_modules', 'prettier-plugin-java', 'dist', 'index.js');
        
        console.log('JS: Loading Prettier 2 (CJS) from ' + prettierPath);
        prettierCache = require(prettierPath);
        
        console.log('JS: Loading Plugin (CJS) from ' + pluginPath);
        javaPluginCache = require(pluginPath);

        return { prettier: prettierCache, javaPlugin: javaPluginCache };
    } catch (e) {
        console.error('JS: loadPrettierModules error: ' + e.message);
        throw new Error('Failed to load prettier modules: ' + e.message);
    }
}

/**
 * Formats the given Java code (Prettier 2 / Plugin 1.x Version).
 */
async function formatCode(code, optionsJson) {
    console.log('JS: [V5-FINAL] formatCode starting (Stable CJS path)');
    const { prettier, javaPlugin } = loadPrettierModules();

    let intellijOptions = {};
    if (optionsJson) {
        try {
            intellijOptions = JSON.parse(optionsJson);
        } catch (e) {
            console.error('JS: Options parse error');
            throw new Error('Invalid options JSON: ' + e.message);
        }
    }

    const { filePath: optFilePath, ...userOverrides } = intellijOptions;

    console.log('JS: Resolving config...');
    const resolvedConfig = optFilePath
        ? (prettier.resolveConfig.sync(optFilePath, { editorconfig: true }) ?? {})
        : {};

    console.log('JS: Executing prettier.format (Sync)...');
    // Prettier 2 and Java Plugin 1.x are synchronous
    const formatted = prettier.format(code, {
        ...resolvedConfig,
        ...userOverrides,
        parser: 'java',
        plugins: [javaPlugin],
    });
    console.log('JS: Formatting done, success: ' + !!formatted);

    return formatted;
}

module.exports = {
    formatCode
};
