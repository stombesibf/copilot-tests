export PODCLIENT=`oc get pods | grep "client" | grep "Running" | awk '{ print $1 }'`
