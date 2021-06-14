#include <jni.h>
#include "NoradLinkImpl.h"
#include "stdafx.h"
#include "cOrbit.h"
#include "cSite.h"

#include  <iostream>

using namespace std;
using namespace std;
using namespace Zeptomoby::OrbitTools;

jobject getSatHorizonCoordinates (JNIEnv *env, jobject satellite, long int datetime, jobject telescope) {

	jclass satClass = env->GetObjectClass(satellite);

	jfieldID fid = env->GetFieldID(satClass, "name", "Ljava/lang/String;");
	jstring satName = (jstring)env->GetObjectField( satellite, fid);
	const char *name = env->GetStringUTFChars(satName, NULL);

	fid = env->GetFieldID(satClass, "tleLine1", "Ljava/lang/String;");
	jstring line1 = (jstring)env->GetObjectField( satellite, fid);
	const char *tle1 = env->GetStringUTFChars(line1, NULL);

	fid = env->GetFieldID(satClass, "tleLine2", "Ljava/lang/String;");
	jstring line2 = (jstring)env->GetObjectField( satellite, fid);
	const char *tle2 = env->GetStringUTFChars(line2, NULL);

	jclass teleClass = env->GetObjectClass(telescope);

	fid = env->GetFieldID(teleClass, "name", "Ljava/lang/String;");
	jstring teleName = (jstring)env->GetObjectField( telescope, fid);
	const char *tele = env->GetStringUTFChars(teleName, NULL);

	fid = env->GetFieldID(teleClass, "location", "Lastrometrics/Location;");
	jobject location = (jobject)env->GetObjectField(telescope, fid);

	jclass locClass = env->GetObjectClass(location);

	jmethodID mid = env->GetMethodID(locClass, "getLatitudeInDegrees", "()D");
	double latdeg = env->CallDoubleMethod(location, mid);

	mid = env->GetMethodID(locClass, "getLongitudeInDegrees", "()D");
	double londeg = env->CallDoubleMethod(location, mid);

	mid = env->GetMethodID(locClass, "getElevation", "()D");
	double elekm = env->CallDoubleMethod(location, mid);


    time_t temp = datetime;

    cJulian JD (temp);

    cSite Parkes ( latdeg, londeg, elekm, tele);
    //cSite Parkes ( -32.9983, 148.2636, 0.0, "Parkes" );
    cTle tle (name, tle1, tle2);
	cOrbit orbit (tle);

	cJulian epoch = orbit.Epoch ();


	double sec = orbit.TPlusEpoch (JD);

	// argument to PositionEci is minutes past epoch
	cVector pos = orbit.PositionEci(sec/60.0).Position();

	cTopo topo = Parkes.GetLookAngle ( orbit.PositionEci(sec/60.0) );

	jclass horizoncls = env->FindClass("astrometrics/HorizonCoordinates");
	jmethodID getHorizonCoordID = env->GetStaticMethodID(horizoncls, "getPredefined", "(DD)Lastrometrics/HorizonCoordinates;");
	jobject satHorizonCoords = env->CallStaticObjectMethod(horizoncls, getHorizonCoordID, topo.ElevationRad(), topo.AzimuthRad());

	env->ReleaseStringUTFChars(satName, name);
	env->ReleaseStringUTFChars(line1, tle1);
	env->ReleaseStringUTFChars(line2, tle2);
	env->ReleaseStringUTFChars(teleName, tele);
    return satHorizonCoords;
}
