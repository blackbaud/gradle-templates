package com.blackbaud.templates

import com.blackbaud.templates.tasks.FileUtils


class ProjectFile extends File {

    ProjectFile(File file) {
        super(file.toURI())
    }

    void addImport(String importPath) {
        FileUtils.addImport(this, importPath)
    }

    void addConfigurationImport(String importPath) {
        FileUtils.addConfigurationImport(this, importPath)
    }

    void appendToClass(String textToAppend) {
        FileUtils.appendToClass(this, textToAppend)
    }

}
