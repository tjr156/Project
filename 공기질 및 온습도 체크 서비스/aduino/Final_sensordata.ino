#include <SoftwareSerial.h>

#include <DHT.h>
#include <DHT_U.h>
#include <string.h>


SoftwareSerial Bluetooth(2,3);

unsigned long duration;
unsigned long duration1;
unsigned long duration2;
unsigned long starttime;
unsigned long sampletime_ms = 3000;//sampe 30s ;
unsigned long lowpulseoccupancy = 0;

float pcsPerCF = 0;
float ugm3 = 0;
float ratio = 0;
float concentration = 0;

#include <math.h>
int a;
float temperature;
int B = 3975;                //B value of the thermistor
float resistance;

int ad_conv(byte channel, byte num);
int calc_RH10(int adval);

int pin = 9;

float voMeasured = 0;
float calcVoltage = 0;
float dustDensity = 0;
int count = 0;
float sum1 = 0;
  float sum2 = 0;



void setup(){
  Serial.begin(9600);
  Bluetooth.begin(9600);
  pinMode(9, INPUT);
  
}

void loop(){
  //미세먼지 측정
  
  duration = analogRead(pin);
  duration1 = duration * 446;
  lowpulseoccupancy = lowpulseoccupancy + duration1;
  ratio = lowpulseoccupancy / (sampletime_ms * 10.0); // Integer percentage 0=>100
  concentration = 1.1 * pow(ratio, 3) - 3.8 * pow(ratio, 2) + 520 * ratio + 0.62; // using spec sheet curve
  pcsPerCF = concentration * 100;
  ugm3 = pcsPerCF / 13000;
  lowpulseoccupancy=0;
  

  //온도 측정
  int err;
  a = analogRead(A1);
  resistance = (float)(1023 - a) * 10000 / a; //get the resistance of the sensor;
  temperature = 1 / (log(resistance / 10000) / B + 1 / 298.15) - 273.15; //convert to temperature via datasheet ;
  String str1 = (String)temperature;

  //습도 측정
  int adval, RH10;
  adval = ad_conv(0, 32); // 32 samples on Channel 0
  RH10 = calc_RH10(adval);
  int whole, fract;
  whole = RH10/10;
  fract = RH10%10;
  String humi = (String)whole + "." + (String)fract;
  str1 = str1 + "!" + humi + "@" + (String)ugm3; 
  
  
  
  

  
  Serial.print("온도 : ");
  Serial.print(temperature);
  Serial.print("   습도 : ");
  Serial.print(whole);
  Serial.print(".");
  Serial.print(fract);  
  Serial.print("   미세먼지 농도 : ");
  Serial.println(ugm3);
  Bluetooth.println(str1);
  
  
  
  delay(2000);
}

int ad_conv(byte channel, byte num)
{
  long sum = 0;
  byte n;

  for (n = 0; n < num; n++)
  {
    sum = sum + analogRead(channel);
  }
  return (sum / num);
}


int calc_RH10(int adval)
{
  int RH10;
  RH10 = adval + 6 * adval / 10 + 3 * adval / 100; // 1.63 * adval
  return (RH10);
}



