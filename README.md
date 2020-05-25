# Create_Lib_For_BLE_Central_Android

Java App running on Android. acting likes a BLE central (client) and has a loop: Scan for "BLE" broadcast-ed by app1 Connect and discover service a writable characteristic from app 1 Send "RED" to this characteristic Sleep 1s Send "GREEN" sleep 1s Disconnect continue the loop. (jump to step 1)

Design Requirements

Library: contains only the business logic (Bluetooth, events...) - no GUI in the lib, will produce the APIs wrapped in an AAR file.
Application: consumes the library, take the logic from the library and use it to display on the GUI.
