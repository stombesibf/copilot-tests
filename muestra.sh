FILE=`oc exec $PODCLIENT -- ls -ltr /sdata/Incoming/ | tail -1 | awk '{ print $9 }'`
oc exec $PODCLIENT -- head /sdata/Incoming/$FILE

echo ""

FILE=`oc exec $PODCLIENT -- ls -ltr /sdata/Outgoing/ | tail -1 | awk '{ print $9 }'`
oc exec $PODCLIENT -- head /sdata/Outgoing/$FILE

