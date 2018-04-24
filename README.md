# undertow-jsfilters

## Usage

### module deployment for all Wildfly versions

unzip `undertow-jsfilters-X.Y.Z-module.zip` inside `$WILDFLY_HOME`

#### Wildfly 9, 10, 11, 12 & >

Nashorn is accessible nothing special to do. 

#### Wildfly <= 8.2.1.Final

`Wildfly 8.2.1.Final` modules lack visibility of 'nashorn', apply [wildfly-core#12](https://github.com/wildfly/wildfly-core/pull/12) to correct that 

### configuration

Update your `standalone.xml` accordingly to reference the filter

```
<subsystem xmlns="urn:jboss:domain:undertow:1.2">
    <buffer-cache name="default"/>
    <server name="default-server">
        <http-listener name="default" socket-binding="http"/>
        <host name="default-host" alias="localhost">
            <location name="/" handler="welcome-content"/>
            <filter-ref name="server-header"/>
...
            <filter-ref name="jsfilter-version"/>
            
        </host>
    </server>
    <filters>
        <response-header name="server-header" header-name="Server" header-value="WildFly/8"/>
...
        <filter name="jsfilter-version" class-name="fr.brouillard.oss.undertow.JSFilter" module="fr.brouillard.oss.undertow.jsfilters">
            <param name="fileName" value="D:/dev/projects/personnal/oss/undertow/js-filters/src/test/js/jsfilters-version-header.js"/>
        </filter>
    </filters>
</subsystem>
```

### Release

- `mvn -Poss clean install`: this will simulate a full build for oss delivery (javadoc, source attachement, GPG signature, ...)
- `git tag -a -s -m "release X.Y.Z, additionnal reason" X.Y.Z`: tag the current HEAD with the given tag name. The tag is signed by the author of the release. Adapt with gpg key of maintainer.
    - Matthieu Brouillard command:  `git tag -a -s -u 2AB5F258 -m "release X.Y.Z, additionnal reason" X.Y.Z`
    - Matthieu Brouillard [public key](https://sks-keyservers.net/pks/lookup?op=get&search=0x8139E8632AB5F258)
- `mvn -Poss,release -DskipTests deploy`
- `git push --follow-tags origin master`
