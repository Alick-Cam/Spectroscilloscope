#include <SoftwareSerial.h>
SoftwareSerial BTSerial(10, 11); // RX | TX
unsigned char data[100];
unsigned char trigger;
void setup()
{
  for(unsigned char x = 0; x<100; x++) {
  if(x>1)
  {
    if((data[x-1] == 20 && data[x-2] == 0)||(data[x-1] == 0 && data[x-2] == 0))
    data[x] = 20;
    else if ((data[x-1] == 0 && data[x-2] == 20)||(data[x-1] == 20 && data[x-2] == 20))
    data[x] = 0;
  }
  else
  data[x] = 0;
  }
  Serial.begin(9600);
  BTSerial.begin(9600);  // HC-05 default speed in AT command more
}

void loop()
{
//if (BTSerial.available()) {
//    trigger = BTSerial.read();
//}
//
//if (trigger == 't') {
//  BTSerial.write('1');
//} else 
//  BTSerial.write('0');
  // Feed any data from bluetooth to Terminal.
  if (BTSerial.available()) {
   if (BTSerial.read() == 't') {
      for(unsigned char x = 0; x<100; x++) {
      Serial.println("In the loop");
      BTSerial.write(data[x]);
      }  
   }
  }


}
