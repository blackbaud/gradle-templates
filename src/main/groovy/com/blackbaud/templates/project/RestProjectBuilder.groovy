package com.blackbaud.templates.project

class RestProjectBuilder {

    File repoDir
    String name
    boolean mybatis = false
    boolean postgres = false
    boolean kafka = false
    boolean clean = false
    boolean disableAuthFilter = false

    private RestProjectBuilder() {}

    public static RestProjectBuilder getInstance() {
        new RestProjectBuilder();
    }

    public RestProjectBuilder repoDir(File repoDir) {
        this.repoDir = repoDir
        this
    }

    public RestProjectBuilder name(String name) {
        this.name = name;
        this
    }

    public RestProjectBuilder useMybatis() {
        this.mybatis = true
        this
    }

    public RestProjectBuilder usePostgres() {
        this.postgres = true
        this
    }

    public RestProjectBuilder useKafka() {
        this.kafka = true
        this
    }

    public RestProjectBuilder clean() {
        this.clean = true
        this
    }

    RestProjectBuilder disableAuthFilter() {
        this.disableAuthFilter = true
        this
    }

    public RestProject build() {
        BasicProject basicProject = createBasicProject()
        RestProject restProject = new RestProject(basicProject)
        restProject.initRestProject(disableAuthFilter)
        if (mybatis) {
            restProject.initPostgres()
            restProject.initMybatis()
        } else if (postgres) {
            restProject.initPostgres()
        }
        if (kafka) {
            restProject.initKafka()
        }
        restProject
    }

    private BasicProject createBasicProject() {
        BasicProjectBuilder basicProjectBuilder = BasicProjectBuilder.getInstance()
                .name(name)
                .repoDir(repoDir)
        if(clean) {
            basicProjectBuilder.clean()
        }
        basicProjectBuilder.build()
    }

}
