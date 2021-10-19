# InformationTransferProtocol
This project defined communication protocol to communicate information strings, error strings, timestamps of
measurements, potentiometer readings, and raw temperature data between the Arduino board and a computer, such
as laptop. UTF-8 format was used to encode the information and error strings. Any extreme measurements from the
potentiometer and temperature sensor generated error strings. The timestamps of measurements were the recorded
time when measurements were taken. Two analog sensors were used to obtain the potentiometer and raw
temperature values. The raw temperature data was then processed at the other end to display temperature in
Celsius. The timestamps, potentiometer readings, and the raw temperatures were sent in a 1Hz delta-time loop

Link to the assignment: https://classes.cec.wustl.edu/~SEAS-SVC-CSE132/weeks/7/assignment/
