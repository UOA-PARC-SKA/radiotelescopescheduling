# Telescope-Scheduling

---
**NOTE: The stable branch is the branch that is to be used and contains the latest working code.**

## List of files and directories

### results directory
Folder where the results are output. Each observation is output as a .csv file containing the overall duration, slew times, waiting times, integrations, as well as the entire schedule of the observation. For simulations where multiple batches were run, an additional .csv is output containing the averaged values of all these observations.

### data directory
Contains the main three data set files name_ra_dec_minP_minS.txt which is the data set 1, dataset2.txt which is data set 2 and dataset3.txt which is data set 3. The dataset files contain the list of the pulsar samples used in the observation runs. The dataset also contains important information of the pulsar’s initial coordinates, scintillation timescale, and the estimated integration time during observation.

### norad and novas directories
These folders are for the norad and novas libraries used for getting satellite data which in used in the simulation. These folders were provided by the original repository as well. There are changes to be made to the files of these folders and have been described in the getting stared section.

### src/optimisation directory
Contains all the scheduling policies. In the triangulations sub directory it contains all the pre-optimisation algorithms. The code for the main scheduler that schedules observations is in this directory as well.

### config file
The file where the parameters for the simulation is initialized. This includes factors such as the type of telescope, the number of runs, the scheduling policy, the pre-optimization algorithm, etc. The data set being used can also be specified in this file in the observations_dataset parameter.

### src/SchedulingMain.java file
The main class that includes the main function which is needed to run the program.

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
3) Update the jdk paths used in the makefiles in the norad and noras folders according to your system. In the makefile in the norad folder, for the _all_ target, change the jdk paths to be specific to your system. Add paths for the include and include/win32 directories of your jdk as already added in the makefiles. Do the same for the _all_ target in the makefile of the navas folder as well. Example for the paths: `C:\Users\hp\.jdks\temurin-16.0.2\include`, `C:\Users\hp\.jdks\temurin-16.0.2\include\win32`. The paths to these folders are to be updated in the makefiles.
4) In the norad folder and it's sub folders, delete all .o and .dll files.
5) In the novas folder, only delete the noval.dll file.
6) Run make in both norad and noras folders. Make for Windows can be installed using chocolatey
7) Once the dll files have been generated, add them as libraries in the project.
8) Change the outputdir address in the config file to be specific to your system.
9) The program can be started by running the SchedulingMain main function.

## How to change the policy being used
In the config file which is in the root directory, change the policy_class property to optimisation.<class name of the policy>, for eg: for policy with class name LargestSlewPolicy, change the policy_class property to optimisation.LargestSlewPolicy.

## How to change the preoptimisation being used
In the config file which is in the root directory, change the preoptimisation property to "all" for selecting the preoptimisation AllPulsarsAsNeighbours, change it to "tsp" for selecting the preoptimisation TravellingSalesmanPreoptimisation and any other string for that property would select the DynamicNNOptimisation.
	
**NOTE: To change the datasets being used, paths or any other configurations, update the config file present in the root directory.**
	
## Make a new pre-optimisation
1) In the optimisation/triangulations directory, make a class that extends NNOptimisation.

2) Create a public function that throws an OutOfObservablesException in your class. Let’s call the function addTempLinks for this example.
	
	Example function defination:
	```java
	public void addTempLinks( 
		List<Target> targets , 
		Pointable current , 
		double ratio , 
		Clock clock , 
		Location loc , 
		Telescope telescope) throws OutOfObservablesException {}
	```
	These areguments are the same as that used in the code.

3) At the start of the policy add current.clearNeighbours(); As the pre-optimisation sets the neighbours to the current pulsar being observed, all the neighbours first have to be cleared and then added based on the respective algorithm.

4) In case there is no neighbour to add, throw an `OutOfObservablesException`. For example:
	```java
	if (neigbours.isEmpty()) throw new OutOfObservablesException()
	```
	Here neighbours is a list used to store the potential neighbours that will be added to the current pulsar.

5) In the DiapatchPolicy class in the optimisations directory, create a private object of your pre-optimisation class. For this example let’s call it `tempOb`.

6) Decide on a name that will be used in the config file to use this pre-optimisation, for this example let’s use `temp`.

7) In the addNeighbours function, add another else if for your pre-optimisation where the function implemented for the pre-optimisation is called, which in this case is `addTempLinks`. The string argument in the argument called preoptimisation is the name of the pre-optimisation specified for the parameter preoptimisation in the config file. For example:
	```java
	if(preoptimisation.equals("temp")) { 
		tempOb.addTempLinks(observables, current, triangulationRatio, Clock.getScheduleClock(), telescope.getLocation(), telescope);
	}
	```
	This example uses the same argument names to what is being used in the project to pass in the respective arguments. Observables contain all the observable pulsars and current is the current pulsar being observed.

8) Create another public function in the DispatchPolicy class. Let’s call is `addTempNeigbours` for this example. This will just call the pre-optimisation function which in this example is `addTempLinks` with the respective arguments. This function takes in a Pointable argument and will pass this on as an argument for the `current` property to the `addTempLinks` function.

9) In the `waitForObservables` function, add another else if in the while loop in the try block. The conditional would be for if the preoptimisation string equals to "temp" (the name used in the config for this pre-optimisation). Here the `addTempNeighbours` function will be called and **`schedule.getCurrentState().getCurrentTarget()`** would be passed in as argument.

10) Now to use this pre-optimisation, just specify the name that was selected, in this case "temp" in the preoptimisation property in the config file.

## Make new scheduling policy
1) In the optimisation directory, make a new class that extends the class that extends `DispatchPolicy`.

2) Create the public function `findNextPath` that returns a Connection object and takes in a Pointable object as an argument.

	Example: **public Connection** findNextPath(Pointable current)

3) To use the policy, set the `policy_class` property in the config file to `optimisation.[policy_name]`
