#include <SoftwareSerial.h>
SoftwareSerial BTSerial(10, 11); // RX | TX
unsigned char data[32] = {0,0,3,255,3,255,0,0,0,0,3,255,3,255,0,0,0,0,3,255,3,255,0,0,0,0,3,255,3,255,0,0};


void setup()
{
  Serial.begin(9600);

 
// for (int i = 0 ; i < 32; i++)
// Serial.println(data[i]);
  uint16_t arr[16];
  uint16_t a;
  //convert to 2 byte data
  for(int i = 0; i < 16; i++) {
      a = 0;
      a = data[2*i]<<8;
      arr[i] = a + data[2*i+1];
    Serial.println(arr[i]);
  }
  
 
  BTSerial.begin(9600);  // HC-05 default speed in AT command more
}

void loop()
{
  // Feed any data from bluetooth to Terminal.
  if (BTSerial.available()) {
   if (BTSerial.read() == 't') {
      for(unsigned char x = 0; x<32; x++) {
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
