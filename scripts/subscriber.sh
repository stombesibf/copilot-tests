#!/bin/bash

# ----- UTILS ----- #

# -- Mejor visualización -- #
# Color variables
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color
BOLD='\033[1m'


# --  Para volver al directorio donde se ejecuta -- #

# Store the original directory
original_dir=$(pwd)

# Function to return to the original directory
function return_to_original_dir {
    cd "$original_dir"
}

# Set trap to return to the original directory on script exit
trap return_to_original_dir EXIT


# ---- SETEO DE VARIABLES ARO ---- #

. ../it-archi-south-cone.global/setenv.sh

# ---- SETEO DE VARIABLES DE SCRIPT ---- #

# Check if a stage is provided; if not, use 'dev' as default and inform the user.

STAGE=${1:-"dev"}
KAMEL_INTEGRATION_NAME=su-md-masterdata-csv
PROJECT_NAME=it-archi-south-cone.arch-as-md

# ----- INICIO ---- #

# Aclaramos el stage default.
if [ -z "$1" ]; then
    echo -e "${YELLOW}No stage specified, defaulting to 'dev'${NC}."
fi

# Navegamos a manifests.
if ! cd ../$PROJECT_NAME.manifests; then
    echo -e "${RED}Failed to change directory to: ${BOLD}$PROJECT_NAME.manifests."
    exit 1
fi

# Validamos checkout de branch.
echo -e "About to switch to branch: ${BOLD}$KAMEL_INTEGRATION_NAME-${YELLOW}$STAGE${NC} in ${BOLD}$PROJECT_NAME.manifests${NC}."
echo -ne "${YELLOW}Do you want to proceed? (y/n): ${NC}"
read -n 1 -r REPLY
echo

if [[ $REPLY =~ ^[Yy]$ ]]; then
	# Switch de branch en manifests.
	if ! git switch -q $KAMEL_INTEGRATION_NAME-$STAGE; then
		echo -e "${RED}Failed to switch to branch: ${BOLD}$KAMEL_INTEGRATION_NAME-$STAGE."
		exit 2
	fi
	echo -e "Checkout in ${BOLD}$PROJECT_NAME.manifests ${GREEN}successful.${NC}"

	# Mapeo 'dev' to 'develop' y 'prod' to 'master' para el switch en it-archi-south-cone.global
	GLOBAL_STAGE=$STAGE
	if [ "$STAGE" == "dev" ]; then
		GLOBAL_STAGE="develop"
	elif [ "$STAGE" == "prod" ]; then
		GLOBAL_STAGE="master"
	fi

	# Navegamos a global.
	if ! cd ../$GLOBAL_PROJECT_NAME; then
		echo -e "${RED}Failed to change directory to: ${BOLD}$GLOBAL_PROJECT_NAME."
		exit 4
	fi

	# Switch de branch en global.
	echo -e "About to switch to branch: ${YELLOW}$GLOBAL_STAGE${NC} in ${BOLD}$GLOBAL_PROJECT_NAME${NC}."
	if ! git switch -q $GLOBAL_STAGE; then
		echo -e "${RED}Failed to checkout ${BOLD}$GLOBAL_STAGE in $GLOBAL_PROJECT_NAME."
		exit 5
	fi
	echo -e "Checkout in ${BOLD}$GLOBAL_PROJECT_NAME ${GREEN}successful.${NC}"

	# Navegamos de vuelta al directorio donde ejecutamos el script.
	if ! cd ../$PROJECT_NAME; then
		echo -e "${RED}Failed to change directory back to: ${BOLD}$PROJECT_NAME."
		exit 3
	fi


	# Chequeamos si el proyecto de OpenShift es el correcto.
	CURRENT_OC_PROJECT=$(oc project -q)
	echo -e "Current OpenShift project is: ${YELLOW}${BOLD}$CURRENT_OC_PROJECT${NC}"

	while true; do
		echo -ne "${YELLOW}Do you want to proceed with this project? (y/n): ${NC}"
		read -n 1 -r REPLY
		echo

		case $REPLY in
			[Yy]* ) 
				echo -e "Proceeding with ${BOLD}$CURRENT_OC_PROJECT project.${NC}"
				break
				;;
			[Nn]* ) 
				echo "Exiting script."
				exit
				;;
			* ) 
				echo -e "Please answer ${BOLD}y (yes) or n (no)${NC}."
				echo -e "Current OpenShift project is: ${YELLOW}${BOLD}$CURRENT_OC_PROJECT${NC}"
				;;
		esac
	done

# ----------------------------------------------------------------- #
# -------------------- COMANDO KAMEL ------------------------------ #
# ----------------------------------------------------------------- #

	kamel run src/MasterData/CsvSubscriber.java \
		--property file:../$GLOBAL_PROJECT_NAME/global.properties \
		--property file:../$PROJECT_NAME.manifests/config/application.properties \
		--resource file:../$PROJECT_NAME.manifests/resources/mapinfo.json \
		-v sdata-storage:/sdata/ \
		--maven-repository $NEXUS_URL \
		-d mvn:com.danone:mapping:1.0.5 \
		-d mvn:com.danone:utils:1.0.0 \
		--trait jvm.options=-Duser.timezone=America/Argentina/Buenos_Aires \
		$2

else
    # Volvemos al directorio original
    cd -

    # Salimos del script
    echo -e "${YELLOW}Branch switch cancelled. Exiting script${NC}."
    exit
fi