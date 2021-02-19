/*
Code writed to send hardcoded data to android device for testing
*/
#include <SoftwareSerial.h>
SoftwareSerial BTSerial(8, 9); // RX | TX
#define nPoints 16
#define rawnPoints 32
unsigned char data[2 * nPoints] = {0,0,3,255,3,255,0,0,0,0,3,255,3,255,0,0,0,0,3,255,3,255,0,0,0,0,3,255,3,255,0,0};
int samples[nPoints];

// function to convert samples from ADC into an array of 8 bit chars. Every 2 byte represents a single value obtained from the ADC
void toUnsignedChar() {
  for (int i = 0; i < nPoints; i++) {
    data[2*i] = samples[i] >> 8;
    data[2*i+1] = samples[i] & 0b0000000011111111;
  }
}
  
  
}

void setup()
{
  Serial.begin(9600);

 
// for (int i = 0 ; i < 32; i++)
// Serial.println(data[i]);
//  Serial.println();
//    Serial.println();
//      Serial.println();
//  uint16_t arr[nPoints];
//  uint16_t a; // Temporary holder
//  //convert to 2 byte data
//  for(int i = 0; i < nPoints; i++) {
//      a = 0;
//      a = data[2*i]<<8;
//      arr[i] = a + data[2*i+1];
//    Serial.println(arr[i]);
//  }
  
 
  BTSerial.begin(9600);  // HC-05 default speed in AT command more
}

void loop()
{
  // Feed any data from bluetooth to Terminal.
  if (BTSerial.available()) {
   if (BTSerial.read() == 't') {
      for(unsigned char x = 0; x<rawnPoints; x++) {
      Serial.println("In the loop");
      BTSerial.write(data[x]);
      }  
   }
  }


}


  //Populate array like ABBAABBAAB
//  for(unsigned char x = 0; x<32; x++) {
// 
//  if(x>1)
//  {
//    if((data[x-1] == 255 && data[x-3] == 0)||(data[x-1] == 0 && data[x-2] == 0))
//    {
//      data[x] = 3;
//      data[x+1] = 255;
//      x++;
//    }
//    
//    else if ((data[x-1] == 0 && data[x-2] == 255)||(data[x-1] == 0 && data[x-2] == 255))
//    {
//      data[x] = 0;
//      data[x+1] = 0;
//      x++;     
//    }
//
//  }
//  else
//  {
//      data[x] = 0;
//      data[x+1] = 0;
//      x++;   
//  }
//  Serial.println(data[x]);
//  }
