if [ "$#" -ne 3 ]; then
	echo "Usage: $0 <Stage> <MessageType> <File>"
	exit 1
fi

STAGE=$1
MESSAGETYPE=$2
FILE=$3

curl -v -s -H "X-MessageType: $MESSAGETYPE" http://stream-broker-arch-as-md-$STAGE.$OPENSHIFT_API_URL/masterdata -F file=@$FILE 
