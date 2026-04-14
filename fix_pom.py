import re

with open(r'd:\code\agent\agent-server\pom.xml', 'r', encoding='utf-8') as f:
    pom = f.read()

build_block = """    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.13.0</version>
                <configuration>
                    <source>21</source>
                    <target>21</target>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                            <version>1.18.30</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
        </plugins>
    </build>"""

pom = re.sub(r'<build>.*?</build>', build_block, pom, flags=re.DOTALL)

with open(r'd:\code\agent\agent-server\pom.xml', 'w', encoding='utf-8') as f:
    f.write(pom)

print("POM fixed.")
