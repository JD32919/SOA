package com.example.clientes.soap;

import com.example.clientes.configuracion.SoapConfig;
import com.example.clientes.modelo.ClienteRespuesta;
import com.example.clientes.modelo.CreditoRespuesta;
import com.example.clientes.servicio.ClienteService;
import com.example.clientes.servicio.ConsumoService;
import java.util.Locale;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;
import org.springframework.ws.soap.SoapMessage;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@Endpoint
public class ClienteSoapEndpoint {

    private final ClienteService clienteService;
    private final ConsumoService consumoService;

    public ClienteSoapEndpoint(ClienteService clienteService, ConsumoService consumoService) {
        this.clienteService = clienteService;
        this.consumoService = consumoService;
    }

    @PayloadRoot(
            namespace = SoapConfig.NAMESPACE,
            localPart = "consultarClienteRequest"
    )
    @ResponsePayload
    public Element consultarCliente(
            @RequestPayload Element solicitud,
            MessageContext contexto)
            throws ParserConfigurationException {

        int id = 0;
        try {
            // el interceptor ya valida el xsd pero por si acaso
            String textoId = solicitud
                    .getElementsByTagNameNS(SoapConfig.NAMESPACE, "id")
                    .item(0)
                    .getTextContent()
                    .trim();
            id = Integer.parseInt(textoId);
        } catch (Exception e) {
            System.out.println("Error leyendo el id del SOAP: " + e.getMessage());
        }

        ClienteRespuesta cliente = null;
        if (id > 0) {
            cliente = clienteService.buscarPorId(id);
        }

        if (cliente == null) {
            SoapMessage respuesta = (SoapMessage) contexto.getResponse();
            respuesta.getSoapBody()
                    .addClientOrSenderFault(
                            "No existe el cliente con identificador " + id,
                            Locale.forLanguageTag("es")
                    );
            return null;
        }

        CreditoRespuesta credito = consumoService.consultarCredito(id, null);

        DocumentBuilderFactory fabrica = DocumentBuilderFactory.newInstance();
        fabrica.setNamespaceAware(true);

        Document doc = fabrica.newDocumentBuilder().newDocument();

        Element respuesta = doc.createElementNS(
                SoapConfig.NAMESPACE,
                "cli:consultarClienteResponse"
        );
        doc.appendChild(respuesta);

        Element elementoCliente = doc.createElementNS(
                SoapConfig.NAMESPACE,
                "cli:cliente"
        );
        respuesta.appendChild(elementoCliente);

        agregarElemento(doc, elementoCliente, "id",
                Integer.toString(cliente.id()));
        agregarElemento(doc, elementoCliente, "nombre",
                cliente.nombre());
        agregarElemento(doc, elementoCliente, "email",
                cliente.email());
        agregarElemento(doc, elementoCliente, "habilitado",
                Boolean.toString(cliente.habilitado()));
        agregarElemento(doc, elementoCliente, "limiteCredito",
                cliente.limiteCredito().toPlainString());
        agregarElemento(doc, elementoCliente, "disponible",
                credito.disponible().toPlainString());

        return respuesta;
    }

    private void agregarElemento(
            Document documento,
            Element padre,
            String nombre,
            String valor) {

        Element elemento = documento.createElementNS(
                SoapConfig.NAMESPACE,
                "cli:" + nombre
        );
        elemento.setTextContent(valor);
        padre.appendChild(elemento);
    }
}
