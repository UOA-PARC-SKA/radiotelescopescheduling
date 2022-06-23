# Define a variable for classpath
CLASS_PATH = ../bin
ORBIT_DIR = orbit
CORE_DIR = core

# Define a virtual path for .class in the bin directory
vpath %.class $(CLASS_PATH)

#all : norad.dll

# $@ matches the target, $< matches the first dependency
#norad.dll : ext_NoradLink.o
#	gcc -Wl,--add-stdcall-alias -shared -o $@ $<

# $@ matches the target, $< matches the first dependency
#ext_NoradLink.o : ext_NoradLink.c NoradLinkImpl.cpp
#	gcc -I"C:\Program Files\Java\jdk1.8.0_121\include" -I"C:\Program Files\Java\jdk1.8.0_121\include\win32" -c $< -o $@
all: orbit_code core_code
	g++ -Wl,--add-stdcall-alias -I"C:\Program Files\Java\jdk-13.0.1\include" -I"C:\Program Files\Java\jdk-13.0.1\include\win32" -Icore -Iorbit -shared orbit/*.o core/*.o -o norad.dll ext_NoradLink.c NoradLinkImpl.cpp

orbit_code:
	$(MAKE) -C $(ORBIT_DIR)
       
core_code:
	$(MAKE) -C $(CORE_DIR)


# $* matches the target filename without the extension
ext_NoradLink.h : NoradLink.class
	javah -classpath $(CLASS_PATH) $*

clean :
	rm ext_NoradLink.h ext_NoradLink.o norad.dll
	

