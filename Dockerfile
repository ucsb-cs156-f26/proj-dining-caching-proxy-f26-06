FROM ubuntu:22.04

# Set environment variables to avoid interactive prompts during installation
ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update 
RUN apt-get install -y openjdk-25-jdk  
RUN apt-get install -y  curl  
RUN apt-get install -y  bash  
RUN apt-get install -y  python3 
RUN apt-get clean
RUN rm -rf /var/lib/apt/lists/*

# Install a pinned upstream Maven instead of the Ubuntu package (3.6.3), whose
# bundled Guava triggers sun.misc.Unsafe warnings on JDK 24+.
ARG MAVEN_VERSION=3.9.16
RUN curl -fsSL -o /tmp/maven.tar.gz https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz \
 && curl -fsSL -o /tmp/maven.tar.gz.sha512 https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz.sha512 \
 && echo "$(awk '{print $1}' /tmp/maven.tar.gz.sha512)  /tmp/maven.tar.gz" | sha512sum -c - \
 && tar -xzf /tmp/maven.tar.gz -C /opt \
 && ln -s /opt/apache-maven-${MAVEN_VERSION}/bin/mvn /usr/bin/mvn \
 && rm /tmp/maven.tar.gz /tmp/maven.tar.gz.sha512

# Set JAVA_HOME environment variable
ENV JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64
ENV PATH="$JAVA_HOME/bin:$PATH"

# Verify installation
RUN java -version
RUN mvn --version

WORKDIR /app

# Verify installation
RUN java -version
RUN curl --version

ENV NODE_VERSION=24.21.0
RUN curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.40.1/install.sh | bash
ENV NVM_DIR=/root/.nvm
RUN . "$NVM_DIR/nvm.sh" && nvm install ${NODE_VERSION}
RUN . "$NVM_DIR/nvm.sh" && nvm use v${NODE_VERSION}
RUN . "$NVM_DIR/nvm.sh" && nvm alias default v${NODE_VERSION}
ENV PATH="/root/.nvm/versions/node/v${NODE_VERSION}/bin/:${PATH}"

RUN . "$NVM_DIR/nvm.sh" && nvm --version
RUN node --version
RUN npm --version

COPY . /home/app

RUN mvn --no-transfer-progress -B -Pproduction -DskipTests -f /home/app/pom.xml clean package

RUN ["chmod", "+x", "/home/app/startup.sh"]
ENTRYPOINT ["/home/app/startup.sh","/home/app/target/diningcachingproxy-0.0.1.jar"]