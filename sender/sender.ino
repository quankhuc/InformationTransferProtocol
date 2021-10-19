/* 
 *  
 *  CSE 132 - Assignment 7
 *  
 *  Fill this out so we know whose assignment this is.
 *  
 *  Name: Quan Khuc
 *  WUSTL Key: 488453
 *  
 *  and if two are partnered together
 *  
 *  Name:
 *  WUSTL Key:
 */

const unsigned int POTENTIOMETER_PIN = A0;
const unsigned int TEMPSENSOR_PIN = A1;

const byte MAGIC_NUMBER = 0x21;
const byte INFO_KEY = 0x30;
const byte ERROR_STRING = 0x31;
const byte TIMESTAMP_KEY = 0x32;
const byte POTENTIOMETER_KEY = 0x33;
const byte RAW_TEMP_KEY = 0x34;

const unsigned long samplePeriod = 1000;
unsigned long nextSampleTime = 0;
const unsigned long threshold_val = 3.0;


void setup() {
  // put your setup code here, to run once:
  Serial.begin(9600);
  analogReference(DEFAULT);
}

void sendMagicNumber() {
  Serial.write(MAGIC_NUMBER);
}

//void sendaCharacter(char a) {
//  // Note, Serial.write only sends 8-bit numbers, so the higher order bits (i.e. >= 8) are ignored.
//  sendMagicNumber();
//  Serial.write(INFO_KEY);
//  Serial.write(a);
//}

void sendULong(unsigned long value) {
  // Note, Serial.write only sends 8-bit numbers, so the higher order bits (i.e. >= 8) are ignored.
  Serial.write(value >> 24);
  Serial.write(value >> 16);
  Serial.write(value >> 8);
  Serial.write(value);
}

void sendTimestamp(unsigned long timestamp) {
  // Send magic number.
  sendMagicNumber();

  // Send timestamp key.
  Serial.write(TIMESTAMP_KEY);

  // Send timestamp value.
  sendULong(timestamp);
}

void sendinfo(char* s){
    int i = 0;
    while(s[i] != '\0'){
    i++;
    }
    sendMagicNumber();
    Serial.write(INFO_KEY);
//    Serial.println(INFO_KEY, HEX);
    sendwordlength(i);  
//    Serial.println(i);
    Serial.write(s);
//    Serial.print(s);
  }


void sendwordlength(int wordlength){
  Serial.write(wordlength >> 8);
//  Serial.println(wordlength >> 8);
  Serial.write(wordlength);
//  Serial.println(wordlength >> 8);
}

void sendVoltage(float voltage){
  sendMagicNumber();
  Serial.write(POTENTIOMETER_KEY);
  unsigned int value = (unsigned int)(voltage * 100.0);
  Serial.write(value >> 8);
  Serial.write(value);
}

void sendRawTemp(int temp){
  sendMagicNumber();
  Serial.write(RAW_TEMP_KEY);
  Serial.write(temp >> 8);
  Serial.write(temp);
}

void sendHighAlarm(char* high_alarm_message){
  sendMagicNumber();
  Serial.write(ERROR_STRING);
  sendwordlength(10);
  Serial.write(high_alarm_message);
}

void loop() {
  // put your main code here, to run repeatedly:
  unsigned long currentTime = millis();

  if(currentTime >= nextSampleTime){
    nextSampleTime += samplePeriod;
    char input [] = "Hi";
    char high_alarm_message[] = "HIGH ALARM";
//    int wordlength = Serial.available();
//    Serial.println(wordlength);
//    if(wordlength > 0){
//      char input[wordlength];
//      for(int i = 0; i < wordlength; i++){
//      input[i] = Serial.read();
//      Serial.println(input[i]);
//      }
//      Serial.println(sizeof(input));

      // Read the raw adc count of the voltage from the pin.
      unsigned int reading = analogRead(POTENTIOMETER_PIN);

      // Convert the raw adc count to the voltage value as a float.
      float voltage = reading * 5.0 / 1023.0;

      //Send strings
      sendinfo(input);
      //send timestamp
      sendTimestamp(currentTime);
      // Send the high alarm message
      if(voltage > threshold_val){
        sendHighAlarm(high_alarm_message);
      }
      
      //send voltage
      sendVoltage(voltage);

      // Get and Send raw temperature
      unsigned int readingcount = analogRead(TEMPSENSOR_PIN);
      sendRawTemp(readingcount);
      
    }
}
