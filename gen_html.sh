#!/bin/bash

#..............................................................................
# Discover the location of this script, and identify the default set of files
# to process (all .ttl files in the parent directory) and output location.
#..............................................................................
SCRIPT="$0"
SCRIPT_HOME=$(dirname "$SCRIPT")
SOURCE_BASE="${SCRIPT_HOME}/.."
SOURCE_EXT=ttl
SOURCE_PATHS=
OUTPUT_DIR=
URL_BASE="http://purl.org"
URL_PATH_CHAR=

#..............................................................................
# Check that the command-line jar has been generated.
#..............................................................................
LODE_JAR=$(ls ${SCRIPT_HOME}/build/LODE-*.jar)
if [ ! -f "$LODE_JAR" ]
then
	echo "Unable to locate the LODE jarfile under '${SCRIPT_HOME}/build/'. Have you run the 'jar' Ant build task?"
	exit 1
fi

#..............................................................................
# Parse command line arguments for any overrides.
#..............................................................................
while getopts "h?f:e:d:o:u:c:" opt; do
	case $opt in
	h|\?)
		echo "usage: gen_rdf.sh [ -f <rdf_or_ttl_file>  | -d <rdf_or_ttl_dir> ] [ -e <rdf_or_ttl_extension> ] [ -o <output_dir> ] [ -u <url_base> ] [-c <url_path_char> ]"
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
	SOURCE_PATHS=$(ls ${SOURCE_BASE}/*.${SOURCE_EXT})
fi
for path in $SOURCE_PATHS
do 
	no_ext_path=${path%.rdf}
	if [[ "${path}" == "${no_ext_path}" ]] then
		no_ext_path=${path%.ttl}
	fi
	output="${no_ext_path}.htm"
	if [ ! -z "${OUTPUT_DIR}" ]
	then
		output_file=$(basename "${output}")
		output="${OUTPUT_DIR}/${output_file}"
	fi
	url_path=$(basename "${no_ext_path}")
	if [ ! -z "$URL_PATH_CHAR" ]
	then
		url_path=$(echo $url_path | sed -e "s/${URL_PATH_CHAR}/\//g")
	fi
	url="${URL_BASE}/${url_path}"
	echo "Creating the HTML for '${url}' from '${path}'"
    java -jar $LODE_JAR -url "${url}" -path "${path}" -html "${output}"
done

