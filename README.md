# hubitat-tuya-zigbee-ir
Hubitat Driver for Tuya Zigbee IR Remote Controls

# Instructions 

This driver was written for my own personal use and I consider it v0.1 proof of concept.

The integration with other Hubitat features is a bit of a quick hack just to get something that worked for my use case. 

Long term, I have a half written application I'd like to finish to go along with this that would be used to create virtual sub devices automatically to make this easier, but I haven't finished it yet.

## Learning Codes

To learn a button from your original remote:

1. On the IR device driver page, run the "Learn" command, giving it a "Code Name" for the IR code. E.x. `PowerToggle` or `VolumeUp`
2. The blue LED on the Tuya IR device should light up
3. Point the original remote control at the Tuya IR blaster and press the corresponding button on your original remote control
4. The blue LED should turn off and an entry should appear in the "Learned Codes" state section

## Testing Codes

After learning a code you can test by pointing the Tuya IR blaster at your device and running the "Send Code" command, giving it the same Code Name you provided to the "Learn" command. 

The Tuya LED light should blink and the command should be sent. 

If it doesn't work, I'd try learning the code again.

## Integration with Rule Machine or other apps

This is a bit if a hack like mentioned above.

There's no Hubitat device type that lets you send arbitrary strings like `PowerToggle` or `VolumeUp` (or the base64 encoded binary data that is the actual IR code), so the driver exposes the same interface as a Button and let's you map arbitrary button numbers to IR commands. 

1. Run the "Map Button" command, picking an arbitrary unique "Button" number (e.x `1`) and giving it the same "Code Name" you learned earlier

Now in Rule Machine (or any other app that can perform actions) you can add the action "Push Button 1 on {Tuya Device Name}" whenever you want the IR blaster to send the code. You can make a Virtual device to represent whatever it is you are controlling and then make a Rule Machine rule to push the corresponding "button" number on the Tuya device whenever the virtual button is pressed.

## Example

To turn on/off my bedroom TV, for example, I:

1. Used the Learn command to learn the IR command emit when I pressed the power on/off button on my original remote, giving it the name `PowerToggle`
2. Used the "Map Button" command to map button 1 to the `PowerToggle` command
3. Created a Virtual Switch device called "Bedroom TV" and then made a Rule Machine rule with the condition "Bedroom TV *changed*" and action "Push Button 1 on Tuya IR Blaster".

![Screenshot showing the Rule Machine rule described above](https://github.com/user-attachments/assets/42d39ca8-9441-4b54-8e9a-0f5f728d5610)

Now toggling the Bedroom TV switch (which I expose in Google Home to control from my phone) turns on/off the TV.
