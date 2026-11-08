package com.example.clientes.configuracion;

import java.util.List;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.config.annotation.WsConfigurer;
import org.springframework.ws.server.EndpointInterceptor;
import org.springframework.ws.soap.server.endpoint.interceptor.PayloadValidatingInterceptor;
import org.springframework.ws.transport.http.MessageDispatcherServlet;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.xml.xsd.SimpleXsdSchema;
import org.springframework.xml.xsd.XsdSchema;

@EnableWs
@Configuration
public class SoapConfig implements WsConfigurer {

    public static final String NAMESPACE =
            "http://ejemplo.com/clientes/soap";

    // Recibe las solicitudes dirigidas a /ws.
    @Bean
    public ServletRegistrationBean<MessageDispatcherServlet>
            messageDispatcherServlet(ApplicationContext applicationContext) {

        MessageDispatcherServlet servlet = new MessageDispatcherServlet();
        servlet.setApplicationContext(applicationContext);
        servlet.setTransformWsdlLocations(true);

        return new ServletRegistrationBean<>(servlet, "/ws/*");
    }

    // Carga el contrato de mensajes.
    @Bean
    public XsdSchema clientesSoapSchema() {
        return new SimpleXsdSchema(
                new ClassPathResource("contratos/clientes-soap.xsd")
        );
    }

    // Publica /ws/clientes.wsdl.
    @Bean(name = "clientes")
    public DefaultWsdl11Definition clientesWsdl() {
        DefaultWsdl11Definition definicion = new DefaultWsdl11Definition();
        definicion.setPortTypeName("ClientesPort");
        definicion.setLocationUri("/ws");
        definicion.setTargetNamespace(NAMESPACE);
        definicion.setSchema(clientesSoapSchema());
        return definicion;
    }

    // Comprueba solicitudes y respuestas contra el XSD.
    @Bean
    public PayloadValidatingInterceptor validadorSoap() {
        PayloadValidatingInterceptor interceptor =
                new PayloadValidatingInterceptor();
        interceptor.setXsdSchema(clientesSoapSchema());
        interceptor.setValidateRequest(true);
        interceptor.setValidateResponse(true);
        return interceptor;
    }

    @Override
    public void addInterceptors(List<EndpointInterceptor> interceptores) {
        interceptores.add(validadorSoap());
    }
}