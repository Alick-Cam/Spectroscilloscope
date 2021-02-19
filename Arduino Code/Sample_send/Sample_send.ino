/*
 * Code written to send actual data sampled with the MCP3004 to the android device for processing
 */

#include <SPI.h>
#define SPI_CLOCK 4000000 // 4MHz(3.6MHz) required by the MCP3004
#define chipSelectPin 10


#include <SoftwareSerial.h>
SoftwareSerial BTSerial(8, 9); // RX | TX


#define nPoints 16
unsigned char data[2 * nPoints];
int samples[nPoints];


void setup() {
  // put your setup code here, to run once:
  Serial.begin(9600);
  BTSerial.begin(9600);  // HC-05 default speed in AT command mode
  SPI_setup();
  
}

void loop() {
  // put your main code here, to run repeatedly:
  // collect samples
  for(int i = 0; i < nPoints; i++) {
    samples[i] = readADC(0);
    delayMicroseconds(10);
  }
  // process for transmission 
  toUnsignedChar();
  // transfer processed samples 
  while(!BTSerial.available());
  if (BTSerial.available()) {
   if (BTSerial.read() == 't') {
      for(unsigned char x = 0; x<2*nPoints; x++) {
      Serial.println("In the loop");
      BTSerial.write(data[x]);
      }  
   }
  }
}

// function to convert samples from ADC into an array of 8 bit chars. Every 2 byte represents a single value obtained from the ADC
void toUnsignedChar() {
  for (int i = 0; i < nPoints; i++) {
    data[2*i] = samples[i] >> 8;
    data[2*i+1] = samples[i] & 0b0000000011111111;
  }
}

// function to setup SPI for communication with MCP3004
void SPI_setup() {
  SPI.begin();
  SPI.beginTransaction(SPISettings(SPI_CLOCK, MSBFIRST, SPI_MODE0));
  pinMode(chipSelectPin, OUTPUT);
  digitalWrite(chipSelectPin, HIGH);  //Immediately set CS (slave select) high so the ADC isn't selected on startup.
}

int readADC(byte channel){
  byte startBit = 0b00000001;
  byte controlByte = 0b10000000 | (channel << 4); // First bit 1 gives us single-ended mode on ADC; the next three bits represent the ADC's analog input
  byte flushByte = 0b00000000; // Flush the ADC to get the remaining byte output.
  
  digitalWrite(chipSelectPin, LOW); //Initiate SPI protocol by dropping slave select low.
  SPI.transfer(startBit); // Fire off our start bit.
  byte readingH = SPI.transfer(controlByte);  // Push in our control byte which tells the ADC what mode to use and what channel we want.
  byte readingL = SPI.transfer(flushByte);  // Get the rest of our output from the ADC by flushing it with a byte of 0s.
  digitalWrite(chipSelectPin, HIGH);  // After flushing, immediately deselect the chip so that it doesn't continue.

  int reading = ((readingH & 0b00000011) << 8) + (readingL); // Per datasheet, we know that only the last two bits of our first transfer contain useful info. The second byte is all useful.

  return reading;
}
