# Define a variable for classpath
CLASS_PATH = ../bin

all: $(OBJS)
	gcc -Wl,--add-stdcall-alias -I"C:\Users\hp\.jdks\corretto-1.8.0_292\include" -I"C:\Users\hp\.jdks\corretto-1.8.0_292\include\win32" -shared $(OBJS) -o novas.dll ext_NovasLink.c

SRCS = novas.c novascon.c nutation.c readeph0.c solsys1.c eph_manager.c
OBJS = $(subst .c,.o,$(SRCS))

%.o : %.c
	gcc -c $<
