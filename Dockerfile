ARG BUILD_NUMBER="docker"
ARG JAVA_VERSION="11"
ARG TOMCAT_VERSION="9.0"

#------------------------------------------------------------------------------
FROM  maven:3-openjdk-${JAVA_VERSION} as builder

ARG BUILD_NUMBER
ARG JAVA_VERSION

WORKDIR /opt

# Download and cache all Maven Dependencies
COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline \
        -DbuildNumber='${BUILD_NUMBER}' \
        -Djdk.release.version="${JAVA_VERSION}"

# Install the source code and compile the .war file
COPY ./src/ ./src/
RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package verify \
		-DbuildNumber='${BUILD_NUMBER}' \
        -Djdk.release.version="${JAVA_VERSION}"


#------------------------------------------------------------------------------
FROM builder as jetty

COPY ./configure-lode-environment.sh ./

ARG DEFAULT_LANG \
	LODE_EXTERNAL_URL \
	MAX_TENTATIVE \
	VENDOR_CSS \
	VENDOR_NAME \
	VENDOR_URL \
	WEBVOWL_EXTERNAL_URL

ENV LODE_CONFIG="/opt/lode.config"
COPY ./configure-lode-environment.sh ./
RUN DEFAULT_LANG="${DEFAULT_LANG:-en}" \
	LODE_EXTERNAL_URL="${LODE_EXTERNAL_URL:-}" \
	MAX_TENTATIVE="${MAX_TENTATIVE:-3}" \
	VENDOR_CSS="${VENDOR_CSS:-}" \
	VENDOR_NAME="${VENDOR_NAME:-}" \
	VENDOR_URL="${VENDOR_URL:-}" \
	WEBVOWL_EXTERNAL_URL="${WEBVOWL_EXTERNAL_URL:-}" \
	. ./configure-lode-environment.sh

VOLUME /root/.m2
ENTRYPOINT ["mvn"]
CMD [ \
	"clean", \
	"jetty:run", \
	"-Djetty.reload=automatic", \
	"-Djetty.scanIntervalSeconds=5", \
	"-DLODE_CONFIG=${LODE_CONFIG}" \
]

#------------------------------------------------------------------------------
FROM onaci/tomcat-base:${TOMCAT_VERSION}-jdk${JAVA_VERSION} as tomcat

ARG JAVA_VERSION
ARG TOMCAT_VERSION

LABEL org.opencontainers.image.base.name="onaci/tomcat-base:${TOMCAT_VERSION}-jdk${JAVA_VERSION}"

# Upgrade the base image
ENV DEBIAN_FRONTEND noninteractive
RUN --mount=target=/var/lib/apt/lists,type=cache,sharing=locked \
	--mount=target=/var/cache/apt,type=cache,sharing=locked \
	apt-get update \
	&& apt-get -y upgrade \
	&& apt-get clean \
	&& apt-get autoremove --purge \
	&& rm -rf /var/lib/apt/lists/*

# Install the compiled PID Java application
ARG LODE_CONTEXT="lode"
COPY --from=builder /opt/target/*.war "${CATALINA_HOME}/webapps/${LODE_CONTEXT}.war"

# Arrange for the tomcat startup process to populate the
# env-derived runtime configuration properties file.
ENV LODE_CONFIG="${CATALINA_HOME}/conf/lode.properties" \
	LODE_CONTEXT="${LODE_CONTEXT}" \
	LODE_CONTEXT_PATH="${CATALINA_HOME}/webapps/${LODE_CONTEXT}"
ENV CATALINA_OPTS="${CATALINA_OPTS} -DLODE_CONFIG='${LODE_CONFIG}'"
COPY ./configure-lode-environment.sh "${CATALINA_HOME}/conf/"
RUN echo ". '${CATALINA_HOME}/conf/configure-lode-environment.sh'" >> "${CATALINA_HOME}/bin/setenv.sh"
