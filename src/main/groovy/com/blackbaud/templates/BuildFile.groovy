package com.blackbaud.templates


class BuildFile extends ProjectFile {

    BuildFile(File file) {
        super(file)
    }

    void applyPlugin(String pluginName) {
        appendAfterLine(/apply\s+plugin:\s+"blackbaud-internal/, /apply plugin: "${pluginName}"/)
    }

}
