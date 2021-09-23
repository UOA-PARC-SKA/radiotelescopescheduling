# Telescope-Scheduling

---

## Getting Started
### Windows
1) add the following libraries (these are the versions that were being used, latest versions of these libraries should work as well):
	a) jna-platform:5.8.0
	b) jna:5.8.0
	c) bson:4.3.2
	d) mongo-java-driver:3.12.8
	e) mongodb-driver-core:4.2.3
	f) protobuf-java:3.17.3
	g) ortools-java:8.2.9025
2) Use the latest MinGW-w64 for the g++ compilers.
3) Update the jdk paths used in the makefiles in the norad and noras folders according to your system.
4) In the norad folder and it's sub folders, delete all .o and .dll files.
5) In the novas folder, only delete the noval.dll file.
6) Run make in both norad and noras folders. Make for Windows can be installed using chocolatey
7) Once the dll files have been generated, add them as libraries in the project.
8) The program can be started by running the ProcessingMain main function.