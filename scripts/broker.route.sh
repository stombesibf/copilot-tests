
oc delete route.route.openshift.io/stream-broker service/stream-broker

POD=`oc get pods | grep "stream-broker" | awk '{ print $1 }'`
oc expose pod/$POD --port=8080 --name=stream-broker
oc expose svc stream-broker
