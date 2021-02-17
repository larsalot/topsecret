package no.nav.brukergrupper.server;


import no.nav.portal.infrastructure.CertificateTrustStore;
import no.nav.portal.infrastructure.RedirectHandler;
import no.nav.portal.rest.api.PortalRestApi;
import org.actioncontroller.config.ConfigObserver;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.HandlerList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import javax.sql.DataSource;
import java.io.File;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

public class PortalServer {
    private static final Logger logger = LoggerFactory.getLogger(PortalServer.class);

    private final Server server = new Server();
    private final ServerConnector connector = new ServerConnector(server);
    private final PortalRestApi brukergrupperRestApi = new PortalRestApi("/rest");

    public PortalServer() {
        HttpConfiguration config = new HttpConfiguration();
        config.addCustomizer(new ForwardedRequestCustomizer());
        config.setSendServerVersion(false);
        config.setSendXPoweredBy(false);
        connector.addConnectionFactory(new HttpConnectionFactory(config));
        server.setHandler(new HandlerList(
              new RedirectHandler("/", "/rest"),
              brukergrupperRestApi
        ));
        setupConfiguration();
    }

    private void setupConfiguration() {
        int port = Optional.ofNullable(System.getenv("HTTP_PLATFORM_PORT")).map(Integer::parseInt)
                .orElse(30080);
        connector.setPort(port);

        new ConfigObserver("navportal")
                .onInetSocketAddress("http.port", port, this::setHttpPort)
                /*Når man legger inn autentisering med OIDC for Profil / Admin så er det ca slik. Se denne commit for hvordan det funket for rest før
                .onStringValue("openid.discovery_url", null, brukergrupperRestApi::setOpenIdConfiguration)
                .onStringValue("openid.client_id", null, brukergrupperRestApi::setClientId)
                .onStringValue("openid.client_secret", null, brukergrupperRestApi::setClientSecret)*/
                .onPrefixedValue("dataSource", DataSourceTransformer::create, this::setDataSource)
        ;
    }

    private void setDataSource(DataSource dataSource) {
        brukergrupperRestApi.setDataSource(dataSource);
    }


    public void setHttpPort(InetSocketAddress port) throws Exception {
        if (connector.getPort() == port.getPort()) {
            return;
        }
        connector.setPort(port.getPort());
        if (server.isStarted()) {
            connector.stop();
            connector.start();
            logger.warn("Started on {}", getURI());
        }
    }

    public void start() throws Exception {
        server.start();
        connector.start();
        logger.warn("Started on {}", getURI());
    }

    public URI getURI() throws URISyntaxException {
        return new URI("http://localhost:" + connector.getLocalPort());
    }

    public static void main(String[] args) throws Exception {
        HttpsURLConnection.setDefaultSSLSocketFactory(CertificateTrustStore.loadFromDirectory(new File(".")).createSslSocketFactory());
        new PortalServer().start();
    }
}
