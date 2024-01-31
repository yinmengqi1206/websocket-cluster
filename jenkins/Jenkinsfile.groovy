#!groovy
import groovy.transform.Field

def createVersion() {
    // 定义一个版本号作为当次构建的版本，输出结果 69_20191210175842
    return "${env.BUILD_ID}_" + new Date().format('yyyyMMddHHmmss')
}
// def和@Field定义变量，可以从同一脚本中定义的方法直接访问，不然定义的方法访问不了
@Field
def MY_BUILD_MODE_JAVA = "JAVA"
@Field
def MY_BUILD_MODE_H5 = "H5"
@Field
def BUILD_STAGE_DEFINE = [
        init           : '初始化',
        cleanWorkspace : '清理工作空间',
        checkout       : 'checkout代码',
        javaCompile    : 'java-编译代码',
        h5Compile      : 'h5-编译代码',
        javaAutoTest   : 'java-自动测试',
        createServerDir: '创建服务器相关目录',
        javaCopy       : 'java-拷贝代码',
        h5Copy         : 'h5-拷贝代码',
        backup         : '备份服务代码',
        javaStop       : 'java-停止服务',
        javaDeploy     : 'java-部署代码',
        h5Deploy       : 'h5-部署代码',
        javaStart      : 'java-启动服务',
        h5Start        : 'h5-reload nginx服务',
        deleteBackup   : '删除备份保留30个',
]
@Field
def BUILD_STAGE_DEFINE_KEYS = BUILD_STAGE_DEFINE.keySet().toArray(new String[0]).join(",")
@Field
def BUILD_STAGE_DEFINE_VALUES = BUILD_STAGE_DEFINE.values().toArray(new String[0]).join(",")
@Field
def BUILD_STAGE_JAVA = "init,checkout,javaCompile,createServerDir,javaCopy,backup,javaStop,javaDeploy,javaStart"
@Field
def BUILD_STAGE_H5 = "init,checkout,h5Compile,createServerDir,h5Copy,backup,h5Deploy,h5Start"
@Field
def BUILD_STAGE_DEFAULT = BUILD_STAGE_JAVA
@Field
def MY_BASE_PATH = '/data/project-service/'
pipeline {
    //在任何可用的代理上执行流水线
    agent any
    // 可能需要修改的变量
    parameters {
        string(name: 'MY_GIT_URL', defaultValue: 'https://github.com/yinmengqi1206/websocket-cluster.git', description: 'Required: gitlab地址http开头')
        string(name: 'MY_GIT_AUTH', defaultValue: 'github', description: 'Required: gitlab登录凭证默认就行')
        string(name: 'MY_MAVEN_CONFIG_FILE', defaultValue: 'maven', description: 'Required: maven setting.xml id')
        // 安装GitBranches插件后使用GitBranches插件
        listGitBranches(name: 'MY_BRANCH', defaultValue: 'main', branchFilter: 'refs/heads/(.*)', type: 'PT_BRANCH', remoteURL: "${MY_GIT_URL}", credentialsId: "${MY_GIT_AUTH}", selectedValue: 'DEFAULT', sortMode: 'DESCENDING_SMART', listSize: '20')
        choice(name: 'MY_BUILD_MODE', choices: ["${MY_BUILD_MODE_JAVA}", "${MY_BUILD_MODE_H5}"], description: 'Required: 构建mode：JAVA,H5')
        choice(name: 'MY_REMOTE_HOST', choices: ["ubuntu-linux"], description: 'Required: 部署的机器的内网ip')
        // 安装Extended Choice Parameter
        extendedChoice(name: 'MY_STAGE', defaultValue: "${BUILD_STAGE_DEFAULT}", value: "${BUILD_STAGE_DEFINE_KEYS}", descriptionPropertyValue: "${BUILD_STAGE_DEFINE_VALUES}", description: "stage服务编排", multiSelectDelimiter: ',', quoteValue: false, type: 'PT_CHECKBOX', visibleItemCount: 30)
        string(name: 'MY_MAVEN_CMD', defaultValue: 'mvn clean install -U -Dmaven.test.skip=true', description: 'Optional(when MY_BUILD_MODE is JAVA): maven执行命令')
        text(name: 'MY_NODEJS_CMD', defaultValue: 'node -v\nnpm -v\npnpm -v\nnpm install\nnpm run build:test', description: 'Optional(when MY_BUILD_MODE is H5): node 命令')
        //string(name: 'MY_BRANCH', defaultValue: '*/master', description: 'Required: 分支名称')
        string(name: 'MY_SERVICE_NAME', defaultValue: 'gateway', description: 'Required: 构建服务名称')
        string(name: 'MY_BUILD_PATH', defaultValue: './gateway/', description: 'Required: 相对目录工作空间中的构建目录，默认为./当前目录')
        string(name: 'MY_TARGET_PATH', defaultValue: 'target/', description: 'Required: 相对构建目录中的构建产物目录，默认为target/')
        string(name: 'MY_SPRING_ARGS', defaultValue: "--spring.profiles.active=dev --my.log.path=${MY_BASE_PATH}logs/", description: 'Required: spring--形式的配置参数')
        string(name: 'MY_JVM_ARGS', defaultValue: '-Xms512m -Xmx512m -Xss256k -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=256m -XX:MaxDirectMemorySize=128m -XX:+ShowCodeDetailsInExceptionMessages', description: 'Required: java虚拟机参数')
    }

    tools {
        maven 'maven'
        jdk 'jdk-17'
    }
    options {
        // 保留多少个流水线的构建记录
        buildDiscarder(logRotator(
                daysToKeepStr: '30',
                numToKeepStr: '30'
        ))
        // 禁止流水线并行执行，防止并行流水线同时访问共享资源导致流水线失败
        disableConcurrentBuilds()
        // 超时时间10分钟，如果不加unit参数默认为1分
        timeout(time: 10, unit: 'MINUTES')
        // 所有输出每行都会打印时间戳
        timestamps()
    }
    // jenkins内置变量，可以使用 env.BUILD_ID引用
    //BUILD_ID：当前构建的 ID，与 Jenkins 版本 1.597+中的 BUILD_NUMBER 完全相同
    //BUILD_NUMBER：当前构建的 ID，和 BUILD_ID 一致
    //BUILD_TAG：用来标识构建的版本号，格式为：jenkins-{BUILD_NUMBER}， 可以对产物进行命名，比如生产的 jar 包名字、镜像的 TAG 等；
    //BUILD_URL：本次构建的完整 URL，比如：http://buildserver/jenkins/job/MyJobName/17/%EF%BC%9B
    //JOB_NAME：本次构建的项目名称
    //NODE_NAME：当前构建节点的名称；
    //JENKINS_URL：Jenkins 完整的 URL，需要在 SystemConfiguration 设置；
    //WORKSPACE：执行构建的工作目录。
    //Jenkinsfile (Declarative Pipeline)
    // 全局变量，会在所有stage中生效
    environment {
        GIT_URL = "${MY_GIT_URL}"
        GIT_AUTH = "${MY_GIT_AUTH}"
        MAVEN_TOOL_NAME = 'maven'
        JAVA_TOOL_NAME = 'jdk-17'
        MAVEN_REPOSITORY = '/var/jenkins_home/repository'
        MAVEN_CONFIG_FILE = "${MY_MAVEN_CONFIG_FILE}"
        // 远程服务器路径
        BASE_PATH = "${MY_BASE_PATH}"
        BACKUP_PATH = "${BASE_PATH}" + 'backup/'
        COPY_PATH = "${BASE_PATH}" + 'deploy/'
        DEPLOY_PATH = "${BASE_PATH}" + 'service/'
        DUMP_PATH = "${BASE_PATH}" + 'dump/'
        LOGS_PATH = "${BASE_PATH}" + 'logs/'
        JAVA_CMD = '/usr/lib/jvm/java-17-openjdk-amd64/bin/java'
        // 可能需要修改的变量
        // 构建mode：java,h5
        BUILD_MODE = "${params.MY_BUILD_MODE}"
        MAVEN_CMD = "${params.MY_MAVEN_CMD}"
        NODEJS_CMD = "${params.MY_NODEJS_CMD}"
        // 分支名称
        BRANCH = "${params.MY_BRANCH}"
        // 部署服务名称
        SERVICE_NAME = "${params.MY_SERVICE_NAME}"
        // 相对目录工作空间中的构建目录，默认为./当前目录
        BUILD_PATH = "${params.MY_BUILD_PATH}"
        // 相对构建目录中的构建产物目录，默认为target/
        TARGET_PATH = "${params.MY_TARGET_PATH}"
        SPRING_ARGS = "${params.MY_SPRING_ARGS}"
        JVM_ARGS = "${params.MY_JVM_ARGS}"

        REMOTE_HOST = "${MY_REMOTE_HOST}"
        SSH_PORT = '22'
        USER_NAME = 'root'
        JAVA_OPT = " -Dfile.encoding=UTF-8 ${JVM_ARGS} " +
                "-XX:+UseG1GC " +
                "-XX:HeapDumpPath=${DUMP_PATH}${SERVICE_NAME}/ " +
                "-XX:+HeapDumpOnOutOfMemoryError " +
                "-XX:+DisableExplicitGC " +
                "-XX:MaxGCPauseMillis=100 " +
                // gc log
                "-Xlog:safepoint,gc*" +
                // output
                ":file=${LOGS_PATH}${SERVICE_NAME}/${SERVICE_NAME}-%t-gc.log" +
                // decorators
                ":time,level,tags,uptime,pid" +
                // output-options
                ":filesize=104857600,filecount=5"

        TIME_VERSION = createVersion()
        BACKUP_MAX_NUMBER = 30
    }
    stages {

        stage('初始化') {
            when {
                allOf {
                    expression { params.MY_STAGE.contains("init") }
                }
            }
            steps {
                echo "env.JOB_NAME:${env.JOB_NAME}"
                echo "env.WORKSPACE:${env.WORKSPACE}"
                echo "param.MY_STAGE:${params.MY_STAGE}"

            }
        }

        stage('清理工作空间') {
            when {
                allOf {
                    expression { params.MY_STAGE.contains("cleanWorkspace") }
                }
            }
            steps {
                echo "清理工作空间"
                cleanWs()
            }
        }
        stage('checkout代码') {
            when {
                allOf {
                    expression { params.MY_STAGE.contains("checkout") }
                }
            }
            steps {
                echo 'checkout代码'
                checkout([$class                           : 'GitSCM',
                          branches                         : [[name: "${BRANCH}"]],
                          doGenerateSubmoduleConfigurations: false,
                          extensions                       : [[$class: 'CheckoutOption', timeout: 2]],
                          submoduleCfg                     : [],
                          userRemoteConfigs                : [[credentialsId: "${GIT_AUTH}", url: "${GIT_URL}"]]
                ])
            }

        }

        stage('java-编译代码') {
            when {
                allOf {
                    expression { params.MY_BUILD_MODE == "${MY_BUILD_MODE_JAVA}" }
                    expression { params.MY_STAGE.contains("javaCompile") }
                }
            }
            steps {
                echo 'java-编译代码'
                // 安装Pipeline Maven Integration
                withMaven(
                        // Maven installation declared in the Jenkins "Global Tool Configuration"
                        maven: "${MAVEN_TOOL_NAME}", // (1)
                        jdk: "${JAVA_TOOL_NAME}",
                        // Use `$WORKSPACE/.repository` for local repository folder to avoid shared repositories
                        mavenLocalRepo: "${MAVEN_REPOSITORY}", // (2)
                        // Maven settings.xml file defined with the Jenkins Config File Provider Plugin
                        // We recommend to define Maven settings.xml globally at the folder level using
                        // navigating to the folder configuration in the section "Pipeline Maven Configuration / Override global Maven configuration"
                        // or globally to the entire master navigating to  "Manage Jenkins / Global Tools Configuration"
                        // mavenSettingsFilePath: "${MAVEN_CONFIG_FILE}" // (3)
                        mavenSettingsConfig: "${MAVEN_CONFIG_FILE}"
                ) {
                    sh "${MAVEN_CMD}"
                }

            }

        }
        stage('h5-编译代码') {
            when {
                allOf {
                    expression { params.MY_BUILD_MODE == "${MY_BUILD_MODE_H5}" }
                    expression { params.MY_STAGE.contains("h5Compile") }
                }
            }
            steps {
                echo 'h5-编译代码'
                script {
                    def node_build_cmd = "${NODEJS_CMD}".tokenize('\n').join(';')
                    echo "${node_build_cmd}"
                    sh "${node_build_cmd}"
                }
            }

        }
        stage('java-自动测试') {
            when {
                allOf {
                    expression { params.MY_BUILD_MODE == "${MY_BUILD_MODE_JAVA}" }
                    expression { params.MY_STAGE.contains("javaAutoTest") }
                }
            }
            steps {
                echo 'java-自动测试'
                echo '跳过自动测试'
            }

        }
        stage('创建服务器相关目录') {
            when {
                allOf {
                    expression { params.MY_STAGE.contains("createServerDir") }
                }
            }
            steps {
                echo '创建服务器相关目录'
                sh "ssh -i /var/jenkins_home/id_rsa -p ${SSH_PORT} ${USER_NAME}@${REMOTE_HOST} rm -rf  ${COPY_PATH}${SERVICE_NAME}"
                echo '部署服务器建立相关目录'
                sh "ssh -i /var/jenkins_home/id_rsa -p ${SSH_PORT} ${USER_NAME}@${REMOTE_HOST} mkdir -p ${BASE_PATH} ${BACKUP_PATH}${SERVICE_NAME} ${COPY_PATH}${SERVICE_NAME} ${DEPLOY_PATH}${SERVICE_NAME} ${DUMP_PATH}${SERVICE_NAME} ${LOGS_PATH}${SERVICE_NAME}"

            }
        }
        stage('java-拷贝代码') {
            when {
                allOf {
                    expression { params.MY_BUILD_MODE == "${MY_BUILD_MODE_JAVA}" }
                    expression { params.MY_STAGE.contains("javaCopy") }
                }
            }
            steps {
                echo 'java-拷贝代码'
                sh "scp -i /var/jenkins_home/id_rsa -r -P ${SSH_PORT} ${BUILD_PATH}${TARGET_PATH}*.jar  ${USER_NAME}@${REMOTE_HOST}:${COPY_PATH}${SERVICE_NAME}/"

            }
        }
        stage('h5-拷贝代码') {
            when {
                allOf {
                    expression { params.MY_BUILD_MODE == "${MY_BUILD_MODE_H5}" }
                    expression { params.MY_STAGE.contains("h5Copy") }
                }
            }
            steps {
                echo '拷贝代码'
                sh "scp -i /var/jenkins_home/id_rsa -r -P ${SSH_PORT} ${BUILD_PATH}${TARGET_PATH}*  ${USER_NAME}@${REMOTE_HOST}:${COPY_PATH}${SERVICE_NAME}/"

            }
        }
        stage('备份服务代码') {
            when {
                allOf {
                    expression { params.MY_STAGE.contains("backup") }
                }
            }
            steps {
                echo '备份服务代码'
                catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
                    sh "ssh -i /var/jenkins_home/id_rsa -p ${SSH_PORT} ${USER_NAME}@${REMOTE_HOST}  \"cd ${DEPLOY_PATH}${SERVICE_NAME}&&tar -cvf ${BACKUP_PATH}${SERVICE_NAME}/${SERVICE_NAME}-${TIME_VERSION}.tar *\""
                }
            }
        }
        stage('java-停止服务') {
            when {
                allOf {
                    expression { params.MY_BUILD_MODE == "${MY_BUILD_MODE_JAVA}" }
                    expression { params.MY_STAGE.contains("javaStop") }
                }
            }
            steps {
                echo 'java-停止服务'
                catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
                    sh '''ssh -i /var/jenkins_home/id_rsa -p ${SSH_PORT} ${USER_NAME}@${REMOTE_HOST} "ps -ef | grep gateway | awk '{print \\$2}' | xargs kill -9" '''
                    sleep(time: 2, unit: "SECONDS")
                }
            }
        }
        stage('java-部署代码') {
            when {
                allOf {
                    expression { params.MY_BUILD_MODE == "${MY_BUILD_MODE_JAVA}" }
                    expression { params.MY_STAGE.contains("javaDeploy") }
                }
            }
            steps {
                echo 'java-部署代码'
                sh "ssh -i /var/jenkins_home/id_rsa -p ${SSH_PORT} ${USER_NAME}@${REMOTE_HOST}  \"rm -rf ${DEPLOY_PATH}${SERVICE_NAME}/*&&cp -r ${COPY_PATH}${SERVICE_NAME}/*.jar ${DEPLOY_PATH}${SERVICE_NAME}/${SERVICE_NAME}.jar\""
            }
        }
        stage('h5-部署代码') {
            when {
                allOf {
                    expression { params.MY_BUILD_MODE == "${MY_BUILD_MODE_H5}" }
                    expression { params.MY_STAGE.contains("h5Deploy") }
                }
            }
            steps {
                echo 'h5-部署代码'
                sh "ssh -i /var/jenkins_home/id_rsa -p ${SSH_PORT} ${USER_NAME}@${REMOTE_HOST}  \"rm -rf ${DEPLOY_PATH}${SERVICE_NAME}/*&&cp -r ${COPY_PATH}${SERVICE_NAME}/* ${DEPLOY_PATH}${SERVICE_NAME}/ \""
            }
        }
        stage('java-启动服务') {
            when {
                allOf {
                    expression { params.MY_BUILD_MODE == "${MY_BUILD_MODE_JAVA}" }
                    expression { params.MY_STAGE.contains("javaStart") }
                }
            }
            steps {
                script {
                    println 'java-启动服务'
                    sh "ssh -i /var/jenkins_home/id_rsa -p ${SSH_PORT} ${USER_NAME}@${REMOTE_HOST}  \"nohup ${JAVA_CMD} ${JAVA_OPT} -jar ${DEPLOY_PATH}${SERVICE_NAME}/${SERVICE_NAME}.jar ${SPRING_ARGS} >/dev/null 2>&1 &\""
                    sleep(time: 5, unit: "SECONDS")
                    def check_cmd = '''ssh -i /var/jenkins_home/id_rsa -p ${SSH_PORT} ${USER_NAME}@${REMOTE_HOST} "ps -ef|grep ${SERVICE_NAME} |grep -v 'grep'|awk '{print \\$2}'"'''
                    def check_cmd_result = sh(script: check_cmd, returnStdout: true).trim()
                    if (check_cmd_result) {
                        println "启动java部署服务成功，进程id：" + check_cmd_result
                    } else {
                        error("启动java部署服务失败，请马上查看程序启动异常")
                    }
                }

            }
        }
        stage('h5-reload nginx服务') {
            when {
                allOf {
                    expression { params.MY_BUILD_MODE == "${MY_BUILD_MODE_H5}" }
                    expression { params.MY_STAGE.contains("h5Start") }
                }
            }
            steps {
                script {
                    echo 'h5-reload nginx服务'
                    sh "ssh -i /var/jenkins_home/id_rsa -p ${SSH_PORT} ${USER_NAME}@${REMOTE_HOST}  systemctl restart nginx.service "
                    sleep(time: 5, unit: "SECONDS")
                    def check_cmd = '''ssh -i /var/jenkins_home/id_rsa -p ${SSH_PORT} ${USER_NAME}@${REMOTE_HOST} "ps -ef|grep nginx |grep -v 'grep'|awk '{print \\$2}'"'''
                    def check_cmd_result = sh(script: check_cmd, returnStdout: true).trim()
                    if (check_cmd_result) {
                        println "启动H5部署服务成功，nginx进程id：" + check_cmd_result
                    } else {
                        error("启动H5部署服务失败，请马上查看nginx程序启动异常")
                    }
                }


            }
        }
        stage('删除备份保留30个') {
            when {
                allOf {
                    expression { params.MY_STAGE.contains("deleteBackup") }
                }
            }
            steps {
                echo '删除备份保留30个'
                sh '''
                       ssh -i /var/jenkins_home/id_rsa -p ${SSH_PORT} ${USER_NAME}@${REMOTE_HOST} "
                       cd ${BACKUP_PATH}${SERVICE_NAME}/
                       FILE_NUM=\\$(ls -l *.tar|wc -l)
                       if [ \\${FILE_NUM} -gt ${BACKUP_MAX_NUMBER} ]
                       then
                          DEL_NUM=\\$((FILE_NUM-30))
                          ls -tr *.tar | head -n \\${DEL_NUM} | xargs rm
                       fi"
                   '''
            }
        }
    }
    //always：无论 Pipeline 或 stage 的完成状态如何，都允许运行该 post 中定义的指令；
    //changed：只有当前 Pipeline 或 stage 的完成状态与它之前的运行不同时，才允许在该 post 部分运行该步骤；
    //fixed：当本次 Pipeline 或 stage 成功，且上一次构建是失败或不稳定时，允许运行该 post 中定义的指令；
    //regression：当本次 Pipeline 或 stage 的状态为失败、不稳定或终止，且上一次构建的 状态为成功时，允许运行该 post 中定义的指令；
    //failure：只有当前 Pipeline 或 stage 的完成状态为失败（failure），才允许在 post 部分运行该步骤，通常这时在 Web 界面中显示为红色
    //success：当前状态为成功（success），执行 post 步骤，通常在 Web 界面中显示为蓝色 或绿色
    //unstable：当前状态为不稳定（unstable），执行 post 步骤，通常由于测试失败或代码 违规等造成，在 Web 界面中显示为黄色
    //aborted：当前状态为终止（aborted），执行该 post 步骤，通常由于流水线被手动终止触发，这时在 Web 界面中显示为灰色；
    //unsuccessful：当前状态不是 success 时，执行该 post 步骤；
    //cleanup：无论 pipeline 或 stage 的完成状态如何，都允许运行该 post 中定义的指令。和 always 的区别在于，cleanup 会在其它执行之后执行。
    post {
        success {
            echo '构建成功'
        }
        unsuccessful {
            echo '构建失败'
        }
    }
}