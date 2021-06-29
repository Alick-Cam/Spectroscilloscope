#define nPoints 256
int f = 50;
float fs;
int samples[nPoints];
unsigned char data[2*nPoints];
float t = 0.0;
#define X1CHANNEL 0
#define X10CHANNEL 1
unsigned char channelD[2];
unsigned char samplerate[2];

void setup() {
    Serial1.begin(115200);
    Serial.begin(115200);
}

void loop() {
  // put your main code here, to run repeatedly:


  if (Serial1.available()) {
   char sCommand = Serial1.read();
   if (sCommand == 't') {
      fs = 51200.0; // max sample rate achievable (atfer testing the embedded system)
     for (int i = 0; i < nPoints ; i ++) {
      t  = (float)i/fs;
//     samples[i] = (int)(467.0*(sin(2*3.14*f*t)+1.0));
      samples[i] = (int) ((((162.63*sin(2*3.14*f*t))/100))/(4.75/1024.0))+467.0;
      data[2*i] = samples[i] >> 8;
      data[2*i+1] = samples[i] & 0b0000000011111111;
     }
      int sampleRate = 51200;
      samplerate[0] = sampleRate >>8;
      samplerate[1]  = sampleRate & 0b0000000011111111;
      int channel = X1CHANNEL;
      channelD[0] = channel >>8;
      channelD[1] = channel & 0b0000000011111111; 
      // send sample rate
      Serial1.write(samplerate[0]);
      Serial1.write(samplerate[1]);      
      // send channel
      Serial1.write(channelD[0]);
      Serial1.write(channelD[1]);           
      for (int i = 0; i < 2*nPoints; i++) {
        Serial1.write(data[i]);
      }
      
   } else if (sCommand == 'q') {
      fs = 248.0; // closest frequency to 256Hz attainable (after testing the embedded system 
      for (int i = 0; i < nPoints ; i ++) {
      t  = (float)i/fs;
//      samples[i] = (int)(467.0*(sin(2*3.14*f*t)+1.0));
      samples[i] = (int) ((((162.63*sin(2*3.14*f*t))/100))/(4.75/1024.0))+467.0;
      data[2*i] = samples[i] >> 8;
      data[2*i+1] = samples[i] & 0b0000000011111111;
      }
      int sampleRate = (int)fs;
      samplerate[0] = sampleRate >>8;
      samplerate[1]  = sampleRate & 0b0000000011111111;
      int channel = X1CHANNEL;
      channelD[0] = channel >>8;
      channelD[1] = channel & 0b0000000011111111;   
      // send sample rate
      Serial1.write(samplerate[0]);
      Serial1.write(samplerate[1]);      
      // send channel
      Serial1.write(channelD[0]);
      Serial1.write(channelD[1]);      
      for (int i = 0; i < 2*nPoints; i++) {
      Serial1.write(data[i]);
      }
  }
  }
}
