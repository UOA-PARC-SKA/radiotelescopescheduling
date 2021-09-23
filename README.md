# Telescope-Scheduling

---

## Getting Started
### Windows
1) add the following libraries (these are the versions that were being used, latest versions of these libraries should work as well):
	1) jna-platform:5.8.0
	2) jna:5.8.0
	3) bson:4.3.2
	4) mongo-java-driver:3.12.8
	5) mongodb-driver-core:4.2.3
	6) protobuf-java:3.17.3
	7) ortools-java:8.2.9025
2) Use the latest MinGW-w64 for the g++ compilers.
3) Update the jdk paths used in the makefiles in the norad and noras folders according to your system.
4) In the norad folder and it's sub folders, delete all .o and .dll files.
5) In the novas folder, only delete the noval.dll file.
6) Run make in both norad and noras folders. Make for Windows can be installed using chocolatey
7) Once the dll files have been generated, add them as libraries in the project.
8) The program can be started by running the ProcessingMain main function.