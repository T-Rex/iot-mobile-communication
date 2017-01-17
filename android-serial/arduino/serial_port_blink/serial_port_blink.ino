
bool state = false;

void setup() {
  pinMode(LED_BUILTIN, OUTPUT);
  Serial.begin(9600);
  while (!Serial) {}
}

void loop() {
  if (Serial.available() > 0) {
    for(int i = 0; i < 3; i++) {
      digitalWrite(LED_BUILTIN, HIGH);
      delay(50); 
      digitalWrite(LED_BUILTIN, LOW);
      delay(50); 
    }
    state = (bool) Serial.read();
  }
  digitalWrite(LED_BUILTIN, state ? HIGH : LOW);
  delay(100);  
}
