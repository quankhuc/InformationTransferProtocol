package communication;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jssc.*;

public class MsgReceiver {
	
	private static final int filter_length = 5;

	final private SerialComm port;
	
	final byte MAGIC_NUMBER = 0x21;
	final byte INFO_KEY = 0X30;
	final byte HIGH_ALARM = 0x31;
	final byte TIMESTAMP_KEY = 0x32;
	final byte POTENTIOMETER_KEY = 0x33;
	final byte RAW_TEMP_KEY = 0x34;

	enum State {
		READ_MAGIC,
		READ_ERRORSTRING,
		READ_KEY,
		READ_TIMESTAMP_VALUE,
		READ_POTENTIOMETER_VALUE,
		READ_TEMP_VALUE,
		READ_INFO_LENGTH,
		READ_INFO_VALUE, 
		READ_HIGH_ALARM_LENGTH, 
		READ_HIGH_ALARM,
	}

	
	public MsgReceiver(String portname) throws SerialPortException {
		port = new SerialComm(portname);
		port.setDebug(true);
	}
	
	

	public void run() throws SerialPortException, IOException {
		// insert FSM code here to read msgs from port
		// and write to console
		State state = State.READ_MAGIC;
		int stringindex = 0;
		int voltageindex = 0;
		int rawindex = 0;
		int timesindex = 0;
		int stringorder = 0;
		long voltagevalue = 0;
		int rawcount = 0;
		long timestamp = 0;
		long wordlength = 0;
		long timesvalue = 0;
		int count = 0;
		int high_alarm_index = 0;
		int high_alarm_order = 0;
		long high_alarm_length = 0;
		int high_alarm_count = 0;
		
		
		
		while(true) {
			if(port.available()) {
				byte b = port.readByte();
//				receiving a byte from the Arduino

				// Note, so long as we only read a byte in the above call, then this code is completely non-blocking.
				// If we were to call readByte again below, it would be a blocking call which would pause program
				// execution while waiting to receive a byte from the serial port.  We don't want to do this when
				// we write non-blocking code.  In order to read multiple bytes to compose our data value, we instead
				// define an index and value variable to keep track of our state during processing.  When we finally
				// receive a full voltage sample (timestamp + voltage value), then we can print it to the console.

				// Process the byte in a finite-state machine (FSM) according to our communication protocol.
				// Our protocol is as follows:
				//    1-byte header:     Magic number
				//    Variable payload:  Key-Value pair
				switch (state) {
				// Read the 1-byte header (i.e. the magic number).
				case READ_MAGIC:
					if (b == MAGIC_NUMBER) {
						state = State.READ_KEY;
					}
					else
					{
//						System.out.print((char) b);
						state = State.READ_ERRORSTRING;
					}
					break;

				// Read the key portion of the payload.
				case READ_KEY:
					// Interpret our protocol key.
					switch (b) {
					case INFO_KEY:
//						System.out.print("INFO_KEY");
						state = State.READ_INFO_LENGTH;
						stringindex = 0;
						stringorder = 0;
						wordlength = 0;
						count = 0;
						break;
					case TIMESTAMP_KEY:
						state = State.READ_TIMESTAMP_VALUE;
						// Initialize our state variables here every time!
						timesindex = 0;
						timesvalue = 0;
						timestamp = 0;
						break;
					case POTENTIOMETER_KEY:
						state = State.READ_POTENTIOMETER_VALUE;
						// Initialize our state variables here every time!
						voltageindex = 0;
						voltagevalue = 0;
						break;
					case RAW_TEMP_KEY:
						state = State.READ_TEMP_VALUE;
						rawindex = 0;
						rawcount = 0;
						break;
					case HIGH_ALARM:
						state = State.READ_HIGH_ALARM_LENGTH;
						high_alarm_index = 0;
						high_alarm_order = 0;
						high_alarm_length = 0;
						high_alarm_count = 0;
						break;	
					default:
						state = State.READ_MAGIC;
						break;
					}
					break;
				//Print out error message	
				case READ_ERRORSTRING:
					System.out.println("!!!Error!!! " + "This is an error message: " + "(" + String.format("%02x",b) + ")");
					state = State.READ_MAGIC;
					break;
				

				// Read the timestamp value in our payload.
				case READ_TIMESTAMP_VALUE:
					timesvalue = (timesvalue << 8) | (b & 0xff);
					++timesindex;
					if (timesindex == 4) {
						// We've read all 4 bytes, so save the timestamp.  We will print it later.
						timestamp = timesvalue;
						System.out.print("This is the time stamp: ");
						System.out.print(timestamp + " ms");
						System.out.println();
						state = State.READ_MAGIC;
					}
					break;
				//Read the string info sent from the Arduino
				
				case READ_INFO_LENGTH:
					stringorder = (b & 0xff);
					wordlength = stringorder;
//					System.out.println("THIS IS WORDLENGTH BEFORE GOING INTO OTHER STATE: " + wordlength);
					if(stringindex < 1) 
					{
					++stringindex;
					}
					else if(stringindex == 1) 
					{
						state = State.READ_INFO_VALUE;
					}		
					//need to know when we can get out of reading characters. Have READ_INFO_LENGTH state separate from READ_INFO then make a READ_INFO_VALUE
					break;
					
				case READ_INFO_VALUE:
					byte[] input = {b};
					count ++;
//					System.out.println("THIS IS WORD LENGTH: " + wordlength);
//					System.out.println("THIS IS COUNT: " + count);
					if(count >= wordlength) {
						String lastword = new String(input, StandardCharsets.UTF_8);
						System.out.print(lastword);
						System.out.println();
						state = State.READ_MAGIC;
						break;
					}
					String c = new String(input, StandardCharsets.UTF_8);
					System.out.print(c);
					break;
					
				// Read the voltage value in our payload.
				case READ_POTENTIOMETER_VALUE:
					voltagevalue = (voltagevalue << 8) | (b & 0xff);
					++voltageindex;
					if (voltageindex == 2) {
						// We've read all 2 bytes, so re-compose the voltage value.  Then print the timestamp 
						// and voltage to the console.
						float voltage = (float)voltagevalue / 100;
						System.out.print("This is voltage: ");
						System.out.print(voltage + " V");
						System.out.println();
						state = State.READ_MAGIC;
					}
					break;
				// Read the temp value in our payload
				case READ_TEMP_VALUE:
					rawcount = (rawcount << 8) | (b & 0xff);
					++rawindex;
					if (rawindex == 2) {
					int raw = rawcount;
						System.out.print("This is the raw A/D counts: ");
						System.out.print(raw + " counts");
						System.out.println();
						float temperature = filterTemp(raw);
						System.out.print("This is the temperature: ");
						System.out.print(temperature + " Celcius");
						System.out.println();
						state = State.READ_MAGIC;
					}
					break;
				
				//Read the high alarm if the voltage is above 3V
				case READ_HIGH_ALARM_LENGTH:
					high_alarm_order = (b & 0xff);
					high_alarm_length = high_alarm_order;
//					System.out.println("THIS IS WORDLENGTH BEFORE GOING INTO OTHER STATE: " + wordlength);
					if(high_alarm_index < 1) 
					{
					++high_alarm_index;
					}
					else if(high_alarm_index == 1) 
					{
						state = State.READ_HIGH_ALARM;
					}		
					//need to know when we can get out of reading characters. Have READ_INFO_LENGTH state separate from READ_INFO then make a READ_INFO_VALUE
					break;
				
				case READ_HIGH_ALARM:
					byte[] high_alarm_input = {b};
					high_alarm_count++;
					if(high_alarm_count >= high_alarm_length) {
						String lastword = new String(high_alarm_input, StandardCharsets.UTF_8);
						System.out.print(lastword);
						System.out.println();
						state = State.READ_MAGIC;
						break;
					}
					String high_alarm = new String(high_alarm_input, StandardCharsets.UTF_8);
					System.out.print(high_alarm);
					break;
					
					
				// make default state is reading the magic number
				default:
					state = State.READ_MAGIC;
					break;
				}
		}

			// We can do other processing here if we want since the above processing is non-blocking.
		}
	}
			
public static float filterTemp(int rawtemp) {
	rawtemp = 5 * rawtemp;   
	double Vraw = (double)rawtemp / 1023; // convert it from mV to V and calculate it as percentage of 5V
	double temperature = (double) (25 + (Vraw - 0.75)*100);
	double arr[] = new double [filter_length];
	double sum = 0;
	for(int i = 0; i < filter_length; i++) {
		arr[i] = temperature;
		sum = sum + temperature;
//		System.out.println("THIS IS THE SUM OF RAW TEMPERATURE: " + sum);
	}
	double avgtemp = sum/filter_length;
	return (int) avgtemp;
}

	public static void main(String[] args) throws SerialPortException, IOException {
		MsgReceiver msgr = new MsgReceiver("COM3"); // Adjust this to be the right port for your machine
		msgr.run();
	}
}
