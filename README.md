AEOSRufnummer
=============

Andorid App to turn on/off service call forwarding to personal mobil number. 



# Configuration

## Permission in your phone 
You need to allow all listed permissions for this application.  

## duty plan  
The duty plan is defined at file `/storage/emulated/0/Documents/aeos_call/aeos_dutyplan.csv`, this
file must be existed before start application. 

The first line must be `name;kw1;kw2;kw3....kw52`,  You can find the example 
at [aeos_dutyplan.csv](doc/aeos_dutyplan.csv) 

## Application configuration 
see more at [app.properties](app/src/main/assets/app.properties)

### personal mobil number
```
phone.u1=011111
phone.u2=021111
...
```
### Call forwarding Pattern
see more at [vodafone service code](./doc/Service-Codes-vodafone.pdf)
```
call.forwarding.auto.vodafone=**21*Zielrufnummer#
call.forwarding.stop.vodafone=##21#
```
## Runtime properties

The runtime properties file `/storage/emulated/0/Documents/aeos_call/app_runtime.properties`
keeps the values from last execution, this file is generated at runtime.  


# Work flow
The switching action is happened at every `Monday` between 06:00 to 08:00.

Steps
- Search for the responsible person in the duty roster for the current week
- Enable call forwarding to the responsible person's mobile phone 
- send sms to last and new responsible person  

Exception  
- when there is no responsible person for current week or there is no phone number (due to misconfiguration), 
  this app send sms (`sms.alarm.no.person`) to every person who is listed 
  at [app.properties](app/src/main/assets/app.properties)




# Development  
Update/Create `local.properties` file after git clone. 

```
## This file is automatically generated by Android Studio.
# Do not modify this file -- YOUR CHANGES WILL BE ERASED!
#
# This file should *NOT* be checked into Version Control Systems,
# as it contains information specific to your local configuration.
#
# Location of the SDK. This is only used by Gradle.
# For customization when using a Version Control System, please read the
# header note.
sdk.dir=C\:\\Users\\homeuser\\AppData\\Local\\Android\\Sdk
```

## Folder structure  

`app/lib/src/...` : test code 

`app/src/main/...`: main code


## Working at android sdk

## Run App 
Connect Phone via USB

At top bar, select `app` and device, then `Run App`  

## Install app to your phone 
