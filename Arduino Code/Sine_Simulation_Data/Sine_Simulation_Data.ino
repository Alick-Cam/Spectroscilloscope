#include <SoftwareSerial.h>
SoftwareSerial Spectros(14, 15); // RX | TX

int f = 30000;
float fs = 200000.0;
int samples[512];
unsigned char data[1024];
float t = 0.0;


void setup() {
    Spectros.begin(115200);
    Serial.begin(115200);
}

void loop() {
  // put your main code here, to run repeatedly:


  if (Spectros.available()) {
   if (Spectros.read() == 't') {
     for (int i = 0; i < 512 ; i ++) {
     t  = (float)i/fs;
     samples[i] = (int)(511.0*(sin(2*3.14*f*t)+1.0));
      data[2*i] = samples[i] >> 8;
      data[2*i+1] = samples[i] & 0b0000000011111111;
     }
      for (int i = 0; i < 1024; i++) {
        Spectros.write(data[i]);
      }
      
   } 
  }
}
