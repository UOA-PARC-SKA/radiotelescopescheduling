# java-telescope-scheduling

## Getting Started

### Windows

1. Add the 3 jar files _(bson-4.2.3.jar, mongodb-driver-core-4.2.3.jar, mongo-java-driver-3.12.8.jar)_ as Libraries.
2. Use the latest MinGW-w64 for the g++ compilers.
3. Update the jdk paths used in the makefiles in the norad and noras folders according to your system.
4. In the norad and noras folders, delete all the .o and .dll files.
5. Run make in both norad and noras folders. **Make for Windows can be installed using chocolatey**
6. Once the dll files have been generated, add them as libraries in the project.
7. The GUI can be started by running the _ProcessingMain main function_.
