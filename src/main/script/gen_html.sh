#!/bin/bash

#..............................................................................
# Discover the location of this script, and identify the default set of files
# to process (all .ttl files in the parent directory) and output location.
#..............................................................................
SCRIPT="$0"
SCRIPT_HOME=$(dirname "$SCRIPT")
SOURCE_BASE="${SCRIPT_HOME}"
SOURCE_EXT=ttl
SOURCE_PATHS=
OUTPUT_DIR=
URL_BASE="http://purl.org"
URL_PATH_CHAR=
LODE_HOME="http://www.essepuntato.it/lode/"

#..............................................................................
# Check that the command-line jar lives in the same directory as this script.
#..............................................................................
LODE_JAR=$(ls ${SCRIPT_HOME}/LODE-*.jar)
if [ ! -f "$LODE_JAR" ]
then
	echo "Unable to locate the LODE jarfile under '${SCRIPT_HOME}'. Have you run the 'jar' Ant build task?"
	exit 1
fi

#..............................................................................
# Parse command line arguments for any overrides.
#..............................................................................
while getopts "h?f:e:d:o:u:c:l:" opt; do
	case $opt in
	h|\?)
		echo "usage: ${SCRIPT} [ -f <rdf_or_ttl_file>  | -d <rdf_or_ttl_dir> ] [ -e <rdf_or_ttl_extension> ] [ -o <output_dir> ] [ -u <url_base> ] [-c <url_path_char> ] [ -v <vis_base>]"
		exit 0
		;;
	f)
		SOURCE_PATHS=$OPTARG
		;;
	e)
		SOURCE_EXT=$OPTARG
		;;
	d)
		SOURCE_BASE=$OPTARG
		;;
	o)
		OUTPUT_DIR=$OPTARG
		;;
	u)
		URL_BASE=$OPTARG
		;;
	c)
		URL_PATH_CHAR=$OPTARG
		;;
    l)
        LODE_HOME=$OPTARG
        ;;
	?)
		echo "Invalid option: $OPTARG" 
		exit 1
		;;
	esac
done
shift $((OPTIND - 1))

#..............................................................................
# Convert and save the identified vocabulary definition files.
#..............................................................................
if [ -z "$SOURCE_PATHS" ]
then
	SOURCE_PATHS=$(ls "${SOURCE_BASE}"/*.${SOURCE_EXT})
fi

# Use newlines as the delimiter to identify listed files instead of any whitespace.
# This ensures the loop works even when there are spaces in filenames.
OLD_IFS=$IFS
IFS=$(echo -en "\n\b")

for path in $SOURCE_PATHS
do
    echo "Processing source path '${path}'" 
	no_ext_path="${path%.rdf}"
	if [[ "${path}" == "${no_ext_path}" ]] 
	then
		no_ext_path="${path%.ttl}"
	fi

	url_path=$(basename "${no_ext_path}")
	if [ ! -z "$URL_PATH_CHAR" ]
	then
		url_path=$(echo "${url_path}" | sed -e "s/${URL_PATH_CHAR}/\//g")
	fi
	url="${URL_BASE}/${url_path}"

	output="${no_ext_path}.htm"
	if [ -z "${OUTPUT_DIR}" ]
	then
		# saving to the same directory that the definition file lives in.
		# use a relative source path in the generated html.
		source="${url_path}.rdf"
	else
		# saving to a different directory than the definition file.
		output_file=$(basename "${output}")
		output="${OUTPUT_DIR}/${output_file}"
		source="${url}.rdf"
	fi
    
    cmd="java -jar $LODE_JAR -url '${url}' -path '${path}' -source '${source}' -html '${output}'"
    if [ -n "${LODE_HOME}" ]; then
      cmd="${cmd} -sourceBase '${LODE_HOME}source?url=' -visBase '${LODE_HOME}owlapi/' -lodeHome '${LODE_HOME}'"
    fi
	echo "Creating the HTML for '${url}' from '${path}'"
    echo "${cmd}"
    ${cmd}
    
done

# Reset the default loop delimiter.
IFS=$OLD_IFS

