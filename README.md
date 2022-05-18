# frtn01-project

This is a repository containing code completing the project of the course Real-time Systems (FRTN01) at LTH, Lund University. Contributers of this project are:

- Axel Björnberg
- Björn Fredlund
- Fredrik Horn
- Klas Junskog

## Setting up the hardware

- Connect the MinSeg to your PC using the supplied USB cable
- Make sure the HC-06 bluetooth module is removed from the circuit board
- Upload the code in the file ```minseg.ino``` using the Arduino IDE. Note: If the HC-06 bluetooth module is connected when uploading, the IDE might give you a timeout error
- Remove the USB cable from the MinSeg, and attach the HC-06 bluetooth module to the bluetooth header
- Carefully place the MinSeg on a flat surface with wheels down (make sure the wheels aren't in contact with the frame) and flip the battery switch to ON
- Watch how the MinSeg balances! It is also waiting for a bluetooth signal from the PC to move forward or backward. Make sure to be ready to catch the MinSeg in case it falls

## Starting up the software

- Open a terminal window (MacOS/Linux/UNIX) and navigate to the directory ```src``` where the source code of the project resides. Here a Makefile resides which enables the use of the ```make``` utility. Anyone interested can look into the different functions available.
- To start the program, first run ```make compile```. Then run ```make run``` to start the Java application. Then open a new terminal, navigate to the ```src``` directory and run ```make vision``` to start the python client that includes the webcam code.
- If all went well, you should now have four windows open. One containing the Java GUI, two windows that contain different camera views and a small window containing RGB sliders. 
- The program is now ready to connect to a MinSeg that's turned on and send commands to move forwards or backwards. For this, the python webcam program is not needed (i.e. you can skip running ```make vision```). However, if you wish to use the 'Control' functions then the camera has to be connected.
### Setup guide for some linux distros

If the bluetooth port of the HC06 isn't recognized by the Java-code try the following.
- Navigate to bluetooth connection and connect to HC06
- Let it connect and copy mac-address
- Open a terminal and paste "sudo rfcomm bind hci6 "mac-adress"
 
Follow the above instructions and the HC06 should be detected by the Java-code
