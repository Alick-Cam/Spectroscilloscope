#include <SPI.h>
#define SPI_CLOCK 4000000 // 4MHz(3.6MHz) required by the MCP3004
#define chipSelectPin 10

void setup() {
  Serial.begin(9600);
  pinMode(A0, INPUT);
   SPI_setup(); 
}

void loop() {
  //collect 
  long reading = 0;
  unsigned long starttime = millis();
  for(int i = 0; i < 256; i++) {
    reading += readADC(0);
//  reading += analogRead(A0);
    delay(4);
  }
  unsigned long timeelapsed = millis()-starttime;
  Serial.println(reading/256);
  Serial.println(timeelapsed);
  Serial.print("Sample frequecy - ");Serial.println(1/((timeelapsed/1000.0)/256.0));
  delay(1000);
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

void SPI_setup() {
  SPI.begin();
  SPI.beginTransaction(SPISettings(SPI_CLOCK, MSBFIRST, SPI_MODE0));
  pinMode(chipSelectPin, OUTPUT);
  digitalWrite(chipSelectPin, HIGH);  //Immediately set CS (slave select) high so the ADC isn't selected on startup.
}
