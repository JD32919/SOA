package com.example.ventas.integracion;

import java.io.StringReader;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

@Component
public class ClienteSoapClientes {
    private static final String NAMESPACE = "http://ejemplo.com/clientes/soap";
    private final String url;
    private final int timeoutMs;
    private final HttpClient http;

    public ClienteSoapClientes(
            @Value("${servicios.clientes.soap-url:http://localhost:9003/ws}") String url,
            @Value("${servicios.timeout-ms:5000}") int timeoutMs) {
        this.url = url;
        this.timeoutMs = timeoutMs;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(timeoutMs))
                .build();
    }

    public CreditoSoap consultarCredito(Integer clienteId) {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                                  xmlns:cli="http://ejemplo.com/clientes/soap">
                  <soapenv:Header/>
                  <soapenv:Body>
                    <cli:consultarClienteRequest><cli:id>%d</cli:id></cli:consultarClienteRequest>
                  </soapenv:Body>
                </soapenv:Envelope>
                """.formatted(clienteId);

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header("Content-Type", "text/xml; charset=utf-8")
                    .header("Accept", "text/xml")
                    .POST(HttpRequest.BodyPublishers.ofString(xml))
                    .build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "El servicio SOAP de Clientes respondio HTTP " + response.statusCode());
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

            Document document = factory.newDocumentBuilder()
                    .parse(new InputSource(new StringReader(response.body())));

            if (document.getElementsByTagNameNS(
                    "http://schemas.xmlsoap.org/soap/envelope/", "Fault").getLength() > 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Clientes rechazo la consulta SOAP del cliente " + clienteId);
            }

            Element cliente = (Element) document.getElementsByTagNameNS(NAMESPACE, "cliente").item(0);
            if (cliente == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "La respuesta SOAP de Clientes no contiene los datos del cliente");
            }

            boolean habilitado = Boolean.parseBoolean(valor(cliente, "habilitado"));
            BigDecimal disponible = new BigDecimal(valor(cliente, "disponible"));
            return new CreditoSoap(habilitado, disponible);

        } catch (ResponseStatusException e) {
            throw e;
        } catch (java.io.IOException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "No fue posible comunicarse con el servicio SOAP de Clientes", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "La consulta SOAP de Clientes fue interrumpida", e);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "No fue posible consultar el credito mediante SOAP", e);
        }
    }

    private String valor(Element padre, String etiqueta) {
        var elementos = padre.getElementsByTagNameNS(NAMESPACE, etiqueta);
        if (elementos.getLength() == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "La respuesta SOAP no contiene el campo " + etiqueta);
        }
        return elementos.item(0).getTextContent().trim();
    }

    public record CreditoSoap(boolean habilitado, BigDecimal disponible) {}
}
