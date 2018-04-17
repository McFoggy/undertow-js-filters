# undertow-jsfilters

## Usage

### module deployment

unzip `undertow-jsfilters-X.Y.Z-module.zip` inside `$WILDFLY_HOME`

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


