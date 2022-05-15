#include <Wire.h>

//Encoderpins
#define encoderPinA  2
#define encoderPinB  3

//Offsets
#define GYRO_OFFSET -1.1 //-1.1 Minseg1, -2 Minseg2
#define ACCEL_OFFSET 4.6 // 4.6 Minseg1 , 3.2 Minseg2, 1 if no batteries

//IMU_Phi
#define IMU_KALMAN_A1 0.501114651056334
#define IMU_KALMAN_A2 -0.177323686540424
#define IMU_KALMAN_A3 0.000578054347052727
#define IMU_KALMAN_A4 0.938716477582768

//IMU_Gamma
#define IMU_KALMAN_B1 0.498885348943666
#define IMU_KALMAN_B2 0.177323686540424
#define IMU_KALMAN_B3 0.00942194565294727
#define IMU_KALMAN_B4 0.0612835224172317

//IMU_C
#define IMU_KALMAN_C1 0.501114651056334
#define IMU_KALMAN_C2 -0.177323686540424
#define IMU_KALMAN_C3 -0.00443309216351061
#define IMU_KALMAN_C4 0.940489714448173

//IMU_D
#define IMU_KALMAN_D1 0.498885348943666
#define IMU_KALMAN_D2 0.177323686540424
#define IMU_KALMAN_D3 0.00443309216351061
#define IMU_KALMAN_D4 0.0595102855518274

//Wheel_Phi
#define WHEEL_KALMAN_A1 1
#define WHEEL_KALMAN_A2 -5.85579524858881
#define WHEEL_KALMAN_A3 0.0100000000000000
#define WHEEL_KALMAN_A4 0.627248807381958

//Wheel_Gamma
#define WHEEL_KALMAN_B1 5.85579524858881
#define WHEEL_KALMAN_B3 0.372751192618042

//Wheel_C
#define WHEEL_KALMAN_C1 1
#define WHEEL_KALMAN_C2 -5.85579524858881
#define WHEEL_KALMAN_C3 0
#define WHEEL_KALMAN_C4 0.685806759867846

//Wheel_D
#define WHEEL_KALMAN_D1 5.85579524858881
#define WHEEL_KALMAN_D3 0.314193240132154

//State Feedback
#define L1 -10.2807760122491
#define L2 -58.5357689934485
#define L3 -0.835210055837888
//#define L4 -0.0168213999540856
#define L4 0

volatile int encoderTheta = 0;
volatile boolean PastA,PastB;

float accelAngle, gyroRotX, kalmanAlpha = 0, kalmanAlphaDot = 0, alphaDot,alpha;
float kalmanThetaDot = 0, kalmanTheta = 0, thetaDot, theta; 
float h = 10, duration, t; //10000 µs = 10 ms;
float u, R_alpha = 0;

void setup() {
  Serial.begin(9600);

  //Setup MPU
  Wire.begin();
  setupMPU();

  pinMode(LED_BUILTIN, OUTPUT);

  //Setup encoder
  pinMode(encoderPinA, INPUT);
  pinMode(encoderPinA, INPUT);
  PastA = (boolean)digitalRead(encoderPinA); //initial value of channel A
  PastB = (boolean)digitalRead(encoderPinB);
  attachInterrupt(digitalPinToInterrupt(encoderPinA), encoderA, CHANGE);
  attachInterrupt(digitalPinToInterrupt(encoderPinB), encoderB, CHANGE);
  
  t = millis();
}


void loop() {
  recordAccelAngle();
  recordGyroRotx();
  imuFilter();
  wheelFilter();
  u = calculateOutput();
  output(u);
  imuUpdateStates();
  wheelUpdateStates();
  bluetooth_poll();
  //sendData();
  t = t + h;
  duration = t - millis();
  delay(duration);
}

void setupMPU(){
  Wire.beginTransmission(0b1101000); //This is the I2C address of the MPU (b1101000/b1101001 for AC0 low/high datasheet sec. 9.2)
  Wire.write(0x6B); //Accessing the register 6B - Power Management (Sec. 4.28)
  Wire.write(0b00000000); //Setting SLEEP register to 0. (Required; see Note on p. 9)
  Wire.endTransmission();  
  Wire.beginTransmission(0b1101000); //I2C address of the MPU
  Wire.write(0x1B); //Accessing the register 1B - Gyroscope Configuration (Sec. 4.4) 
  Wire.write(0x00000000); //Setting the gyro to full scale +/- 250deg./s 
  Wire.endTransmission(); 
  Wire.beginTransmission(0b1101000); //I2C address of the MPU
  Wire.write(0x1C); //Accessing the register 1C - Acccelerometer Configuration (Sec. 4.5) 
  Wire.write(0b00000000); //Setting the accel to +/- 2g
  Wire.endTransmission(); 
}

void recordAccelAngle() 
{
  Wire.beginTransmission(0b1101000); //I2C address of the MPU
  Wire.write(0x3D); //Starting register for Accel Readings
  Wire.endTransmission();
  Wire.requestFrom(0b1101000,4); //Request Accel Registers (3D - 40)
  while(Wire.available() < 4);
  float accelY = Wire.read()<<8|Wire.read(); //Store middle two bytes into accelY
  float accelZ = Wire.read()<<8|Wire.read(); //Store last two bytes into accelZ
  accelAngle = atan2(accelZ,(-accelY))*57.2958 + ACCEL_OFFSET;//Calculate angle and convert to degrees
}


void recordGyroRotx() 
{
  Wire.beginTransmission(0b1101000); //I2C address of the MPU
  Wire.write(0x43); //Starting register for Gyro Readings
  Wire.endTransmission();
  Wire.requestFrom(0b1101000,2); //Request Gyro Registers (43 - 44)
  while(Wire.available() < 2);
  float gyroX = Wire.read()<<8|Wire.read(); //Store first two bytes into accelX
  gyroRotX = (gyroX / 131.0) + GYRO_OFFSET; //To get a value in deg/s
}

void bluetooth_poll() 
{
  if (Serial.available() > 0) {
    char incomingChar = Serial.read();
    if (incomingChar == 43) { // '+'
      digitalWrite(LED_BUILTIN, HIGH);
      //R_theta += (10.0 / 21.0) * (180.0/3.141592653589793238463);
      R_alpha += 0.1;    
    } else if (incomingChar == 45) { // '-'
      digitalWrite(LED_BUILTIN, LOW);
      //R_theta -= (10.0 / 21.0) * (180.0/3.141592653589793238463);
      R_alpha -= 0.1;
    } else if (incomingChar == 48) { // '0'
      R_alpha = 0;
    }

    if (R_alpha > 1) {
      R_alpha = 1;
    } else if (R_alpha < -1) {
      R_alpha = -1;
    }
  }
}


void sendData() {
  //Serial.print(gyroRotX);
  //Serial.print(" ");
  //Serial.println(accelAngle);
  //Serial.print(" ");
  //Serial.println(alphaDot);
  //Serial.print(" ");
  Serial.println(alpha);
  //Serial.print(" ");
  //Serial.print(encoderTheta);
  //Serial.print(" ");
  //Serial.print(theta);
  //Serial.print(" ");
  //Serial.print(thetaDot);
  //Serial.print(" ");
  //Serial.println(u/255);
}

void imuFilter()
{
  alphaDot = IMU_KALMAN_C1*kalmanAlphaDot + IMU_KALMAN_C2*kalmanAlpha + IMU_KALMAN_D1*gyroRotX + IMU_KALMAN_D2*accelAngle;
  alpha = IMU_KALMAN_C3*kalmanAlphaDot + IMU_KALMAN_C4*kalmanAlpha + IMU_KALMAN_D3*gyroRotX + IMU_KALMAN_D4*accelAngle;
}

void wheelFilter()
{
  thetaDot = WHEEL_KALMAN_C1*kalmanThetaDot + WHEEL_KALMAN_C2*kalmanTheta + WHEEL_KALMAN_D1*encoderTheta;
  theta = WHEEL_KALMAN_C3*kalmanThetaDot + WHEEL_KALMAN_C4*kalmanTheta + WHEEL_KALMAN_D3*encoderTheta;
}

void imuUpdateStates()
{
  kalmanAlphaDot = IMU_KALMAN_A1*kalmanAlphaDot + IMU_KALMAN_A2*kalmanAlpha + IMU_KALMAN_B1*gyroRotX + IMU_KALMAN_B2*accelAngle;
  kalmanAlpha = IMU_KALMAN_A3*kalmanAlphaDot + IMU_KALMAN_A4*kalmanAlpha + IMU_KALMAN_B3*gyroRotX + IMU_KALMAN_B4*accelAngle;
}

void wheelUpdateStates()
{
  kalmanThetaDot = WHEEL_KALMAN_A1*kalmanThetaDot + WHEEL_KALMAN_A2*kalmanTheta + WHEEL_KALMAN_B1*encoderTheta;
  kalmanTheta = WHEEL_KALMAN_A3*kalmanThetaDot + WHEEL_KALMAN_A4*kalmanTheta + WHEEL_KALMAN_B3*encoderTheta;
}

void encoderA()
{
     PastB&&PastA ? encoderTheta--:  encoderTheta++;
     PastA = !PastA;
}

void encoderB()
{
     PastA&&PastB ? encoderTheta++:  encoderTheta--;
     PastB = !PastB;
}

float calculateOutput()
{
  return -(L1*alphaDot  + L2*(alpha - R_alpha)*2 + L3*thetaDot + L4*theta);
}

void output(float outputVal)
{
  //Saturation
  if (outputVal > 255){
    outputVal = 255;
  } else if(outputVal < -255){
    outputVal = -255;
  }
  // Output forwards or backwards
  if (outputVal  <0) {
    analogWrite(4, -outputVal);
    analogWrite(5, 0);
  } else {
    analogWrite(5, outputVal);
    analogWrite(4, 0);
  }
}
