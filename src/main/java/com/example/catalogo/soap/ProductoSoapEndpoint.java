package com.example.catalogo.soap;

import com.example.catalogo.configuracion.SoapConfig;
import com.example.catalogo.modelo.ProductoRespuesta;
import com.example.catalogo.servicio.ProductoService;
import java.util.Locale;
import java.util.Optional;
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
public class ProductoSoapEndpoint {

    private final ProductoService productoService;

    public ProductoSoapEndpoint(ProductoService productoService) {
        this.productoService = productoService;
    }

    @PayloadRoot(
            namespace = SoapConfig.NAMESPACE,
            localPart = "consultarProductoRequest"
    )
    @ResponsePayload
    public Element consultarProducto(
            @RequestPayload Element solicitud,
            MessageContext contexto)
            throws ParserConfigurationException {

        // El interceptor ya comprobó la estructura y el tipo del id.
        String textoId = solicitud
                .getElementsByTagNameNS(SoapConfig.NAMESPACE, "id")
                .item(0)
                .getTextContent()
                .trim();

        int id = Integer.parseInt(textoId);

        Optional<ProductoRespuesta> resultado =
                productoService.buscarPorId(id);

        if (resultado.isEmpty()) {
            SoapMessage respuesta = (SoapMessage) contexto.getResponse();
            respuesta.getSoapBody()
                    .addClientOrSenderFault(
                            "No existe el producto con identificador " + id,
                            Locale.forLanguageTag("es")
                    );
            return null;
        }

        ProductoRespuesta producto = resultado.get();

        DocumentBuilderFactory fabrica = DocumentBuilderFactory.newInstance();
        fabrica.setNamespaceAware(true);

        Document documento = fabrica.newDocumentBuilder().newDocument();

        Element respuesta = documento.createElementNS(
                SoapConfig.NAMESPACE,
                "cat:consultarProductoResponse"
        );
        documento.appendChild(respuesta);

        Element elementoProducto = documento.createElementNS(
                SoapConfig.NAMESPACE,
                "cat:producto"
        );
        respuesta.appendChild(elementoProducto);

        agregarElemento(documento, elementoProducto, "id",
                Integer.toString(producto.id()));
        agregarElemento(documento, elementoProducto, "nombre",
                producto.nombre());
        agregarElemento(documento, elementoProducto, "precio",
                producto.precio().toPlainString());
        agregarElemento(documento, elementoProducto, "existencias",
                Integer.toString(producto.existencias()));
        agregarElemento(documento, elementoProducto, "activo",
                Boolean.toString(producto.activo()));

        return respuesta;
    }

    private void agregarElemento(
            Document documento,
            Element padre,
            String nombre,
            String valor) {

        Element elemento = documento.createElementNS(
                SoapConfig.NAMESPACE,
                "cat:" + nombre
        );
        elemento.setTextContent(valor);
        padre.appendChild(elemento);
    }
}