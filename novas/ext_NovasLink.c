#include <jni.h>
#include "ext_NovasLink.h"

#include "eph_manager.h"
#include "novas.h"

#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include <time.h>

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

static int loaded=0;

int get_julian_time(long tt, double *jd_tt, double *delta_t, double *jd_ut1);

JNIEXPORT jobject JNICALL Java_ext_NovasLink_getCBHorizonCoordinates
(JNIEnv *env, jobject thisObj, jlong time, jint cbID, jobject telescope)
{
	short int error = 0;
	if(!loaded)
	{
		// Julian Date at beginning and end of range of validity of ephemeris file
		double jd_beg=0, jd_end=0;
		// JPL DE version number
		short int de_num = 0;
		// error code

		if ((error = ephem_open ("novas/JPLEPH", &jd_beg,&jd_end,&de_num)) != 0)
		{
			if (error == 1)
				printf ("JPL ephemeris file not found.\n");
			else
				printf ("Error reading JPL ephemeris file header.\n");
		}

		loaded = 1;
	}
	long int datetime = (long int)time;
	int novasCBID = (int) cbID;

	// Approximation good to 0.9 seconds.

	double jd_tt, delta_t, jd_ut1;
	get_julian_time(datetime, &jd_tt, &delta_t, &jd_ut1);
	jclass teleClass = (*env)->GetObjectClass(env, telescope);

	jfieldID fid = (*env)->GetFieldID(env, teleClass, "name", "Ljava/lang/String;");
	jstring teleName = (jstring)(*env)->GetObjectField(env, telescope, fid);
	const char *tele = (*env)->GetStringUTFChars(env,teleName, NULL);

	fid = (*env)->GetFieldID(env, teleClass, "location", "Lastrometrics/Location;");
	jobject location = (jobject)(*env)->GetObjectField(env, telescope, fid);

	jclass locClass = (*env)->GetObjectClass(env, location);

	jmethodID mid = (*env)->GetMethodID(env, locClass, "getLatitudeInDegrees", "()D");
	const double latdeg = (*env)->CallDoubleMethod(env,location, mid);

	mid = (*env)->GetMethodID(env,locClass, "getLongitudeInDegrees", "()D");
	const double londeg = (*env)->CallDoubleMethod(env,location, mid);

	mid = (*env)->GetMethodID(env, locClass, "getElevation", "()D");
	const double elekm = (*env)->CallDoubleMethod(env, location, mid);

	// these dummy values will not be used
	const double temperature = 10.0;
	const double pressure = 1010.0;

	on_surface geo_loc;
	make_on_surface (latdeg,londeg,elekm,temperature,pressure, &geo_loc);

//	printf ("Geodetic location:\n");
//	printf ("%15.10f        %15.10f        %15.10f\n\n", geo_loc.longitude,
//			geo_loc.latitude, geo_loc.height);

	// dummy place-holder object
	cat_entry dummy;
	make_cat_entry ("DUMMY","xxx",0,0.0,0.0,0.0,0.0,0.0,0.0, &dummy);

	//CB = name of the body; but irrelevant for the function call.
	object obj;
	make_object (0,novasCBID,"CB",&dummy, &obj);


	const short int accuracy = 0;
	double rat, dect, dist;
	topo_planet (jd_tt,&obj,delta_t,&geo_loc,accuracy,&rat,&dect,&dist);


//	printf ("%s topocentric positions:\n", obj.name);
//	printf ("%15.10f        %15.10f        %15.12f\n", rat, dect, dist);

	// Position in local horizon coordinates.  (Polar motion ignored here.)

	double zd, az, rar, decr;
	equ2hor (jd_ut1,delta_t,accuracy,0.0,0.0,&geo_loc,rat,dect,1,
			&zd,&az,&rar,&decr);

	double azrad = az * M_PI / 180;
//	printf ("Deg az and rad az\n", obj.name);
//	printf ("%15.10f        %15.10f\n", az, azrad);
//	printf ("\n");

	double altdeg = 90 - zd;
	double altrad = altdeg * M_PI / 180;
	jclass horizoncls = (*env)->FindClass(env, "astrometrics/HorizonCoordinates");
	jmethodID getHorizonCoordID = (*env)->GetStaticMethodID(env, horizoncls, "getPredefined", "(DD)Lastrometrics/HorizonCoordinates;");
	jobject satHorizonCoords = (*env)->CallStaticObjectMethod(env, horizoncls, getHorizonCoordID, altrad, azrad);

	return satHorizonCoords;
}

//JNIEXPORT jdouble JNICALL Java_ext_NovasLink_getCBRadius
//  (JNIEnv *env, jobject thisObj, jlong time, jint cbID, jobject telescope)
//{
//	double jd_beg, jd_end;
//	short int de_num = 0;
//	short int error = 0;
//	  if ((error = ephem_open ("novas/JPLEPH", &jd_beg,&jd_end,&de_num)) != 0)
//	   {
//	      if (error == 1)
//	         printf ("JPL ephemeris file not found.\n");
//	       else
//	         printf ("Error reading JPL ephemeris file header.\n");
//	      return (error);
//	   }
//	    else
//	   {
//	      printf ("JPL ephemeris DE%d open. Start JD = %10.2f  End JD = %10.2f\n",
//	         de_num, jd_beg, jd_end);
//	      printf ("\n");
//	   }
//	const short int accuracy = 0;
//
//	double elon, elat;
//	double jd[2], pos[3], vel[3], pose[3];
//
//	long int datetime = (long int)time;
//	int novasCBID = (int) cbID;
//
//	// Approximation good to 0.9 seconds.
//
//	double jd_tt, delta_t, jd_ut1;
//	get_julian_time(datetime, &jd_tt, &delta_t, &jd_ut1);
//
//	cat_entry dummy;
//	make_cat_entry ("DUMMY","xxx",0,0.0,0.0,0.0,0.0,0.0,0.0, &dummy);
//
//	//CB = name of the body; but irrelevant for the function call.
//	object obj;
//	if ((error = make_object (0,novasCBID,"CB",&dummy, &obj)) != 0)
//	{
//		printf ("Error %d from make_object \n", error);
//	}
//	  jd[0] = jd_tt;
//	   jd[1] = 0.0;
//	   if ((error = ephemeris (jd,&obj,1,accuracy, pos,vel)) != 0)
//	   {
//	      printf ("Error %d from ephemeris (Mars).", error);
//	      return (error);
//	   }
//
//	   if ((error = equ2ecl_vec (T0,2,accuracy,pos, pose)) != 0)
//	   {
//	      printf ("Error %d from equ2ecl_vec.", error);
//	      return (error);
//	   }
//
//	   if ((error = vector2radec (pose, &elon,&elat)) != 0)
//	   {
//	      printf ("Error %d from vector2radec.", error);
//	      return (error);
//	   }
//	   elon *= 15.0;
//
//	   double r = sqrt (pose[0] * pose[0] + pose[1] * pose[1] + pose[2] * pose[2]);
//
//	   printf ("%d heliocentric ecliptic longitude and latitude and "
//	           "radius vector:\n",  novasCBID);
//	   printf ("%15.10f        %15.10f        %15.12f\n", elon, elat, r);
//	   printf ("\n");
//
//}

int get_julian_time(long tt, double *jd_tt, double *delta_t, double *jd_ut1)
{
	time_t temp = tt;
	//time already is GMT, gmtime backdates it again
	struct tm date = *localtime(&temp);

	double jd_utc = julian_date (date.tm_year+1900,
			date.tm_mon+1,
			date.tm_mday,
			date.tm_hour);

	jd_utc += date.tm_min / (24.0 * 60);
	jd_utc += date.tm_sec / (24.0 * 3600);
	// From Bulletin A - e.g. http://maia.usno.navy.mil/ser7/ser7.dat
	// Beginning 1 January 2017:
	//          TAI-UTC = 37.000 000 seconds

	double leap_secs = 37;

	// Approximation good to 0.9 seconds.
	double ut1_utc = 0;

	*jd_tt = jd_utc + (leap_secs + 32.184) / 86400.0;
	*jd_ut1 = jd_utc + ut1_utc / 86400.0;
	*delta_t = 32.184 + leap_secs - ut1_utc;
}

