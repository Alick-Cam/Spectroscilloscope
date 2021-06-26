#include <SPI.h>
#include <SoftwareSerial.h>
SoftwareSerial Spectros(14, 15); // RX | TX
#define SPI_CLOCK 4000000 // 4MHz(3.6MHz) required by the MCP3004
#define chipSelectPin 10
#define HF 200000
#define LF 256
#define nPoints 256
#define X1CHANNEL 0
#define X10CHANNEL 1
int samples1[nPoints];
int samples10[nPoints];
unsigned char data[2*nPoints];
unsigned char channelD[2];
void setup() {
  // put your setup code here, to run once:
  SPI_setup();
  Serial.begin(115200);
  Spectros.begin(115200); 
}

void loop() {
  /*
  Wait for cature signal from Android device (t => HF, q => LF
  Sample from x10 channel 
  Sample from x1 channel 
  Determine which channel has better resolution
  Send Channel 
  Send data
  */
   if (Spectros.available()) {
   char sCommand = Spectros.read();
   if (sCommand == 't') {
    // use HF
    for (int i = 0; i < nPoints ; i ++) {
      samples1[i] = readADC(X1CHANNEL);
      delayMicroseconds(10); // 200kSPS
    }
    for (int i = 0; i < nPoints ; i ++) {
      samples10[i] = readADC(X10CHANNEL);
      delayMicroseconds(10); // 200kSPS
    }
    int channel = chooseChannel();
    toUnsignedChar(channel);
    channelD[0] = channel >>8;
    channelD[1] = channel & 0b0000000011111111;
    // send channel
    Spectros.write(channelD[0]);
    Spectros.write(channelD[1]);
    for (int i = 0; i < 2*nPoints; i++) {
      Spectros.write(data[i]);
    }
      
   }else if (sCommand == 'q') {
    // Use LF
    for (int i = 0; i < nPoints ; i ++) {
      samples1[i] = readADC(X1CHANNEL);
      delay(4); // 256SPS
    }
    for (int i = 0; i < nPoints ; i ++) {
      samples10[i] = readADC(X10CHANNEL);
      delay(4); // 256SPS
    }
    int channel = chooseChannel();
    toUnsignedChar(channel);
    channelD[0] = channel >>8;
    channelD[1] = channel & 0b0000000011111111;
    // send channel
    Spectros.write(channelD[0]);
    Spectros.write(channelD[1]);
    
    for (int i = 0; i < 2*nPoints; i++) {
      Spectros.write(data[i]);
    }    
   }
  }

}

// function to convert samples from ADC into an array of 8 bit chars. Every 2 byte represents a single value obtained from the ADC
void toUnsignedChar(int channel) {
  for (int i = 0; i < nPoints; i++) {
    if(channel == X10CHANNEL) {
    data[2*i] = samples10[i] >> 8;
    data[2*i+1] = samples10[i] & 0b0000000011111111;
    }
    else {
    data[2*i] = samples1[i] >> 8;
    data[2*i+1] = samples1[i] & 0b0000000011111111;      
    }
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

unsigned char chooseChannel() {
  // channel with X10 gain has more priority
  unsigned char select;
  int inRangeCount10 = 0;
  for (int i = 0; i < nPoints; i++) {
    if (samples10[i] < 1023 && samples10[i] > 0) {
      inRangeCount10++;
    }
  }
  if(inRangeCount10 == nPoints){
    select = X10CHANNEL;
  } else {
    select = X1CHANNEL;
  }
  return select;
}
