compile:
	javac Main.java Monitor.java comm/Server.java -cp .:comm/jSerialComm-2.9.1.jar comm/BtComm.java -cp .:comm/jSerialComm-2.9.1.jar comm/BtGUI.java
all:
	javac Main.java Monitor.java comm/Server.java -cp .:comm/jSerialComm-2.9.1.jar comm/BtComm.java -cp .:comm/jSerialComm-2.9.1.jar comm/BtGUI.java
	java -cp .:../:comm/jSerialComm-2.9.1.jar src.Main
	make -s clean
	
vision:
	python3 opencv/vision.py
run:
	java -cp .:../:comm/jSerialComm-2.9.1.jar src.Main
clean:
	rm -rf *.class
	rm -rf comm/*.class
