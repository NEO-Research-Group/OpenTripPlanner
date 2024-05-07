#!/bin/sh
jarfile=$(find /data -name otp-\*-shaded.jar)
echo $jarfile
java -Xmx8G -jar "${jarfile}" "$@"


