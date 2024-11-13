Instructivo ARCH-AS-MD-DEV (Masterdata)

ACLARACIONES

Para ejecutar los comandos se utilizo GitBash en Windows para simular un ambiente UNIX. En algunos comandos se agrega MSYS_NO_PATHCONV=1 al principio del comando para evitar que los directorios se alteren automaticamente. Esto no es necesario en un ambiente UNIX normal o con otras herramientas

Algunos scripts que se ejecutan solo funcionan en un ambiente UNIX o simulado con GitBash. En general los comandos oc funcionan normalmente en CMD

CONTENIDO

Este es es el contenido de la carpeta arch-as-md-dev:

$ find arch-as-md-dev/
# CARPETA PRINCIPAL #
arch-as-md-dev/
arch-as-md-dev/README.txt
# CARPETA CONFIGURACION #
arch-as-md-dev/config
arch-as-md-dev/config/application.properties
arch-as-md-dev/config/claim1g.yaml
arch-as-md-dev/config/md-1.topic.yaml
arch-as-md-dev/config/md-2.topic.yaml
# CARPETA ARCHIVOS DE PRUEBA #
arch-as-md-dev/files
arch-as-md-dev/files/MT0003.csv
arch-as-md-dev/files/MT0003.small.csv
arch-as-md-dev/files/MT0004.csv
arch-as-md-dev/files/MT0004.small.csv
arch-as-md-dev/files/MT0006.csv
arch-as-md-dev/files/MT0006.small.csv
arch-as-md-dev/files/MT0007.csv
arch-as-md-dev/files/MT0007.small.csv
# CARPETA RECURSOS #
arch-as-md-dev/resources
arch-as-md-dev/resources/mapinfo.json
arch-as-md-dev/resources/sftpinfo.json
# CARPETA SCRIPTS #
arch-as-md-dev/scripts
arch-as-md-dev/scripts/run_broker.sh
arch-as-md-dev/scripts/run_client.sh
arch-as-md-dev/scripts/run_sftpsender.sh
arch-as-md-dev/scripts/run_subscriber.sh
arch-as-md-dev/scripts/sftp_start.sh
# CARPETA FUENTES #
arch-as-md-dev/src
arch-as-md-dev/src/Client.java
arch-as-md-dev/src/CsvSubscriber.java
arch-as-md-dev/src/SftpSender.java
arch-as-md-dev/src/StreamBroker.java

INICIAR O ACTUALIZAR SERVICIOS

# Todos los comandos se ejecutan desde la carpeta principal #
$ cd arch-as-md-dev/

# Voy al proyecto arch-as-md-dev #
$ oc project arch-as-md-dev
Now using project "arch-as-md-dev" on server "https://api.brazilsouth.ar.nead.danet:6443".

$ scripts/run_broker.sh
Modeline options have been loaded from source files
Full command: kamel run src/StreamBroker.java -w --resource file:resources/mapinfo.json -v claim1g:/sdata/ --property=file:config/application.properties --dependency=camel-netty-http --dependency=camel-jackson
Integration "stream-broker" created
Progress: integration "stream-broker" in phase Initialization
Progress: integration "stream-broker" in phase Building Kit
Condition "IntegrationPlatformAvailable" is "True" for Integration stream-broker: arch-as-md-dev/camel-k
Integration "stream-broker" in phase "Initialization"
Integration "stream-broker" in phase "Building Kit"
Condition "IntegrationKitAvailable" is "False" for Integration stream-broker: creating a new integration kit
Integration Kit "kit-cj68ibm7a8o5j6dh1rk0", created by Integration "stream-broker", changed phase to "Build Submitted"
Build "kit-cj68ibm7a8o5j6dh1rk0", created by Integration "stream-broker", changed phase to "Scheduling"
Build "kit-cj68ibm7a8o5j6dh1rk0", created by Integration "stream-broker", changed phase to "Pending"
Build "kit-cj68ibm7a8o5j6dh1rk0", created by Integration "stream-broker", changed phase to "Running"
Integration Kit "kit-cj68ibm7a8o5j6dh1rk0", created by Integration "stream-broker", changed phase to "Build Running"
Build "kit-cj68ibm7a8o5j6dh1rk0", created by Integration "stream-broker", changed phase to "Succeeded"
Integration Kit "kit-cj68ibm7a8o5j6dh1rk0", created by Integration "stream-broker", changed phase to "Ready"
Progress: integration "stream-broker" in phase Deploying
Condition "IntegrationKitAvailable" is "True" for Integration stream-broker: kit-cj68ibm7a8o5j6dh1rk0
Integration "stream-broker" in phase "Deploying"
Progress: integration "stream-broker" in phase Running
route.route.openshift.io "stream-broker" deleted
service "stream-broker" deleted
service/stream-broker exposed
route.route.openshift.io/stream-broker exposed

$ scripts/run_subscriber.sh
Modeline options have been loaded from source files
Full command: kamel run src/CsvSubscriber.java -w --resource file:resources/mapinfo.json -v claim1g:/sdata/ --property=file:config/application.properties --dependency=camel-file --dependency=camel:csv --dependency=camel-jackson --dependency=camel-dataformat
Integration "csv-subscriber" created
Progress: integration "csv-subscriber" in phase Initialization
Condition "IntegrationPlatformAvailable" is "True" for Integration csv-subscriber: arch-as-md-dev/camel-k
Integration "csv-subscriber" in phase "Initialization"
Progress: integration "csv-subscriber" in phase Building Kit
Integration "csv-subscriber" in phase "Building Kit"
Condition "IntegrationKitAvailable" is "True" for Integration csv-subscriber: kit-cj49kee7a8o5j6dh1re0
Integration "csv-subscriber" in phase "Deploying"
Progress: integration "csv-subscriber" in phase Deploying
Progress: integration "csv-subscriber" in phase Running

$ scripts/run_sftpsender.sh
Modeline options have been loaded from source files
Full command: kamel run src/SftpSender.java -w --resource file:resources/sftpinfo.json -v claim1g:/sdata/ --property=file:config/application.properties --dependency=camel-file --dependency=camel:csv --dependency=camel-jackson
Integration "sftp-sender" created
Progress: integration "sftp-sender" in phase Initialization
Progress: integration "sftp-sender" in phase Building Kit
Progress: integration "sftp-sender" in phase Deploying
Condition "IntegrationPlatformAvailable" is "True" for Integration sftp-sender: arch-as-md-dev/camel-k
Integration "sftp-sender" in phase "Initialization"
Integration "sftp-sender" in phase "Building Kit"
Condition "IntegrationKitAvailable" is "True" for Integration sftp-sender: kit-cj49lk67a8o5j6dh1reg
Integration "sftp-sender" in phase "Deploying"
Progress: integration "sftp-sender" in phase Running

$ scripts/run_client.sh
Integration "client" created
Progress: integration "client" in phase Initialization
Progress: integration "client" in phase Building Kit
Progress: integration "client" in phase Deploying
Progress: integration "client" in phase Running

$ kamel get
NAME            PHASE   KIT
client          Running arch-as-md-dev/kit-cj49nsm7a8o5j6dh1rf0
csv-subscriber  Running arch-as-md-dev/kit-cj49kee7a8o5j6dh1re0
sftp-sender     Running arch-as-md-dev/kit-cj49lk67a8o5j6dh1reg
stream-broker   Running arch-as-md-dev/kit-cj68ibm7a8o5j6dh1rk0

EJECUCION Y CONSULTA

# defino la variable PODCLIENT con el nombre del pod del servicio client #
export PODCLIENT=`oc get pods -n arch-as-md-dev | grep "client-" | grep "Running" | awk '{ print $1 }'`

# copio los archivos de prueba al pod cliente (por unica vez o cuando se reinicia ese pod) #
$ oc rsync files $PODCLIENT:. -n arch-as-md-dev
WARNING: rsync command not found in path. Download cwRsync for Windows and add it to your PATH.
files/MT0003.csv
files/MT0003.small.csv
files/MT0004.csv
files/MT0004.small.csv
files/MT0006.csv
files/MT0006.small.csv
files/MT0007.csv
files/MT0007.small.csv

# ejecuto el MessageType 0003 #
$ oc exec $PODCLIENT -n arch-as-md-dev -- curl -s -H "X-MessageType: MT0003" http://stream-broker.arch-as-md-dev:8080/masterdata -F file=@/tmp/files/MT0003.small.csv
Done

# listo las carpetas Incoming y Outgoing en el filesystem de Integracion #
$ MSYS_NO_PATHCONV=1 oc exec $PODCLIENT -- ls -ltr /sdata/Incoming /sdata/Outgoing
/sdata/Incoming:
total 2
-rwxrwxrwx. 1 root 1000880000 2012 Aug  4 05:38 MT0003.20230804053856.csv

/sdata/Outgoing:
total 4
-rwxrwxrwx. 1 root 1000880000 1916 Aug  4 05:38 MT0003.FX1.20230804053856.csv
-rwxrwxrwx. 1 root 1000880000 1406 Aug  4 05:38 MT0003.FX2.20230804053856.csv

# ejecuto el MessageType 0004 #
$ oc exec $PODCLIENT -n arch-as-md-dev -- curl -s -H "X-MessageType: MT0004" http://stream-broker.arch-as-md-dev:8080/masterdata -F file=@files/MT0004.small.csv
Done

# listo las carpetas Incoming y Outgoing en el filesystem de Integracion #
$ MSYS_NO_PATHCONV=1 oc exec $PODCLIENT -- ls -ltr /sdata/Incoming /sdata/Outgoing
/sdata/Incoming:
total 4
-rwxrwxrwx. 1 root 1000880000 2012 Aug  4 05:38 MT0003.20230804053856.csv
-rwxrwxrwx. 1 root 1000880000 1386 Aug  4 05:40 MT0004.20230804054045.csv

/sdata/Outgoing:
total 6
-rwxrwxrwx. 1 root 1000880000 1916 Aug  4 05:38 MT0003.FX1.20230804053856.csv
-rwxrwxrwx. 1 root 1000880000 1406 Aug  4 05:38 MT0003.FX2.20230804053856.csv
-rwxrwxrwx. 1 root 1000880000 2256 Aug  4 05:40 MT0004.FX.20230804054045.csv

# veo las primeras lineas del archivo de entrada y del de salida del MessageType 0004 #
$ MSYS_NO_PATHCONV=1 oc exec $PODCLIENT -- head -5 /sdata/Incoming/MT0004.20230804054045.csv /sdata/Outgoing/MT0004.FX.20230804054045.csv
==> /sdata/Incoming/MT0004.20230804054045.csv <==
"CompanyCode","Product","ProductType","ProductDescription","BaseUnit","VolumeUnit","NetWeight","GrossWeight","IsMarkedForDeletion","ProcessDate","MeasureID","MeasureunitID","Plant","ToleranceValue","District","IsGeneric","StandardPrice","SupplierAccountGroup","ValidityEndDate","MATERIALTYPEID","MATERIALGROUP","Enabled"
5700,10345033,"XXX","FIL FULL LEV ANA 2.25 DIS 0318",0,0,0,0,,,"KG","KG",5700,0,0,0,0,0,0,5700,5700,
5700,50520749,"XXX","BULO-BULON ACERO INOX.MET.7X1.00 20MM",0,0,0,0,,,"UN","UN",5700,0,0,0,0,0,0,5700,5700,
5700,129528,"XXX","LEVITE LIMONADA C/T FRUT SG 600CC",0,0,0,0,,,"BT","BT",5700,0,0,0,0,0,"20210715 00:07:00",5700,5700,
5700,50515630,"XXX","L5SO-JUEGO DE 6 BUJES DE ANCLAJE-P027",0,0,0,0,,,"UN","UN",5700,0,0,0,0,0,0,5700,5700,

==> /sdata/Outgoing/MT0004.FX.20230804054045.csv <==
"Empresa","Material","Descripcion","Medida","MedidaStock","Cubicaje","PesoNeto","PesoBruto","Planta","Tolerancia","CodProv","CONVUNIDADES","PRODGENERICO","PRECIOLANDED","CODCLI","FBAJA","GRUPO1","DESCGRUPO1","GRUPO2","DESCGRUPO2","GRUPO3","DESCGRUPO3","GRUPO4","DESCGRUPO4","GRUPO5","DESCGRUPO5","GRUPO6","DESCGRUPO6"
5700,"10345033","FIL FULL LEV ANA 2.25 DIS 0318","KG","KG","0","0","0","7100","0","0","0","0","0","0","0",7100,"7100","7100","7100","5700","5700","5700","5700","5700","5700","5700","5700"
5700,"50520749","BULO-BULON ACERO INOX.MET.7X1.00 20MM","UN","UN","0","0","0","7100","0","0","0","0","0","0","0",7100,"7100","7100","7100","5700","5700","5700","5700","5700","5700","5700","5700"
5700,"129528","LEVITE LIMONADA C/T FRUT SG 600CC","BT","BT","0","0","0","7100","0","0","0","0","0","0","20210715 00:07:00",7100,"7100","7100","7100","5700","5700","5700","5700","5700","5700","5700","5700"
5700,"50515630","L5SO-JUEGO DE 6 BUJES DE ANCLAJE-P027","UN","UN","0","0","0","7100","0","0","0","0","0","0","0",7100,"7100","7100","7100","5700","5700","5700","5700","5700","5700","5700","5700"

# modifico el archivo de transformaciones y mapeos (usar el editor de su preferencia) #
$ vi resources/mapinfo.json

# subo el nuevo archivo al filesystem de integracion #
$ oc rsync resources $PODCLIENT:/sdata
WARNING: rsync command not found in path. Download cwRsync for Windows and add it to your PATH.
resources/mapinfo.json
resources/sftpinfo.json

# vuelvo a ejecutar la integracion del MessageType 0004 #
$ oc exec $PODCLIENT -n arch-as-md-dev -- curl -s -H "X-MessageType: MT0004" http://stream-broker.arch-as-md-dev:8080/masterdata -F file=@files/MT0004.small.csv
Done

# busco el nombre de los archivos recientemente generados #
$ MSYS_NO_PATHCONV=1 oc exec $PODCLIENT -- ls -ltr /sdata/Incoming /sdata/Outgoing
/sdata/Incoming:
total 5
-rwxrwxrwx. 1 root 1000880000 2012 Aug  4 05:38 MT0003.20230804053856.csv
-rwxrwxrwx. 1 root 1000880000 1386 Aug  4 05:40 MT0004.20230804054045.csv
-rwxrwxrwx. 1 root 1000880000 1386 Aug  4 05:57 MT0004.20230804055751.csv

/sdata/Outgoing:
total 9
-rwxrwxrwx. 1 root 1000880000 1916 Aug  4 05:38 MT0003.FX1.20230804053856.csv
-rwxrwxrwx. 1 root 1000880000 1406 Aug  4 05:38 MT0003.FX2.20230804053856.csv
-rwxrwxrwx. 1 root 1000880000 2256 Aug  4 05:40 MT0004.FX.20230804054045.csv
-rwxrwxrwx. 1 root 1000880000 2236 Aug  4 05:57 MT0004.FX.20230804055751.csv

# muestro las primeras lineas del archivo de entrada y del de salida y confirmo que el cambio se refleja en el nuevo archivo de salida #
$ MSYS_NO_PATHCONV=1 oc exec $PODCLIENT -- head -5 /sdata/Incoming/MT0004.20230804055751.csv /sdata/Outgoing/MT0004.FX.20230804055751.csv
==> /sdata/Incoming/MT0004.20230804055751.csv <==
"CompanyCode","Product","ProductType","ProductDescription","BaseUnit","VolumeUnit","NetWeight","GrossWeight","IsMarkedForDeletion","ProcessDate","MeasureID","MeasureunitID","Plant","ToleranceValue","District","IsGeneric","StandardPrice","SupplierAccountGroup","ValidityEndDate","MATERIALTYPEID","MATERIALGROUP","Enabled"
5700,10345033,"XXX","FIL FULL LEV ANA 2.25 DIS 0318",0,0,0,0,,,"KG","KG",5700,0,0,0,0,0,0,5700,5700,
5700,50520749,"XXX","BULO-BULON ACERO INOX.MET.7X1.00 20MM",0,0,0,0,,,"UN","UN",5700,0,0,0,0,0,0,5700,5700,
5700,129528,"XXX","LEVITE LIMONADA C/T FRUT SG 600CC",0,0,0,0,,,"BT","BT",5700,0,0,0,0,0,"20210715 00:07:00",5700,5700,
5700,50515630,"XXX","L5SO-JUEGO DE 6 BUJES DE ANCLAJE-P027",0,0,0,0,,,"UN","UN",5700,0,0,0,0,0,0,5700,5700,

==> /sdata/Outgoing/MT0004.FX.20230804055751.csv <==
"Empresa","Material","Descripcion","Medida","MedidaStock","Cubicaje","PesoNeto","PesoBruto","Planta","Tolerancia","CodProv","CONVUNIDADES","PRODGENERICO","PRECIOLANDED","CODCLI","FBAJA","GRUPO1","DESCGRUPO1","GRUPO2","DESCGRUPO2","GRUPO3","DESCGRUPO3","GRUPO4","DESCGRUPO4","GRUPO5","DESCGRUPO5","GRUPO6","DESCGRUPO6"
5700,"10345033","FIL FULL LEV ANA 2.25 DIS 0318","KG","KG","0","0","0","7100","0","0","0","0","0","0","0",7100,7100,"7100","7100","5700","5700","5700","5700","5700","5700","5700","5700"
5700,"50520749","BULO-BULON ACERO INOX.MET.7X1.00 20MM","UN","UN","0","0","0","7100","0","0","0","0","0","0","0",7100,7100,"7100","7100","5700","5700","5700","5700","5700","5700","5700","5700"
5700,"129528","LEVITE LIMONADA C/T FRUT SG 600CC","BT","BT","0","0","0","7100","0","0","0","0","0","0","20210715 00:07:00",7100,7100,"7100","7100","5700","5700","5700","5700","5700","5700","5700","5700"
5700,"50515630","L5SO-JUEGO DE 6 BUJES DE ANCLAJE-P027","UN","UN","0","0","0","7100","0","0","0","0","0","0","0",7100,7100,"7100","7100","5700","5700","5700","5700","5700","5700","5700","5700"


MSYS_NO_PATHCONV=1 oc exec $PODCLIENT -- head -10 /sdata/Outgoing/MT0003.FX1.20230804053856.csv


# Actualizar archivo mapinfo

oc rsync <local-dir> <pod-name>:/<remote-dir>

oc rsync resources/test sftp-sender-54b4785ccb-6dj6c:/sdata -n arch-as-md-test
$ oc rsync files $PODCLIENT:. -n arch-as-md-dev

MSYS_NO_PATHCONV=1 oc exec csv-subscriber-7b59dd98c4-94nr4 -- head -5 /sdata/resources/martin.txt

oc rsync resources sftp-sender-559498bcfd-2mrwl:/sdata -n arch-as-md-dev


