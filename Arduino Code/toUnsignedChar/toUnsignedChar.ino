/*
  Code written to develop function to convert samples from ADC into an array of 8 bit chars. Every 2 byte represents a single value obtained from the ADC
 */
#define nPoints 16
//int samples[nPoints]  = {0, 600, 512, 300, 600, 250, 450, 350, 250, 700, 600, 0 , 1 , 700, 400, 456};
int samples[nPoints] = {0, 1023, 1023, 0, 0, 1023, 1023, 0, 0, 1023, 1023, 0, 0, 1023, 1023, 0};
unsigned char data[2 * nPoints];


void toUnsignedChar() {
  for (int i = 0; i < nPoints; i++) {
    data[2*i] = samples[i] >> 8;
    data[2*i+1] = samples[i] & 0b0000000011111111;
  }
}
void setup() {
  // put your setup code here, to run once:
 Serial.begin(9600);
 toUnsignedChar();
 for(int i = 0 ; i < 2*nPoints; i++) 
 Serial.println(data[i]);

}

void loop() {
  // put your main code here, to run repeatedly:
 
}
