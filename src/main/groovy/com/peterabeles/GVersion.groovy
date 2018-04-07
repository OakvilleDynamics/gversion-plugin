/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.peterabeles

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.UnknownProjectException

import java.text.SimpleDateFormat

class GVersionExtension {
//    List javadoc_links = []
//    String javadoc_bottom_path = "misc/bottom.txt"
    String gversion_file_path
    String gversion_package = ""
    String gversion_class_name = "GVersion"
    String date_format = "yyyy-MM-dd HH:mm:ss"
}

class GVersion implements Plugin<Project> {

    static String gversion_file_path( GVersionExtension extension ) {
        if(extension.gversion_file_path == null )
            throw new RuntimeException("Must set gversion_file_path")

        File gversion_file_path = new File(extension.gversion_file_path,
                extension.gversion_package.replace(".",File.pathSeparator))
        return gversion_file_path.getPath()
    }

    void apply(Project project) {
        // Add the 'greeting' extension object
        def extension = project.extensions.create('gversion', GVersionExtension)

//        project.tasks.create('alljavadoc',JavaDoc){
////        task alljavadoc(type: Javadoc) {
//            if( extension.length == 0 ) {
//                throw new RuntimeException("javadoc_links has not been specified")
//            }
//            if( extension.size() == 0 ) {
//                throw new RuntimeException("project_name needs to be specified")
//            }
//
//            // only include source code in src directory to avoid including 3rd party code which some projects do as a hack
//            source = javadocProjects.collect { project(it).fileTree('src').include('**/*.java') }
////    source = javadocProjects.collect { project(it).sourceSets.main.allJava }
//            classpath = files(javadocProjects.collect { project(it).sourceSets.main.compileClasspath })
//
//            destinationDir = file("docs/api")
//
//            // Hack for Java 8u121 and beyond. Comment out if running an earlier version of Java
//            options.addBooleanOption("-allow-script-in-comments", true)
//
//            // Add a list of uses of a class to javadoc
//            options.use = true
//
//            configure(options) {
//                failOnError = false
//                docTitle = extension.project_name+" JavaDoc ($project.version)"
//                links = extension.javadoc_links
//            }
//
//            // Work around a Gradle design flaw. It won't copy over files in doc-files
//            doLast {
//                copy {
//                    from javadocProjects.collect { project(it).fileTree('src').include('**/doc-files/*') }
//                    into destinationDir
//                }
//            }
//
//        }
//
//        task alljavadocWeb() {
//            doFirst {
//                alljavadoc.options.bottom = file(extension.javadoc_bottom_path).text
//                alljavadoc.destinationDir = file("docs/api-web")
//            }
//        }
//        alljavadocWeb.finalizedBy(alljavadoc)


        project.ext.checkProjectExistsAddToList = { whichProject , list ->
            try {
                project.project(whichProject)
                list.add(whichProject)
            } catch( UnknownProjectException ignore ) {}
        }

        // Force the release build to fail if it depends on a SNAPSHOT
        project.tasks.create('checkDependsOnSNAPSHOT'){
            doLast {
                if (project.version.endsWith("SNAPSHOT"))
                    return

                project.configurations.compile.each {
                    if (it.toString().contains("SNAPSHOT"))
                        throw new Exception("Release build contains snapshot dependencies: " + it)
                }
            }
        }

        project.task('checkForVersionFile') {
            doLast {
                def f = new File(gversion_file_path(extension),extension.gversion_class_name+".java")
                if( !f.exists() ) {
                    throw new RuntimeException("GVersion.java does not exist. Call 'createVersionFile'")
                }
            }
        }

        // Creates a resource file containing build information
        project.task('createVersionFile'){
            doLast {
                def gversion_file_path = gversion_file_path(extension)
                println("createVersionFile called. Path "+gversion_file_path)

                def git_revision
                def git_sha

                try {
                    def proc = 'git rev-list --count HEAD'.execute()
                    proc.consumeProcessErrorStream(new StringBuffer())
                    proc.waitFor()
                    if( proc.exitValue() != 0 )
                        throw new IOException();
                    git_revision = proc.text.trim()
                    proc = 'git rev-parse HEAD'.execute()
                    proc.consumeProcessErrorStream(new StringBuffer())
                    proc.waitFor()
                    if( proc.exitValue() != 0 )
                        throw new IOException()
                    git_sha = proc.text.trim()
                } catch (IOException ignore) {
                    git_revision = -1
                    git_sha = "UNKNOWN"
                }

                def formatter = new SimpleDateFormat(extension.date_format)
                def date_string = formatter.format(new Date())

                def f = new File(gversion_file_path,extension.gversion_class_name+".java")
                def writer = new FileWriter(f);
                if( extension.gversion_package.size() > 0 ) {
                    writer << "package $extension.gversion_package;\n"
                    writer << "\n\n"
                }
                writer << "/**\n"
                writer << " * Automatically generated file containing build version information.\n"
                writer << " */\n"
                writer << "public class "+extension.gversion_class_name+" {\n"
                writer << "\tpublic static final String MAVEN_GROUP = \"$project.group\";\n"
                writer << "\tpublic static final String MAVEN_NAME = \"$project.name\";\n"
                writer << "\tpublic static final String VERSION = \"$project.version\";\n"
                writer << "\tpublic static final int GIT_REVISION = $git_revision;\n"
                writer << "\tpublic static final String GIT_SHA = \"$git_sha\";\n"
                writer << "\tpublic static final String BUILD_DATE = \"$date_string\";\n"
                writer << "}"
                writer.flush()
                writer.close()
            }
        }

//        project.tasks.create('testReport',TestReport.class) {
//            doLast {
//                destinationDir = project.file("$project.buildDir/reports/allTests")
//                reportOn project.subprojects*test
//            }
//        }
    }
}