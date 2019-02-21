#
# MockServer Dockerfile
#
# https://github.com/jamesdbloom/mockserver
# http://www.mock-server.com
#

# pull base image
FROM jamesdbloom/mockserver:mockserver-5.5.1

# maintainer details
MAINTAINER Joonas Köppä "joonas.koppa@liaison.com"

COPY run_mockserver.sh /opt/mockserver/run_mockserver.sh
COPY configurations /opt/mockserver/configurations
COPY create_configurations.sh /opt/mockserver

RUN sh -c ./create_configurations.sh

# define default command.
CMD ["/opt/mockserver/run_mockserver.sh", "-logLevel", "INFO", "-serverPort", "1080"]
