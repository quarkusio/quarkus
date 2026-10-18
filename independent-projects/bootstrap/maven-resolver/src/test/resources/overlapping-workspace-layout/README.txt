Apparently, this kind of layout could be created for some projects by the Pipeline Jenkins plugin:
1) the Jenkins job clones the repository to /home/jenkins/workspace/<project-name> to find the Jenkinsfile, which contains a definition of the pipeline
2) the pipeline file checks out the repository to build into a subdirectory /home/jenkins/workspace/<project-name>/<project-name>