#!/bin/sh
jarfile=$(find /data -name otp-\*-shaded.jar)
echo $jarfile
java -Xmx16G -jar "${jarfile}" "$@"


