@ECHO OFF
TITLE KSP Kessler Client

ECHO Uploading save to server...
CALL kessler\launcher.bat -jar kessler/client.jar put saves/default/persistent.sfs
ECHO Downloading merged save...
CALL kessler\launcher.bat -jar kessler/client.jar get saves/default/persistent.sfs
ECHO Done!
