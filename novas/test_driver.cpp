
extern "C"{
  
#include "eph_manager.h" 
#include "novas.h"

}

#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include <time.h>

#include <iostream>
#include <vector>
using namespace std;


int main (void)
{
  /* ***********************************************************************

    Open the JPL binary ephemeris file, here named "JPLEPH".
    This appears to open a file that is stored as a global variable.

  *********************************************************************** */

  // Julian Date at beginning and end of range of validity of ephemeris file
  double jd_beg=0, jd_end=0;
  // JPL DE version number
  short int de_num = 0;
  // error code
  short int error = 0;

  if ((error = ephem_open ("JPLEPH", &jd_beg,&jd_end,&de_num)) != 0)
  {
    if (error == 1)
      printf ("JPL ephemeris file not found.\n");
    else
      printf ("Error reading JPL ephemeris file header.\n");
    return (error);
  }
  else
  {
    printf ("JPL ephemeris DE%d open. Start JD = %10.2f  End JD = %10.2f\n",
	    de_num, jd_beg, jd_end);
    printf ("\n");
  }

  /* ***********************************************************************
     
     Create an on_surface structure to store the geocentric coordinates of
     the Parkes observatory.
     
     *********************************************************************** */
  
  // Parkes Observatory: 32.9983° S, 148.2636° E
  const double latitude = -32.9983;
  const double longitude = 148.2636;
  const double height = 0.0;

  // these dummy values will not be used
  const double temperature = 10.0;
  const double pressure = 1010.0;
   
  on_surface geo_loc;
  make_on_surface (latitude,longitude,height,temperature,pressure, &geo_loc);

  printf ("Geodetic location:\n");
  printf ("%15.10f        %15.10f        %15.10f\n\n", geo_loc.longitude,
	  geo_loc.latitude, geo_loc.height);


  /* ***********************************************************************
     
     Create object structures for the Moon, Sun, and Jupiter
     
     *********************************************************************** */

  // dummy place-holder object
  cat_entry dummy;
  make_cat_entry ("DUMMY","xxx",0,0.0,0.0,0.0,0.0,0.0,0.0, &dummy);

  vector<object> objects (3);
  
  if ((error = make_object (0,11,"Moon",&dummy, &objects[0])) != 0)
  {
    printf ("Error %d from make_object (Moon)\n", error);
    return (error);
  }

  if ((error = make_object (0,10,"Sun",&dummy, &objects[1])) != 0)
  {
    printf ("Error %d from make_object (Moon)\n", error);
    return (error);
  }

  if ((error = make_object (0,5,"Jupiter",&dummy, &objects[2])) != 0)
  {
    printf ("Error %d from make_object (Moon)\n", error);
    return (error);
  }

  
  /* ***********************************************************************

    Convert the current UTC to Julian Date and then to 
    Terrestrial Time (TT) and Universal Time (UT1)

    TT is used to compute the right ascension and declination of the planet
    UT1 is used to compute the azimuth and elevation as seen from Parkes

  *********************************************************************** */

  time_t temp = time(NULL);
  struct tm date = *gmtime(&temp);
  cerr << "Using today's date: " << asctime(&date) << endl;

  double jd_utc = julian_date (date.tm_year+1900,
			       date.tm_mon+1,
			       date.tm_mday,
			       date.tm_hour);

  // From Bulletin A - e.g. http://maia.usno.navy.mil/ser7/ser7.dat
  // Beginning 1 January 2017:                                             
  //          TAI-UTC = 37.000 000 seconds
    
  double leap_secs = 37;

  // Approximateion good to 0.9 seconds.
  double ut1_utc = 0;
  
  double jd_tt = jd_utc + (leap_secs + 32.184) / 86400.0;
  double jd_ut1 = jd_utc + ut1_utc / 86400.0;
  double delta_t = 32.184 + leap_secs - ut1_utc;
   
  printf ("TT and UT1 Julian Dates and Delta-T:\n");
  printf ("%15.6f        %15.6f        %16.11f\n", jd_tt, jd_ut1, delta_t);
  printf ("\n");

  /* ***********************************************************************

     Print the topocentric coordinates of the objects

  *********************************************************************** */

  for (unsigned i=0; i<objects.size(); i++)
  {
    const short int accuracy = 0;
    double rat, dect, dist;
    if ((error = topo_planet (jd_tt,&objects[i],delta_t,&geo_loc,accuracy,
			      &rat,&dect,&dist)) != 0)
    {
      printf ("Error %d from topo_planet.", error);
      return (error);
    }
    
    printf ("%s topocentric positions:\n", objects[i].name);
    printf ("%15.10f        %15.10f        %15.12f\n", rat, dect, dist);
    
    // Position in local horizon coordinates.  (Polar motion ignored here.)

    double zd, az, rar, decr;
    equ2hor (jd_ut1,delta_t,accuracy,0.0,0.0,&geo_loc,rat,dect,1,
	     &zd,&az,&rar,&decr);

    printf ("%s zenith distance and azimuth:\n", objects[i].name);
    printf ("%15.10f        %15.10f\n", zd, az);
    printf ("\n");
  }

   

  ephem_close();  /* remove this line for use with solsys version 2 */
  
  return (0);
}
